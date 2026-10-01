package com.hellokurukshetra.admin.feature.people

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hellokurukshetra.admin.data.ApiClient
import com.hellokurukshetra.admin.ui.*
import org.json.JSONObject

@Composable
fun PeopleScreen(api: ApiClient) {
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(page, role, search, refreshKey) {
        loading = true
        error = null
        val query = buildString {
            append("/admin/people?page=").append(page).append("&limit=20")
            if (role.isNotBlank()) append("&role=").append(role)
            if (search.isNotBlank()) append("&search=").append(java.net.URLEncoder.encode(search.trim(), "UTF-8"))
        }
        api.get(query)
            .onSuccess {
                rows = extract(it, "items")
                val p = pagination(it)
                totalPages = p.optInt("totalPages", 1).coerceAtLeast(1)
            }
            .onFailure { error = it.message ?: "Unable to load people" }
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            PageHeader("People", "Search riders, drivers and guides from the live user directory.", { refreshKey++ }, loading)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(search, { search = it; page = 1 }, label = { Text("Search name, username or email") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { search = ""; role = ""; page = 1; refreshKey++ }) { Text("Clear") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = role.isBlank(), onClick = { role = ""; page = 1 }, label = { Text("All") })
                FilterChip(selected = role == "RIDER", onClick = { role = "RIDER"; page = 1 }, label = { Text("Riders") })
                FilterChip(selected = role == "DRIVER", onClick = { role = "DRIVER"; page = 1 }, label = { Text("Drivers") })
                FilterChip(selected = role == "GUIDE", onClick = { role = "GUIDE"; page = 1 }, label = { Text("Guides") })
            }
        }
        error?.let { item { ErrorBanner(it) { refreshKey++ } } }
        if (loading && rows.isEmpty()) item { LoadingState("Loading people…") }
        if (!loading && rows.isEmpty() && error == null) item { EmptyState("No people match these filters.") }
        items(rows, key = { it.optString("id") }) { row ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(displayName(row), style = MaterialTheme.typography.titleMedium)
                        StatusBadge(row.optString("status", "ACTIVE"))
                    }
                    Text(summary(row), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val roles = stringArrayFromRoles(row)
                    if (roles.isNotEmpty()) Text(roles.joinToString(" • "), style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = {
                        val id = row.optString("id")
                        scope.launch { api.get("/admin/people/" + id).onSuccess { selected = dataObject(it) }.onFailure { error = it.message } }
                    }) { Text("View details") }
                }
            }
        }
        item { PaginationBar(page, totalPages, { if (page > 1) page-- }, { if (page < totalPages) page++ }) }
    }

    selected?.let { row ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(displayName(row)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Username: " + row.optString("username", "—"))
                    Text("Email: " + row.optString("email", "—"))
                    Text("Status: " + row.optString("status", "—"))
                    Text("Language: " + row.optString("preferredLanguage", "—"))
                    Text("Roles: " + rolesText(row))
                    Text("Created: " + row.optString("createdAt", "—"))
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }
        )
    }
}

private fun stringArrayFromRoles(row: JSONObject): List<String> {
    val roles = row.optJSONArray("roles") ?: return emptyList()
    return (0 until roles.length()).mapNotNull { roles.optJSONObject(it)?.optString("role") }.filter { it.isNotBlank() }
}

private fun rolesText(row: JSONObject): String = stringArrayFromRoles(row).joinToString(", ").ifBlank { "—" }
