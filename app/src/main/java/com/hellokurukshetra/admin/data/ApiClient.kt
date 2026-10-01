package com.hellokurukshetra.admin.data

import android.content.Context
import android.util.Base64
import com.hellokurukshetra.admin.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ApiClient(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("admin_session", Context.MODE_PRIVATE)
    private val client = OkHttpClient()
    private val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/') + "/"
    private val sessionStore = SessionStore(prefs)
    @Volatile private var loggedIn = false

    init {
        // ApiClient is constructed from MainActivity on Dispatchers.IO.
        // Keep this Keystore-backed check out of the Compose/UI thread.
        loggedIn = !sessionStore.get("access_token").isNullOrBlank()
    }

    fun isLoggedIn() = loggedIn

    fun logout() {
        prefs.edit().clear().apply()
        loggedIn = false
    }

    suspend fun login(identifier: String, password: String) =
        request("/auth/login", "POST", JSONObject().put("identifier", identifier.trim()).put("password", password), false)

    suspend fun get(path: String) = request(path, "GET", null, true)
    suspend fun post(path: String, body: JSONObject? = null) = request(path, "POST", body, true)
    suspend fun patch(path: String, body: JSONObject) = request(path, "PATCH", body, true)
    suspend fun put(path: String, body: JSONObject) = request(path, "PUT", body, true)

    private suspend fun request(
        path: String,
        method: String,
        body: JSONObject?,
        auth: Boolean,
        retry: Boolean = true
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        var dnsAttempt = 0
        var result: Result<JSONObject>? = null

        while (result == null) {
            try {
                val builder = Request.Builder()
                    .url(baseUrl + path.trimStart('/'))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")

                sessionStore.get("access_token")
                    ?.takeIf { auth && it.isNotBlank() }
                    ?.let { builder.header("Authorization", "Bearer $it") }

                if (auth && method != "GET") {
                    builder.header("Idempotency-Key", UUID.randomUUID().toString())
                }

                val requestBody = body?.toString()?.toRequestBody("application/json".toMediaType())
                when (method) {
                    "POST" -> builder.post(requestBody ?: ByteArray(0).toRequestBody(null))
                    "PATCH" -> builder.patch(requestBody ?: ByteArray(0).toRequestBody(null))
                    "PUT" -> builder.put(requestBody ?: ByteArray(0).toRequestBody(null))
                    else -> builder.get()
                }

                val response = client.newCall(builder.build()).execute()
                try {
                    val raw = response.body?.string().orEmpty()
                    val json = runCatching {
                        JSONObject(if (raw.isBlank()) "{}" else raw)
                    }.getOrElse {
                        JSONObject().put("raw", raw)
                    }

                    if (response.code == 401 && auth && retry) {
                        val refreshToken = sessionStore.get("refresh_token")
                        if (!refreshToken.isNullOrBlank() && refresh(refreshToken)) {
                            result = request(path, method, body, auth, false)
                        } else {
                            logout()
                        }
                    }

                    if (result == null && !response.isSuccessful) {
                        val msg = json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                            ?: json.optString("message").takeIf { it.isNotBlank() }
                            ?: "Request failed (" + response.code + ")"
                        result = Result.failure(IllegalStateException(msg))
                    }

                    if (result == null && response.isSuccessful) {
                        result = Result.success(json)
                    }
                } finally {
                    response.close()
                }
            } catch (e: UnknownHostException) {
                if (dnsAttempt >= 2) {
                    result = Result.failure(
                        IllegalStateException(
                            "Cannot connect to the backend. Please check your internet connection and try again.",
                            e
                        )
                    )
                } else {
                    dnsAttempt++
                    delay(700L * dnsAttempt)
                }
            } catch (e: Exception) {
                result = Result.failure(e)
            }
        }

        result ?: Result.failure(IllegalStateException("Request did not produce a result"))
    }

    @Synchronized
    private fun refresh(token: String): Boolean {
        return try {
            val current = sessionStore.get("refresh_token")
            if (!current.isNullOrBlank() && current != token) {
                return !sessionStore.get("access_token").isNullOrBlank()
            }

            val request = Request.Builder()
                .url(baseUrl + "auth/refresh")
                .post(JSONObject().put("refreshToken", token).toString().toRequestBody("application/json".toMediaType()))
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val data = JSONObject(response.body?.string().orEmpty()).optJSONObject("data") ?: return false
                val access = data.optString("accessToken")
                val refresh = data.optString("refreshToken")
                if (access.isBlank()) return false
                sessionStore.put("access_token", access)
                if (refresh.isNotBlank()) sessionStore.put("refresh_token", refresh)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun saveSession(data: JSONObject) {
        val access = data.optString("accessToken")
        val refresh = data.optString("refreshToken")
        if (access.isNotBlank()) {
            sessionStore.put("access_token", access)
            loggedIn = true
        }
        if (refresh.isNotBlank()) sessionStore.put("refresh_token", refresh)
    }
}

private class SessionStore(private val prefs: android.content.SharedPreferences) {
    private val alias = "hello_kurukshetra_admin_session_key"
    private val transformation = "AES/GCM/NoPadding"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun key(): SecretKey {
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun put(name: String, value: String) {
        val secret = key()
        // Let Android Keystore generate the GCM IV. Some Android Keystore
        // implementations reject caller-provided IVs for randomized encryption.
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, secret)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        prefs.edit().putString(name, Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)).apply()
    }

    fun get(name: String): String? {
        val payload = prefs.getString(name, null) ?: return null
        return try {
            val decoded = Base64.decode(payload, Base64.NO_WRAP)
            if (decoded.size <= 12) return null
            val iv = decoded.copyOfRange(0, 12)
            val encrypted = decoded.copyOfRange(12, decoded.size)
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            prefs.edit().remove(name).apply()
            null
        }
    }
}
