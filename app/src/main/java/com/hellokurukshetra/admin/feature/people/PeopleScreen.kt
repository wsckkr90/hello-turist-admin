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
    var editing by remember { mutableStateOf<JSONObject?>(null) }
    var showForm by rememberSaveable { mutableStateOf(false) }
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PageHeader("People", "Search riders, drivers and guides from the live user directory.", { refreshKey++ }, loading)
                Button(onClick = { editing = null; showForm = true }) { Text("Add person") }
            }
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AdminAvatar(displayName(row), row.optString("profileImageUrl").takeIf { it.isNotBlank() })
                        Column(Modifier.weight(1f)) {
                            Text(displayName(row), style = MaterialTheme.typography.titleMedium)
                            Text(row.optString("username", "—"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusBadge(row.optString("status", "ACTIVE"))
                        if (hasVerifiedProviderRole(row)) Text("✓ Verified", color = AdminSuccess, style = MaterialTheme.typography.labelMedium)
                    }
                    Text(summary(row), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val roles = stringArrayFromRoles(row)
                    if (roles.isNotEmpty()) Text(roles.joinToString(" • "), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                        val id = row.optString("id")
                        scope.launch { api.get("/admin/people/" + id).onSuccess { selected = dataObject(it) }.onFailure { error = it.message } }
                        }) { Text("View details") }
                        TextButton(onClick = { editing = row; showForm = true }) { Text("Edit") }
                    }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AdminAvatar(displayName(row), row.optString("profileImageUrl").takeIf { it.isNotBlank() })
                        Column {
                            Text(displayName(row), style = MaterialTheme.typography.titleMedium)
                            Text(row.optString("username", "—"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("Username: " + row.optString("username", "—"))
                    Text("Email: " + row.optString("email", "—"))
                    Text("Status: " + row.optString("status", "—"))
                    Text("Language: " + row.optString("preferredLanguage", "—"))
                    Text("Roles: " + rolesText(row))
                    if (hasVerifiedProviderRole(row)) Text("✓ Provider verified", color = AdminSuccess, style = MaterialTheme.typography.labelMedium)
                    Text("Created: " + row.optString("createdAt", "—"))
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } },
            dismissButton = { TextButton(onClick = { val id=row.optString("id"); scope.launch { api.delete("/admin/people/$id").onSuccess { selected=null; refreshKey++ }.onFailure { error=it.message } } }) { Text("Deactivate") } }
        )
    }
    if (showForm) PersonForm(api, editing, { showForm=false; refreshKey++ }, { error=it })
}

@Composable
private fun PersonForm(
    api: ApiClient,
    existing: JSONObject?,
    onDone: () -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(existing?.optString("name").orEmpty()) }
    var username by remember { mutableStateOf(existing?.optString("username").orEmpty()) }
    var email by remember { mutableStateOf(existing?.optString("email").orEmpty()) }
    var phone by remember { mutableStateOf(existing?.optString("phone").orEmpty()) }
    var password by remember { mutableStateOf("") }
    var role by remember {
        mutableStateOf(
            existing?.optJSONArray("roles")?.optJSONObject(0)?.optString("role") ?: "RIDER"
        )
    }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(if (existing == null) "Add person" else "Edit person") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 520.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(if (existing == null) "Password (8+ chars required)" else "Password (optional)")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("RIDER", "DRIVER", "GUIDE").forEach { selectedRole ->
                        FilterChip(
                            selected = role == selectedRole,
                            onClick = { role = selectedRole },
                            label = { Text(selectedRole) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy &&
                    name.isNotBlank() &&
                    username.isNotBlank() &&
                    (existing != null || password.length >= 8),
                onClick = {
                    scope.launch {
                        busy = true
                        val body = JSONObject()
                            .put("name", name.trim())
                            .put("username", username.trim())
                            .put("email", email)
                            .put("phone", phone)
                            .put("role", role)
                        if (password.isNotBlank()) body.put("password", password)

                        val result = if (existing == null) {
                            api.post("/admin/people", body)
                        } else {
                            api.patch("/admin/people/" + existing.optString("id"), body)
                        }
                        result
                            .onSuccess { onDone() }
                            .onFailure { onError(it.message ?: "Save failed") }
                        busy = false
                    }
                }
            ) {
                Text(if (existing == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDone) { Text("Cancel") }
        }
    )
}

private fun stringArrayFromRoles(row: JSONObject): List<String> {
    val roles = row.optJSONArray("roles") ?: return emptyList()
    return (0 until roles.length()).mapNotNull { roles.optJSONObject(it)?.optString("role") }.filter { it.isNotBlank() }
}

private fun rolesText(row: JSONObject): String = stringArrayFromRoles(row).joinToString(", ").ifBlank { "—" }

private fun hasVerifiedProviderRole(row: JSONObject): Boolean {
    val roles = row.optJSONArray("roles") ?: return false
    for (i in 0 until roles.length()) {
        val r = roles.optJSONObject(i) ?: continue
        if ((r.optString("role") == "DRIVER" || r.optString("role") == "GUIDE") && r.optString("verificationStatus") == "VERIFIED") return true
    }
    return false
}
