package com.hellokurukshetra.admin.feature.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hellokurukshetra.admin.data.ApiClient
import com.hellokurukshetra.admin.ui.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun SupportScreen(api: ApiClient) {
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var target by remember { mutableStateOf<JSONObject?>(null) }
    var message by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("IN_PROGRESS") }
    var page by rememberSaveable { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(page, refreshKey) {
        loading = true
        error = null
        api.get("/admin/support/tickets?page=$page&limit=20")
            .onSuccess {
                rows = extract(it, "items")
                totalPages = pagination(it).optInt("totalPages", 1).coerceAtLeast(1)
            }
            .onFailure { error = it.message ?: "Unable to load support tickets" }
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { PageHeader("Support tickets", "Reply to customers and keep ticket status under control.", { refreshKey++ }, loading) }
        error?.let { item { ErrorBanner(it) { refreshKey++ } } }
        if (loading && rows.isEmpty()) item { LoadingState("Loading support tickets…") }
        if (!loading && rows.isEmpty() && error == null) item { EmptyState("No support tickets found.") }

        items(rows, key = { it.optString("id") }) { row ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(row.optString("subject", displayName(row)), style = MaterialTheme.typography.titleMedium)
                        StatusBadge(row.optString("status", "OPEN"))
                    }
                    Text("Customer: " + row.optJSONObject("user")?.optString("name", row.optJSONObject("user")?.optString("username", "—")))
                    Text(summary(row), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { target = row; status = row.optString("status", "OPEN") }) { Text("Manage") }
                        if (row.optString("status") != "CLOSED") {
                            TextButton(onClick = {
                                scope.launch {
                                    api.patch("/admin/support/tickets/" + row.optString("id") + "/status", JSONObject().put("status", "CLOSED"))
                                        .onSuccess { refreshKey++ }
                                        .onFailure { error = it.message }
                                }
                            }) { Text("Close") }
                        }
                    }
                }
            }
        }
        item { PaginationBar(page, totalPages, { if (page > 1) page-- }, { if (page < totalPages) page++ }) }
    }

    target?.let { ticket ->
        AlertDialog(
            onDismissRequest = { target = null; message = "" },
            title = { Text("Manage ticket") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(ticket.optString("subject", "Support ticket"), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(message, { message = it }, label = { Text("Reply message") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                    Text("Set status", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = status == "OPEN", onClick = { status = "OPEN" }, label = { Text("Open") })
                        FilterChip(selected = status == "IN_PROGRESS", onClick = { status = "IN_PROGRESS" }, label = { Text("In progress") })
                        FilterChip(selected = status == "CLOSED", onClick = { status = "CLOSED" }, label = { Text("Closed") })
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = message.isBlank() || message.trim().isNotEmpty(),
                    onClick = {
                        scope.launch {
                            var failed = false
                            if (message.trim().isNotEmpty()) {
                                api.post("/admin/support/tickets/" + ticket.optString("id") + "/reply", JSONObject().put("message", message.trim()))
                                    .onFailure { error = it.message; failed = true }
                            }
                            if (!failed) {
                                api.patch("/admin/support/tickets/" + ticket.optString("id") + "/status", JSONObject().put("status", status))
                                    .onSuccess { target = null; message = ""; refreshKey++ }
                                    .onFailure { error = it.message }
                            }
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { target = null; message = "" }) { Text("Cancel") } }
        )
    }
}
