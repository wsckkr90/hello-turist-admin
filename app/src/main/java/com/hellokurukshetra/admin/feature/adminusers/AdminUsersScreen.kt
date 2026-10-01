package com.hellokurukshetra.admin.feature.adminusers

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
fun AdminUsersScreen(api: ApiClient) {
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var permissions by remember { mutableStateOf(setOf<String>()) }
    var available by remember { mutableStateOf<List<String>>(emptyList()) }
    var saving by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        api.get("/admin/users?limit=100")
            .onSuccess { rows = extract(it, "items") }
            .onFailure { error = it.message ?: "Unable to load admin users" }
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { PageHeader("Admin users", "Manage administrator permissions using the backend permission catalog.", { refreshKey++ }, loading) }
        error?.let { item { ErrorBanner(it) { refreshKey++ } } }
        if (loading && rows.isEmpty()) item { LoadingState("Loading admin users…") }
        if (!loading && rows.isEmpty() && error == null) item { EmptyState("No active admin users found.") }

        items(rows, key = { it.optString("id") }) { row ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(displayName(row), style = MaterialTheme.typography.titleMedium)
                        StatusBadge(row.optString("status", "ACTIVE"))
                    }
                    Text(summary(row), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = {
                        val id = row.optString("id")
                        scope.launch {
                            api.get("/admin/rbac/users/" + id + "/permissions")
                                .onSuccess {
                                    val data = dataObject(it)
                                    available = stringArray(data, "availablePermissions")
                                    permissions = stringArray(data, "permissions").toSet()
                                    selected = row
                                }
                                .onFailure { error = it.message ?: "Unable to load permissions" }
                        }
                    }) { Text("Manage permissions") }
                }
            }
        }
    }

    selected?.let { row ->
        AlertDialog(
            onDismissRequest = { if (!saving) selected = null },
            title = { Text("Permissions — " + displayName(row)) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 430.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Text("Choose only the access this administrator needs.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(available) { permission ->
                        FilterChip(
                            selected = permissions.contains(permission),
                            onClick = {
                                permissions = if (permissions.contains(permission)) permissions - permission else permissions + permission
                            },
                            label = { Text(permission) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(enabled = !saving, onClick = {
                    saving = true
                    scope.launch {
                        api.put(
                            "/admin/rbac/users/" + row.optString("id") + "/permissions",
                            JSONObject().put("permissions", permissions.toList())
                        ).onSuccess {
                            selected = null
                            refreshKey++
                        }.onFailure { error = it.message ?: "Unable to save permissions" }
                        saving = false
                    }
                }) {
                    if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Save")
                }
            },
            dismissButton = { TextButton(enabled = !saving, onClick = { selected = null }) { Text("Cancel") } }
        )
    }
}
