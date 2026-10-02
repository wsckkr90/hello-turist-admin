package com.hellokurukshetra.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.hellokurukshetra.admin.data.ApiClient
import com.hellokurukshetra.admin.ui.AdminGold
import com.hellokurukshetra.admin.ui.AdminNavy
import com.hellokurukshetra.admin.ui.AdminShell
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun AdminApp(api: ApiClient) {
    var logged by remember { mutableStateOf(api.isLoggedIn()) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(logged) {
        if (!logged) return@LaunchedEffect
        val seen = mutableSetOf<String>()
        var initialized = false
        while (api.isLoggedIn()) {
            api.get("/admin/emergency/incidents?state=TRIGGERED&limit=50").onSuccess { root ->
                val items = root.optJSONObject("data")?.optJSONArray("items") ?: root.optJSONArray("items")
                val current = mutableListOf<Pair<String, Pair<String,String>>>()
                for (i in 0 until (items?.length() ?: 0)) {
                    val o=items!!.optJSONObject(i) ?: continue
                    val id=o.optString("id"); if(id.isBlank()) continue
                    current += id to (o.optString("riderName","Rider") to o.optString("category","EMERGENCY"))
                }
                if (initialized) current.filterNot { seen.contains(it.first) }.forEach { (id, info) -> EmergencyAlertManager.alert(context, id, info.first, info.second) }
                seen.clear(); seen.addAll(current.map{it.first}); initialized=true
            }
            kotlinx.coroutines.delay(5_000L)
        }
    }

    if (logged) {
        AdminShell(api) {
            api.logout()
            logged = false
        }
    } else {
        AdminLogin(api) { logged = true }
    }
}

@Composable
private fun AdminLogin(api: ApiClient, onLogin: () -> Unit) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(AdminNavy), contentAlignment = Alignment.Center) {
        Card(Modifier.padding(24.dp).fillMaxWidth().widthIn(max = 460.dp)) {
            Column(Modifier.padding(30.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("HELLO KURUKSHETRA", color = AdminNavy, fontWeight = FontWeight.Bold)
                Text("Admin Control Center", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Secure operations for people, rides, safety, payments and content.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    identifier,
                    { identifier = it; error = null },
                    label = { Text("Username or email") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    password,
                    { password = it; error = null },
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    enabled = !busy && identifier.isNotBlank() && password.isNotBlank(),
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            api.login(identifier, password)
                                .onSuccess { root ->
                                    val data = root.optJSONObject("data") ?: JSONObject()
                                    api.saveSession(data)
                                    if (api.isLoggedIn()) onLogin()
                                    else error = "Login succeeded but no access token was returned."
                                }
                                .onFailure { error = it.message ?: "Unable to sign in" }
                            busy = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminNavy),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = AdminNavy)
                    else Text("SIGN IN", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
