package com.hellokurukshetra.admin.data

import android.content.Context
import com.hellokurukshetra.admin.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ApiClient(context: Context) {
    private val prefs=context.getSharedPreferences("admin_session",Context.MODE_PRIVATE)
    private val client=OkHttpClient()
    private val baseUrl=BuildConfig.API_BASE_URL.trimEnd('/')+"/"
    fun isLoggedIn()=!prefs.getString("access_token",null).isNullOrBlank()
    fun logout(){prefs.edit().clear().apply()}
    suspend fun login(identifier:String,password:String)=request("/auth/login","POST",JSONObject().put("identifier",identifier.trim()).put("password",password),false)
    suspend fun get(path:String)=request(path,"GET",null,true)
    suspend fun post(path:String,body:JSONObject?=null)=request(path,"POST",body,true)
    suspend fun patch(path:String,body:JSONObject)=request(path,"PATCH",body,true)
    private suspend fun request(path:String,method:String,body:JSONObject?,auth:Boolean,retry:Boolean=true):Result<JSONObject>=withContext(Dispatchers.IO){
        try{val b=Request.Builder().url(baseUrl+path.trimStart('/')).header("Accept","application/json").header("Content-Type","application/json");prefs.getString("access_token",null)?.takeIf{auth&&!it.isBlank()}?.let{b.header("Authorization","Bearer $it")};val rb=body?.toString()?.toRequestBody("application/json".toMediaType());when(method){"POST"->b.post(rb?:ByteArray(0).toRequestBody(null));"PATCH"->b.patch(rb?:ByteArray(0).toRequestBody(null));else->b.get()};client.newCall(b.build()).execute().use{response->val raw=response.body?.string().orEmpty();val json=runCatching{JSONObject(if(raw.isBlank())"{}" else raw)}.getOrElse{JSONObject().put("raw",raw)};if(response.code==401&&auth&&retry){val rt=prefs.getString("refresh_token",null);if(!rt.isNullOrBlank()&&refresh(rt))return@withContext request(path,method,body,auth,false)};if(!response.isSuccessful){val msg=json.optJSONObject("error")?.optString("message")?.takeIf{it.isNotBlank()}?:json.optString("message").takeIf{it.isNotBlank()}?:"Request failed ("+response.code+")";return@withContext Result.failure(IllegalStateException(msg))};Result.success(json)}}catch(e:Exception){Result.failure(e)}}
    private fun refresh(token:String):Boolean=try{val req=Request.Builder().url(baseUrl+"auth/refresh").post(JSONObject().put("refreshToken",token).toString().toRequestBody("application/json".toMediaType())).header("Content-Type","application/json").build();client.newCall(req).execute().use{response->if(!response.isSuccessful)return false;val d=JSONObject(response.body?.string().orEmpty()).optJSONObject("data")?:return false;val a=d.optString("accessToken");val r=d.optString("refreshToken");if(a.isBlank())return false;prefs.edit().putString("access_token",a).apply{if(r.isNotBlank())putString("refresh_token",r)}.apply();true}}catch(_:Exception){false}
    fun saveSession(data:JSONObject){prefs.edit().putString("access_token",data.optString("accessToken")).putString("refresh_token",data.optString("refreshToken")).apply()}
}