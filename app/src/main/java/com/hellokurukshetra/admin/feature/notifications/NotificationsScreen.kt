package com.hellokurukshetra.admin.feature.notifications

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
fun NotificationsScreen(api: ApiClient) {
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var send by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var success by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        api.get("/admin/notifications?limit=100")
            .onSuccess { rows = extract(it, "notifications") }
            .onFailure { error = it.message ?: "Unable to load notifications" }
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            PageHeader(
                "Notifications",
                "Review delivery history and send targeted customer messages.",
                { refreshKey++ },
                loading,
                action = { Button(onClick = { send = true }) { Text("Send notification") } }
            )
        }
        success?.let { item { SuccessBanner(it) } }
        error?.let { item { ErrorBanner(it) { refreshKey++ } } }
        if (loading && rows.isEmpty()) item { LoadingState("Loading notification history…") }
        if (!loading && rows.isEmpty() && error == null) item { EmptyState("No notifications found.") }

        items(rows, key = { it.optString("id") }) { row ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(row.optJSONObject("user")?.optString("name", "Customer") ?: "Customer", style = MaterialTheme.typography.titleMedium)
                        Text(row.optString("createdAt", ""), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(row.optString("title", "Notification"), fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    Text(row.optString("body", ""))
                }
            }
        }
    }

    if (send) {
        AlertDialog(
            onDismissRequest = { send = false },
            title = { Text("Send notification") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Use the customer's user ID from People.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(user, { user = it }, label = { Text("User ID") }, singleLine = true)
                    OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(body, { body = it }, label = { Text("Message") }, minLines = 4)
                }
            },
            confirmButton = {
                Button(enabled = user.isNotBlank() && title.isNotBlank() && body.isNotBlank(), onClick = {
                    scope.launch {
                        api.post(
                            "/admin/notifications",
                            JSONObject().put("userId", user.trim()).put("title", title.trim()).put("body", body.trim())
                        ).onSuccess {
                            send = false
                            user = ""
                            title = ""
                            body = ""
                            success = "Notification sent successfully."
                            refreshKey++
                        }.onFailure { error = it.message ?: "Unable to send notification" }
                    }
                }) { Text("Send") }
            },
            dismissButton = { TextButton(onClick = { send = false }) { Text("Cancel") } }
        )
    }
}
