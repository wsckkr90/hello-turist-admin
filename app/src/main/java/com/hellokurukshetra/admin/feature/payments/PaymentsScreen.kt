package com.hellokurukshetra.admin.feature.payments
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
fun PaymentsScreen(api: ApiClient) {
 var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
 var error by remember { mutableStateOf<String?>(null) }
 var refundId by remember { mutableStateOf<String?>(null) }
 var confirmRefundId by remember { mutableStateOf<String?>(null) }
 LaunchedEffect(Unit) {
  api.get("/admin/finance/payments?limit=100").onSuccess { rows = extract(it, "items") }.onFailure { error = it.message }
 }
 LaunchedEffect(refundId) {
  val id = refundId ?: return@LaunchedEffect
  api.post("/admin/finance/payments/" + id + "/refund", JSONObject().put("reason", "Admin refund"))
    .onFailure { error = it.message }
  refundId = null
 }
 LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
  item { PageHeader("Payments & refunds", "Review payment status and initiate authorised refunds.") }
  error?.let { item { ErrorBanner(it) } }
  items(rows, key = { it.optString("id") }) { row ->
   Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp)) {
     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(displayName(row), style = MaterialTheme.typography.titleMedium)
      StatusBadge(row.optString("status", "UNKNOWN"))
     }
     Text(summary(row))
     TextButton(onClick = { confirmRefundId = row.optString("id") }) { Text("Request refund") }
    }
   }
  }
 }
 if (confirmRefundId != null) {
  AlertDialog(
   onDismissRequest = { confirmRefundId = null },
   title = { Text("Confirm refund") },
   text = { Text("Are you sure you want to initiate a refund for this payment?") },
   confirmButton = { Button(onClick = { refundId = confirmRefundId; confirmRefundId = null }) { Text("Confirm refund") } },
   dismissButton = { TextButton(onClick = { confirmRefundId = null }) { Text("Cancel") } }
  )
 }

}