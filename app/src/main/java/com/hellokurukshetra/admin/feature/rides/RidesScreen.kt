package com.hellokurukshetra.admin.feature.rides
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
fun RidesScreen(api: ApiClient) {
 var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
 var error by remember { mutableStateOf<String?>(null) }
 var action by remember { mutableStateOf<Pair<String, String>?>(null) }
 var input by remember { mutableStateOf("") }
 LaunchedEffect(Unit) { api.get("/admin/rides?limit=100").onSuccess { rows = extract(it, "items") }.onFailure { error = it.message } }
 LaunchedEffect(action) {
  val current = action ?: return@LaunchedEffect
  val body = if (current.second == "assign") JSONObject().put("driverId", input.trim()) else JSONObject().put("reason", input.trim().ifBlank { "Admin intervention" })
  val result = when (current.second) { "assign" -> api.post("/admin/rides/" + current.first + "/assign", body); "cancel" -> api.post("/admin/rides/" + current.first + "/cancel", body); else -> api.post("/admin/rides/" + current.first + "/recover", body) }
  result.onFailure { error = it.message }
  action = null; input = ""
 }
 LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
  item { PageHeader("Ride operations", "Monitor rides and perform controlled admin interventions.") }
  error?.let { item { ErrorBanner(it) } }
  items(rows, key = { it.optString("id") }) { row ->
   val id = row.optString("id")
   Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp)) {
     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(displayName(row), style = MaterialTheme.typography.titleMedium); StatusBadge(row.optString("status", "UNKNOWN")) }
     Text(summary(row))
     Row { OutlinedButton(onClick = { action = id to "assign" }) { Text("Assign") }; Spacer(Modifier.width(6.dp)); OutlinedButton(onClick = { action = id to "cancel" }) { Text("Cancel") }; Spacer(Modifier.width(6.dp)); OutlinedButton(onClick = { action = id to "recover" }) { Text("Recover") } }
    }
   }
  }
 }
 if (action != null) AlertDialog(onDismissRequest = { action = null }, title = { Text(action!!.second.replaceFirstChar { it.uppercase() } + " ride") }, text = { OutlinedTextField(input, { input = it }, label = { Text(if (action!!.second == "assign") "Driver ID" else "Reason") }) }, confirmButton = { Button(onClick = { }) { Text("Confirm") } }, dismissButton = { TextButton(onClick = { action = null }) { Text("Close") } })
}