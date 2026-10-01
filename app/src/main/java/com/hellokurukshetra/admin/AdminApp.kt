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
import com.hellokurukshetra.admin.ui.AdminNavy
import com.hellokurukshetra.admin.ui.AdminGold
import com.hellokurukshetra.admin.ui.AdminShell
import org.json.JSONObject
@Composable fun AdminApp(api:ApiClient){var logged by remember{mutableStateOf(api.isLoggedIn())};if(logged)AdminShell(api){api.logout();logged=false}else AdminLogin(api){logged=true}}
@Composable private fun AdminLogin(api:ApiClient,onLogin:()->Unit){var id by remember{mutableStateOf("")};var password by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)};val scope=rememberCoroutineScope();Box(Modifier.fillMaxSize().background(AdminNavy),contentAlignment=Alignment.Center){Card(Modifier.padding(24.dp).fillMaxWidth().widthIn(max=460.dp)){Column(Modifier.padding(30.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Text("HELLO KURUKSHETRA",color=AdminNavy,fontWeight=FontWeight.Bold);Text("Admin Control Center",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Manage people, rides, safety, payments and content from one place.",color=Color.Gray);OutlinedTextField(id,{id=it},label={Text("Username or email")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(password,{password=it},label={Text("Password")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth());error?.let{Text(it,color=MaterialTheme.colorScheme.error)};Button(enabled=!busy,onClick={if(id.isBlank()||password.isBlank()){error="Enter username and password";return@Button};busy=true;scope.launch{api.login(id,password).onSuccess{root->val d=root.optJSONObject("data")?:JSONObject();api.saveSession(d);api.get("/admin/analytics").onSuccess{onLogin()}.onFailure{api.logout();error="This account does not have active admin access."}}.onFailure{error=it.message};busy=false}},colors=ButtonDefaults.buttonColors(containerColor=AdminGold,contentColor=AdminNavy),modifier=Modifier.fillMaxWidth().height(52.dp)){if(busy)CircularProgressIndicator(strokeWidth=2.dp,modifier=Modifier.size(22.dp))else Text("SIGN IN",fontWeight=FontWeight.Bold)}}}}}
