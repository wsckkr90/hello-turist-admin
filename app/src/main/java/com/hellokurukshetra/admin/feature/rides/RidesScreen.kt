package com.hellokurukshetra.admin.feature.rides

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
fun RidesScreen(api: ApiClient) {
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var action by remember { mutableStateOf<Pair<String, String>?>(null) }
    var input by remember { mutableStateOf("") }\n    var driverId by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var page by rememberSaveable { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(page, search, refreshKey) {
        loading = true
        error = null
        val query = "/admin/rides?page=$page&limit=20" +
            if (search.isBlank()) "" else "&search=" + java.net.URLEncoder.encode(search.trim(), "UTF-8")
        api.get(query)
            .onSuccess {
                rows = extract(it, "items")
                totalPages = pagination(it).optInt("totalPages", 1).coerceAtLeast(1)
            }
            .onFailure { error = it.message ?: "Unable to load rides" }
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { PageHeader("Ride operations", "Monitor live ride state and perform controlled interventions.", { refreshKey++ }, loading) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(search, { search = it; page = 1 }, label = { Text("Search ride, pickup, drop-off or rider") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { search = ""; page = 1; refreshKey++ }) { Text("Clear") }
            }
        }
        error?.let { item { ErrorBanner(it) { refreshKey++ } } }
        if (loading && rows.isEmpty()) item { LoadingState("Loading rides…") }
        if (!loading && rows.isEmpty() && error == null) item { EmptyState("No rides match the current search.") }

        items(rows, key = { it.optString("id") }) { row ->
            val id = row.optString("id")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(displayName(row), style = MaterialTheme.typography.titleMedium)
                        StatusBadge(row.optString("status", "UNKNOWN"))
                    }
                    Text(summary(row), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = {
                            scope.launch { api.get("/admin/rides/" + id).onSuccess { selected = dataObject(it) }.onFailure { error = it.message } }
                        }) { Text("Details") }
                        OutlinedButton(onClick = { action = id to "assign"; driverId = "" }) { Text("Assign") }
                        OutlinedButton(onClick = { action = id to "interrupt" }) { Text("Interrupt") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { action = id to "cancel" }) { Text("Cancel") }
                        OutlinedButton(onClick = { action = id to "recover"; driverId = "" }) { Text("Recover") }
                    }
                }
            }
        }
        item { PaginationBar(page, totalPages, { if (page > 1) page-- }, { if (page < totalPages) page++ }) }
    }

    action?.let { current ->
        val assign = current.second == "assign"
        AlertDialog(
            onDismissRequest = { action = null; input = ""; driverId = "" },
            title = { Text(current.second.replaceFirstChar { it.uppercase() } + " ride") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (assign || current.second == "recover") {
                        AdminPersonPicker(
                            api = api,
                            role = "DRIVER",
                            label = if (assign) "Verified driver" else "Replacement driver (optional)",
                            selectedId = driverId,
                            onSelected = { driverId = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (current.second == "recover") {
                            Text(
                                "Leave the driver unselected to let the backend choose an approved available replacement.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        OutlinedTextField(
                            input,
                            { input = it },
                            label = { Text("Reason") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = if (assign) driverId.isNotBlank() else true,
                    onClick = {
                        val body = when (current.second) {
                            "assign" -> JSONObject().put("driverId", driverId)
                            "recover" -> JSONObject().put("reason", input.trim().ifBlank { "Admin recovery" }).apply {
                                if (driverId.isNotBlank()) put("driverId", driverId)
                            }
                            else -> JSONObject().put("reason", input.trim().ifBlank { "Admin intervention" })
                        }
                        scope.launch {
                            val result = when (current.second) {
                                "assign" -> api.post("/admin/rides/" + current.first + "/assign", body)
                                "cancel" -> api.post("/admin/rides/" + current.first + "/cancel", body)
                                "interrupt" -> api.post("/admin/rides/" + current.first + "/interrupt", body)
                                else -> api.post("/admin/rides/" + current.first + "/recover", body)
                            }
                            result.onSuccess { refreshKey++ }.onFailure { error = it.message ?: "Ride action failed" }
                        }
                        action = null
                        input = ""
                    }
                ) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { action = null; input = ""; driverId = "" }) { Text("Close") } }
        )
    }

    selected?.let { row ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Ride details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Rider: " + row.optJSONObject("rider")?.optString("name", "—"))
                    Text("Status: " + row.optString("status", "—"))
                    Text("Pickup: " + row.optString("pickupAddress", "—"))
                    Text("Drop-off: " + row.optString("dropoffAddress", "—"))
                    Text("Created: " + row.optString("createdAt", "—"))
                    Text("Updated: " + row.optString("updatedAt", "—"))
                    Text("Assignments: " + (row.optJSONArray("assignments")?.length() ?: 0))
                    Text("Events: " + (row.optJSONArray("events")?.length() ?: 0))
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }
        )
    }
}
