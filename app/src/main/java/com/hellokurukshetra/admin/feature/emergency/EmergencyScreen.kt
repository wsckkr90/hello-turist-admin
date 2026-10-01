package com.hellokurukshetra.admin.feature.emergency
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
fun EmergencyScreen(api: ApiClient) {
 var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
 var error by remember { mutableStateOf<String?>(null) }
 var action by remember { mutableStateOf<Pair<String, String>?>(null) }
 LaunchedEffect(Unit) {
  api.get("/admin/emergency/incidents?limit=100").onSuccess {
   rows = extract(it, "items").ifEmpty { extract(it, "incidents") }
  }.onFailure { error = it.message }
 }
 LaunchedEffect(action) {
  val current = action ?: return@LaunchedEffect
  api.post("/admin/emergency/incidents/" + current.first + "/" + current.second).onFailure { error = it.message }
  action = null
 }
 LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
  item { PageHeader("Emergency response", "Monitor incidents and record response actions.") }
  error?.let { item { ErrorBanner(it) } }
  items(rows, key = { it.optString("id") }) { row ->
   val id = row.optString("id")
   Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp)) {
     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(displayName(row), style = MaterialTheme.typography.titleMedium)
      StatusBadge(row.optString("status", "OPEN"))
     }
     Text(summary(row))
     Row {
      TextButton(onClick = { action = id to "acknowledge" }) { Text("Acknowledge") }
      TextButton(onClick = { action = id to "escalate" }) { Text("Escalate") }
      TextButton(onClick = { action = id to "resolve" }) { Text("Resolve") }
     }
    }
   }
  }
 }
}