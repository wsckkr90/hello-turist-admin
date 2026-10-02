package com.hellokurukshetra.admin.feature.emergency

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hellokurukshetra.admin.data.ApiClient
import com.hellokurukshetra.admin.ui.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun EmergencyScreen(api: ApiClient) {
    val context = LocalContext.current
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var action by remember { mutableStateOf<Pair<String, String>?>(null) }
    var responderId by remember { mutableStateOf("") }
    var stateFilter by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        api.get(buildString {
                append("/admin/emergency/incidents?limit=100")
                if (stateFilter.isNotBlank()) append("&state=").append(stateFilter)
                if (categoryFilter.isNotBlank()) append("&category=").append(java.net.URLEncoder.encode(categoryFilter.trim(), "UTF-8"))
            })
            .onSuccess { rows = extract(it, "items") }
            .onFailure { error = it.message ?: "Unable to load emergency incidents" }
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { PageHeader("Emergency response", "Monitor incidents, state transitions and responders.", { refreshKey++ }, loading) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(selected = stateFilter.isBlank(), onClick = { stateFilter = "" }, label = { Text("All states") })
                FilterChip(selected = stateFilter == "TRIGGERED", onClick = { stateFilter = "TRIGGERED" }, label = { Text("Triggered") })
                FilterChip(selected = stateFilter == "ACKNOWLEDGED", onClick = { stateFilter = "ACKNOWLEDGED" }, label = { Text("Acknowledged") })
                FilterChip(selected = stateFilter == "ESCALATED", onClick = { stateFilter = "ESCALATED" }, label = { Text("Escalated") })
                FilterChip(selected = stateFilter == "RESOLVED", onClick = { stateFilter = "RESOLVED" }, label = { Text("Resolved") })
            }
        }
        item {
            OutlinedTextField(
                categoryFilter,
                { categoryFilter = it },
                label = { Text("Filter category") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
                    val rider = detail.optJSONObject("rider")
                    item { Text("Rider", style = MaterialTheme.typography.labelLarge) }
                    item { Text((rider?.optString("name", "—") ?: "—") + " • " + (rider?.optString("username", "—") ?: "—")) }
                    val ride = detail.optJSONObject("ride")
                    item { Text("Ride: " + ride?.optString("status", "—")) }
                    val assignments = ride?.optJSONArray("assignments")
                    item { Text("Assigned driver", style = MaterialTheme.typography.labelLarge) }
                    if (assignments == null || assignments.length() == 0) {
                        item { Text("No driver assignment found.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        repeat(assignments.length()) { index ->
                            val assignment = assignments.optJSONObject(index) ?: JSONObject()
                            val driver = assignment.optJSONObject("driver")
                            item { Text("• " + (driver?.optString("name", "Unknown") ?: "Unknown") + " — " + assignment.optString("status", "—")) }
                        }
                    }
                    val snapshot = incident?.optJSONObject("locationSnapshot")
                    val lat = snapshot?.optDouble("latitude", Double.NaN)
                    val lon = snapshot?.optDouble("longitude", Double.NaN)
                    item { Text("Emergency location", style = MaterialTheme.typography.labelLarge) }
                    if (lat != null && lon != null && !lat.isNaN() && !lon.isNaN()) {
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(String.format(Locale.US, "%.6f, %.6f", lat, lon), modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon"))) }
                                }) { Text("Map") }
                            }
                        }
                    } else {
                        item { Text("No location snapshot available.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    val locations = ride?.optJSONArray("locations")
                    item { Text("Ride location trail", style = MaterialTheme.typography.labelLarge) }
                    if (locations == null || locations.length() == 0) {
                        item { Text("No recorded ride locations.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        repeat(minOf(locations.length(), 10)) { index ->
                            val point = locations.optJSONObject(index) ?: JSONObject()
                            val pLat = point.optString("latitude")
                            val pLon = point.optString("longitude")
                            item {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("$pLat, $pLon", modifier = Modifier.weight(1f))
                                    TextButton(onClick = {
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$pLat,$pLon?q=$pLat,$pLon"))) }
                                    }) { Text("Map") }
                                }
                            }
                        }
                    }
                    val rideEvents = ride?.optJSONArray("events")
                    val mediaEvents = rideEvents?.let { events ->
                        (0 until events.length()).mapNotNull { i ->
                            events.optJSONObject(i)?.takeIf { it.optJSONObject("payload")?.optString("kind") == "RECORDING_UPLOADED" }
                        }
                    }.orEmpty()
                    item { Text("Safety media", style = MaterialTheme.typography.labelLarge) }
                    if (mediaEvents.isEmpty()) {
                        item { Text("No audio/video recording metadata found.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        mediaEvents.forEach { event ->
                            val payload = event.optJSONObject("payload") ?: JSONObject()
                            item { Text("• " + payload.optString("media", "MEDIA") + " • " + payload.optString("contentType", "—") + " • expires " + payload.optString("expiresAt", "—")) }
                        }
                    }
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
                        AdminPersonPicker(
                            api = api,
                            label = "Responder",
                            selectedId = responderId,
                            onSelected = { responderId = it },
                            modifier = Modifier.fillMaxWidth()
                        )
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
