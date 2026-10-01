package com.hellokurukshetra.admin.feature.support
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
fun SupportScreen(api: ApiClient) {
 var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
 var error by remember { mutableStateOf<String?>(null) }
 var target by remember { mutableStateOf<String?>(null) }
 var message by remember { mutableStateOf("") }
 var submit by remember { mutableStateOf(false) }
 LaunchedEffect(Unit) {
  api.get("/admin/support/tickets?limit=100").onSuccess { rows = extract(it, "items") }.onFailure { error = it.message }
 }
 LaunchedEffect(submit) {
  if (!submit) return@LaunchedEffect
  val id = target ?: return@LaunchedEffect
  if (message.isNotBlank()) api.post("/admin/support/tickets/" + id + "/reply", JSONObject().put("message", message.trim())).onFailure { error = it.message }
  message = ""
  submit = false
 }
 LaunchedEffect(target) {
  val id = target ?: return@LaunchedEffect
  if (message.isNotBlank()) {
   // draft is submitted by the submit state above
  }
 }
 LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
  item { PageHeader("Support tickets", "Handle customer issues with a clear audit trail.") }
  error?.let { item { ErrorBanner(it) } }
  items(rows, key = { it.optString("id") }) { row ->
   Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp)) {
     Text(displayName(row), style = MaterialTheme.typography.titleMedium)
     StatusBadge(row.optString("status", "OPEN"))
     Text(summary(row))
     TextButton(onClick = { target = row.optString("id") }) { Text("Reply") }
    }
   }
  }
 }
 target?.let { id ->
  AlertDialog(
   onDismissRequest = { target = null },
   title = { Text("Reply to ticket") },
   text = { OutlinedTextField(message, { message = it }, label = { Text("Message") }, minLines = 3) },
   confirmButton = { Button(onClick = { submit = true }) { Text("Send") } },
   dismissButton = { TextButton(onClick = { target = null; message = "" }) { Text("Cancel") } }
  )
 }
}