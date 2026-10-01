package com.hellokurukshetra.admin.feature.verification
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
fun VerificationScreen(api: ApiClient) {
 var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
 var error by remember { mutableStateOf<String?>(null) }
 var selected by remember { mutableStateOf<JSONObject?>(null) }
 var approve by remember { mutableStateOf<String?>(null) }
 LaunchedEffect(Unit) {
  val all = mutableListOf<JSONObject>()
  for (role in listOf("DRIVER", "GUIDE")) for (status in listOf("UNDER_VERIFICATION", "RESUBMITTED", "PENDING")) {
   api.get("/admin/verification/requests?role=" + role + "&status=" + status + "&limit=100").onSuccess { all.addAll(extractVerification(it)) }.onFailure { error = it.message }
  }
  rows = all.distinctBy { it.optString("id") }
 }
 LaunchedEffect(approve) {
  val id = approve ?: return@LaunchedEffect
  api.patch("/admin/verification/requests/" + id, JSONObject().put("status", "VERIFIED"))
    .onSuccess { rows = rows.filterNot { it.optString("id") == id } }.onFailure { error = it.message }
  approve = null
 }
 LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
  item { PageHeader("Provider verification", "Review driver and guide applications before activation.") }
  error?.let { item { ErrorBanner(it) } }
  if (rows.isEmpty() && error == null) item { EmptyState("No pending provider verification requests.") }
  items(rows, key = { it.optString("id") }) { row ->
   val user = row.optJSONObject("user")
   Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(user?.optString("name").orEmpty().ifBlank { "Provider" }, style = MaterialTheme.typography.titleMedium); StatusBadge(row.optString("status", "PENDING")) }
     Text("Documents: " + (row.optJSONArray("documents")?.length() ?: 0))
     Row { OutlinedButton(onClick = { selected = row }) { Text("Review") }; Spacer(Modifier.width(8.dp)); Button(onClick = { approve = row.optString("id") }) { Text("Approve") } }
    }
   }
  }
 }
 selected?.let { row -> AlertDialog(onDismissRequest = { selected = null }, title = { Text("Verification details") }, text = { Text(summary(row)) }, confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }) }
}
private fun extractVerification(root: JSONObject): List<JSONObject> { for (key in listOf("items", "requests", "verificationRequests")) { val result = extract(root, key); if (result.isNotEmpty()) return result }; return emptyList() }