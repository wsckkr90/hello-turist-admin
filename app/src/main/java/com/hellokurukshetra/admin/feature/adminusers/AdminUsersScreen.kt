package com.hellokurukshetra.admin.feature.adminusers
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
fun AdminUsersScreen(api: ApiClient) {
 var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
 var error by remember { mutableStateOf<String?>(null) }
 var selected by remember { mutableStateOf<String?>(null) }
 var permissions by remember { mutableStateOf("") }
 var save by remember { mutableStateOf(false) }
 LaunchedEffect(Unit) { api.get("/admin/users?limit=100").onSuccess { rows = extract(it, "items") }.onFailure { error = it.message } }
 LaunchedEffect(selected) {
  val id = selected ?: return@LaunchedEffect
  api.get("/admin/rbac/users/" + id + "/permissions").onSuccess {
   val a = dataObject(it).optJSONArray("permissions")
   permissions = if (a == null) "" else (0 until a.length()).joinToString(",") { i -> a.optString(i) }
  }.onFailure { error = it.message }
 }
 LaunchedEffect(save) {
  if (save && selected != null) {
   api.put("/admin/rbac/users/" + selected + "/permissions", JSONObject().put("permissions", permissions.split(",").map { it.trim() }.filter { it.isNotBlank() }))
     .onFailure { error = it.message }
   save = false; selected = null
  }
 }
 LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
  item { PageHeader("Admin users", "Manage administrator access and permissions.") }
  error?.let { item { ErrorBanner(it) } }
  items(rows, key = { it.optString("id") }) { row -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(displayName(row), style = MaterialTheme.typography.titleMedium); StatusBadge(row.optString("status", "ACTIVE")) }; Text(summary(row)); TextButton(onClick = { selected = row.optString("id") }) { Text("Edit permissions") } } } }
 }
 selected?.let { AlertDialog(onDismissRequest = { selected = null }, title = { Text("Permissions") }, text = { OutlinedTextField(permissions, { permissions = it }, label = { Text("Comma-separated permissions") }, minLines = 4) }, confirmButton = { Button(onClick = { save = true }) { Text("Save") } }, dismissButton = { TextButton(onClick = { selected = null }) { Text("Cancel") } }) }
}