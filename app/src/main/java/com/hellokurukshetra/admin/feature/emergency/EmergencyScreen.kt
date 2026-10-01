package com.hellokurukshetra.admin.feature.emergency

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
fun EmergencyScreen(api: ApiClient) {
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var action by remember { mutableStateOf<Pair<String, String>?>(null) }
    var responderId by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        api.get("/admin/emergency/incidents?limit=100")
            .onSuccess { rows = extract(it, "items") }
            .onFailure { error = it.message ?: "Unable to load emergency incidents" }
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { PageHeader("Emergency response", "Monitor incidents, state transitions and responders.", { refreshKey++ }, loading) }
        error?.let { item { ErrorBanner(it) { refreshKey++ } } }
        if (loading && rows.isEmpty()) item { LoadingState("Loading emergency incidents…") }
        if (!loading && rows.isEmpty() && error == null) item { EmptyState("No emergency incidents.") }

        items(rows, key = { it.optString("id") }) { row ->
            val id = row.optString("id")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(row.optString("category", "Emergency"), style = MaterialTheme.typography.titleMedium)
                        StatusBadge(row.optString("state", row.optString("status", "TRIGGERED")))
                    }
                    Text("Rider: " + row.optString("riderName", row.optString("riderUsername", "—")))
                    Text("Ride: " + row.optString("rideId", "—"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = {
                            scope.launch { api.get("/admin/emergency/incidents/" + id).onSuccess { selected = dataObject(it) }.onFailure { error = it.message } }
                        }) { Text("Details") }
                        TextButton(onClick = { action = id to "acknowledge" }) { Text("Acknowledge") }
                        TextButton(onClick = { action = id to "escalate" }) { Text("Escalate") }
                        TextButton(onClick = { action = id to "resolve" }) { Text("Resolve") }
                    }
                }
            }
        }
    }

    action?.let { current ->
        AlertDialog(
            onDismissRequest = { action = null },
            title = { Text(current.second.replaceFirstChar { it.uppercase() }) },
            text = { Text("Confirm this emergency state transition? The backend will validate the current incident state.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        api.post("/admin/emergency/incidents/" + current.first + "/" + current.second)
                            .onSuccess { action = null; refreshKey++ }
                            .onFailure { error = it.message ?: "Emergency action failed" }
                    }
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { action = null }) { Text("Cancel") } }
        )
    }

    selected?.let { detail ->
        val incident = detail.optJSONObject("incident")
        val responders = detail.optJSONArray("responders")
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Emergency details") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Text("State: " + incident?.optString("state", "—")) }
                    item { Text("Category: " + incident?.optString("category", "—")) }
                    item { Text("Trigger: " + incident?.optString("triggerSource", "—")) }
                    item { Text("Rider: " + detail.optJSONObject("rider")?.optString("name", "—")) }
                    item { Text("Ride: " + detail.optJSONObject("ride")?.optString("status", "—")) }
                    item {
                        Text("Responders", style = MaterialTheme.typography.titleMedium)
                    }
                    if (responders == null || responders.length() == 0) {
                        item { Text("No responders assigned.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(responders.length()) { index ->
                            val responder = responders.optJSONObject(index) ?: JSONObject()
                            Column {
                                Text(responder.optString("responderId", "Responder"))
                                StatusBadge(responder.optString("status", "ASSIGNED"))
                                if (responder.optString("status") != "COMPLETED") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = {
                                            val assignmentId = responder.optString("id")
                                            scope.launch {
                                                api.patch("/admin/emergency/responders/" + assignmentId, JSONObject().put("status", "EN_ROUTE"))
                                                    .onSuccess { refreshKey++ }
                                                    .onFailure { error = it.message }
                                            }
                                        }) { Text("En route") }
                                        TextButton(onClick = {
                                            val assignmentId = responder.optString("id")
                                            scope.launch {
                                                api.patch("/admin/emergency/responders/" + assignmentId, JSONObject().put("status", "ARRIVED"))
                                                    .onSuccess { refreshKey++ }
                                                    .onFailure { error = it.message }
                                            }
                                        }) { Text("Arrived") }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        OutlinedTextField(responderId, { responderId = it }, label = { Text("Responder user ID") }, singleLine = true)
                    }
                    item {
                        Button(
                            enabled = responderId.isNotBlank() && incident?.optString("state") != "RESOLVED",
                            onClick = {
                                val incidentId = incident?.optString("id").orEmpty()
                                scope.launch {
                                    api.post("/admin/emergency/incidents/" + incidentId + "/responders", JSONObject().put("responderId", responderId.trim()))
                                        .onSuccess {
                                            responderId = ""
                                            selected = null
                                            refreshKey++
                                        }
                                        .onFailure { error = it.message }
                                }
                            }
                        ) { Text("Assign responder") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }
        )
    }
}
