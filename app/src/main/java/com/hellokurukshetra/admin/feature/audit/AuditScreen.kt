package com.hellokurukshetra.admin.feature.audit

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
fun AuditScreen(api: ApiClient) {
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var action by remember { mutableStateOf("") }
    var entity by remember { mutableStateOf("") }
    var page by rememberSaveable { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(page, action, entity, refreshKey) {
        loading = true
        error = null
        val query = buildString {
            append("/admin/audit-logs?page=").append(page).append("&limit=20")
            if (action.isNotBlank()) append("&action=").append(java.net.URLEncoder.encode(action.trim(), "UTF-8"))
            if (entity.isNotBlank()) append("&entityType=").append(java.net.URLEncoder.encode(entity.trim(), "UTF-8"))
        }
        api.get(query).onSuccess {
            rows = extract(it, "items")
            totalPages = pagination(it).optInt("totalPages", 1).coerceAtLeast(1)
        }.onFailure { error = it.message ?: "Unable to load audit logs" }
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { PageHeader("Audit logs", "A chronological record of administrator actions.", { refreshKey++ }, loading) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(action, { action = it; page = 1 }, label = { Text("Action") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(entity, { entity = it; page = 1 }, label = { Text("Entity type") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { action = ""; entity = ""; page = 1; refreshKey++ }) { Text("Clear") }
            }
        }
        error?.let { item { ErrorBanner(it) { refreshKey++ } } }
        if (loading && rows.isEmpty()) item { LoadingState("Loading audit history…") }
        if (!loading && rows.isEmpty() && error == null) item { EmptyState("No audit events found.") }
        items(rows, key = { it.optString("id") }) { row ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(row.optString("action", "Admin action"), style = MaterialTheme.typography.titleMedium)
                        Text(row.optString("createdAt", ""), style = MaterialTheme.typography.labelSmall)
                    }
                    Text(row.optString("entityType", "—") + " • " + row.optString("entityId", "—"))
                    Text("Actor: " + row.optString("actorUsername", row.optString("actorUserId", "—")), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val metadata = row.optJSONObject("metadata")
                    if (metadata != null) Text(metadata.toString(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { PaginationBar(page, totalPages, { if (page > 1) page-- }, { if (page < totalPages) page++ }) }
    }
}
