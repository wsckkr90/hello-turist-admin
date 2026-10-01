package com.hellokurukshetra.admin.feature.promotions

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
import java.time.Instant
import java.time.temporal.ChronoUnit

@Composable
fun PromotionsScreen(api: ApiClient) {
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var create by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("10") }
    var deactivateId by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(search, refreshKey) {
        loading = true
        error = null
        val query = "/admin/promotions?limit=100" + if (search.isBlank()) "" else "&search=" + java.net.URLEncoder.encode(search.trim(), "UTF-8")
        api.get(query).onSuccess { rows = extract(it, "items") }.onFailure { error = it.message ?: "Unable to load promotions" }
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            PageHeader(
                "Promotions",
                "Create, review and deactivate customer offers.",
                { refreshKey++ },
                loading,
                action = { Button(onClick = { create = true }) { Text("Create promotion") } }
            )
        }
        item {
            OutlinedTextField(search, { search = it }, label = { Text("Search code or title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        error?.let { item { ErrorBanner(it) { refreshKey++ } } }
        if (loading && rows.isEmpty()) item { LoadingState("Loading promotions…") }
        if (!loading && rows.isEmpty() && error == null) item { EmptyState("No promotions found.") }

        items(rows, key = { it.optString("id") }) { row ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(row.optString("code", displayName(row)), style = MaterialTheme.typography.titleMedium)
                        StatusBadge(if (row.optBoolean("isActive", true)) "ACTIVE" else "CLOSED")
                    }
                    Text(row.optString("title", "Promotion"))
                    Text("Discount: " + row.opt("discountValue").toString() + " " + row.optString("discountType", ""))
                    Text(summary(row), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (row.optBoolean("isActive", true)) TextButton(onClick = { deactivateId = row.optString("id") }) { Text("Deactivate") }
                }
            }
        }
    }

    if (create) {
        AlertDialog(
            onDismissRequest = { create = false },
            title = { Text("Create promotion") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(code, { code = it }, label = { Text("Code") }, singleLine = true)
                    OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value, { value = it }, label = { Text("Discount %") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(enabled = code.trim().length >= 2 && title.trim().length >= 2, onClick = {
                    val body = JSONObject()
                        .put("code", code.trim().uppercase())
                        .put("title", title.trim())
                        .put("discountType", "PERCENTAGE")
                        .put("discountValue", value.toDoubleOrNull() ?: -1)
                        .put("startsAt", Instant.now().toString())
                        .put("expiresAt", Instant.now().plus(30, ChronoUnit.DAYS).toString())
                        .put("isActive", true)
                    scope.launch {
                        api.post("/admin/promotions", body)
                            .onSuccess {
                                create = false
                                code = ""
                                title = ""
                                value = "10"
                                refreshKey++
                            }
                            .onFailure { error = it.message ?: "Unable to create promotion" }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { create = false }) { Text("Cancel") } }
        )
    }

    deactivateId?.let { id ->
        AlertDialog(
            onDismissRequest = { deactivateId = null },
            title = { Text("Deactivate promotion") },
            text = { Text("This offer will no longer be active for new rides.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        api.post("/admin/promotions/" + id + "/deactivate")
                            .onSuccess { deactivateId = null; refreshKey++ }
                            .onFailure { error = it.message ?: "Unable to deactivate promotion" }
                    }
                }) { Text("Deactivate") }
            },
            dismissButton = { TextButton(onClick = { deactivateId = null }) { Text("Cancel") } }
        )
    }
}
