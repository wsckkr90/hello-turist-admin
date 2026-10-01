package com.hellokurukshetra.admin.feature.notifications
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hellokurukshetra.admin.data.ApiClient
import com.hellokurukshetra.admin.ui.*
import org.json.JSONObject
@Composable
fun NotificationsScreen(api: ApiClient) {
 var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
 var error by remember { mutableStateOf<String?>(null) }
 var send by remember { mutableStateOf(false) }
 var user by remember { mutableStateOf("") }
 var title by remember { mutableStateOf("") }
 var body by remember { mutableStateOf("") }
 LaunchedEffect(Unit) {
  api.get("/admin/notifications?limit=100").onSuccess { rows = extract(it, "notifications") }.onFailure { error = it.message }
 }
 LaunchedEffect(send) {
  if (send && user.isNotBlank() && title.isNotBlank() && body.isNotBlank()) {
   api.post("/admin/notifications", JSONObject().put("userId", user.trim()).put("title", title.trim()).put("body", body.trim()))
     .onFailure { error = it.message }
   send = false
  }
 }
 LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
  item { PageHeader("Notifications", "Review notification history and send a direct message."); Button(onClick = { send = true }) { Text("Send notification") } }
  error?.let { item { ErrorBanner(it) } }
  items(rows, key = { it.optString("id") }) { row -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(displayName(row), style = MaterialTheme.typography.titleMedium); Text(summary(row)) } } }
 }
 if (send) {
  AlertDialog(onDismissRequest = { send = false }, title = { Text("Send notification") },
   text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(user, { user = it }, label = { Text("User ID") }); OutlinedTextField(title, { title = it }, label = { Text("Title") }); OutlinedTextField(body, { body = it }, label = { Text("Message") }, minLines = 3) } },
   confirmButton = { Button(onClick = { if (user.isNotBlank() && title.isNotBlank() && body.isNotBlank()) send = true }) { Text("Send") } },
   dismissButton = { TextButton(onClick = { send = false }) { Text("Cancel") } })
 }
}