package com.hellokurukshetra.admin.feature.payments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hellokurukshetra.admin.data.ApiClient
import com.hellokurukshetra.admin.ui.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun PaymentsScreen(api: ApiClient) {
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var summary by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refundId by remember { mutableStateOf<String?>(null) }
    var refundAmount by remember { mutableStateOf("") }
    var refundReason by remember { mutableStateOf("") }
    var page by rememberSaveable { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(page, refreshKey) {
        loading = true
        error = null
        val summaryResult = api.get("/admin/finance/summary")
        summaryResult.onSuccess { summary = dataObject(it) }
        summaryResult.onFailure { error = it.message ?: "Unable to load finance summary" }

        api.get("/admin/finance/payments?page=$page&limit=20")
            .onSuccess {
                rows = extract(it, "items")
                totalPages = pagination(it).optInt("totalPages", 1).coerceAtLeast(1)
            }
            .onFailure { error = it.message ?: "Unable to load payments" }
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { PageHeader("Payments & refunds", "Review payment health and initiate authorised Razorpay refunds.", { refreshKey++ }, loading) }
        error?.let { item { ErrorBanner(it) { refreshKey++ } } }

        summary?.let { finance ->
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    val total = finance.optJSONObject("totals")
                    val captured = finance.optJSONObject("captured")
                    val refunded = finance.optJSONObject("refunded")
                    Metric("Transactions", total?.optInt("count", 0).toString(), Modifier.weight(1f))
                    Metric("Captured", captured?.optDouble("amount", 0.0).toString(), Modifier.weight(1f), "INR")
                    Metric("Refunded", refunded?.optDouble("amount", 0.0).toString(), Modifier.weight(1f), "INR")
                }
            }
        }

        if (loading && rows.isEmpty()) item { LoadingState("Loading payments…") }
        if (!loading && rows.isEmpty() && error == null) item { EmptyState("No payments found.") }

        items(rows, key = { it.optString("id") }) { row ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(displayName(row), style = MaterialTheme.typography.titleMedium)
                        StatusBadge(row.optString("status", "UNKNOWN"))
                    }
                    Text("Amount: INR " + row.opt("amount").toString())
                    Text(summary(row), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (row.optString("status") == "CAPTURED") {
                        OutlinedButton(onClick = {
                            refundId = row.optString("id")
                            refundAmount = ""
                            refundReason = ""
                        }) { Text("Refund") }
                    }
                }
            }
        }
        item { PaginationBar(page, totalPages, { if (page > 1) page-- }, { if (page < totalPages) page++ }) }
    }

    refundId?.let { id ->
        AlertDialog(
            onDismissRequest = { refundId = null },
            title = { Text("Issue refund") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(refundAmount, { refundAmount = it }, label = { Text("Amount in INR (optional)") }, singleLine = true)
                    OutlinedTextField(refundReason, { refundReason = it }, label = { Text("Reason") }, minLines = 3)
                    Text("Leave amount empty to refund the remaining captured amount.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(enabled = refundReason.trim().length >= 2, onClick = {
                    val body = JSONObject().put("reason", refundReason.trim())
                    if (refundAmount.isNotBlank()) body.put("amount", refundAmount.toDoubleOrNull() ?: -1)
                    scope.launch {
                        api.post("/admin/finance/payments/" + id + "/refund", body)
                            .onSuccess { refundId = null; refreshKey++ }
                            .onFailure { error = it.message ?: "Refund failed" }
                    }
                }) { Text("Confirm refund") }
            },
            dismissButton = { TextButton(onClick = { refundId = null }) { Text("Cancel") } }
        )
    }
}
