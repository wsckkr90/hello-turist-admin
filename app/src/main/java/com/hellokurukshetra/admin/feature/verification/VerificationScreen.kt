package com.hellokurukshetra.admin.feature.verification

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hellokurukshetra.admin.data.ApiClient
import com.hellokurukshetra.admin.ui.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun VerificationScreen(api: ApiClient) {
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var decisionId by remember { mutableStateOf<String?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    suspend fun load() {
        loading = true
        error = null
        try {
            val result = coroutineScope {
                listOf("DRIVER", "GUIDE").map { role ->
                    async {
                        api.get("/admin/verification/requests?role=$role&limit=100")
                    }
                }.awaitAll()
            }
            val all = mutableListOf<JSONObject>()
            result.forEach { response ->
                response.onSuccess { all.addAll(extract(it, "items")) }
                    .onFailure { error = it.message ?: "Unable to load verification requests" }
            }
            rows = all.filter {
                val status = it.optString("status")
                status == "PENDING" || status == "UNDER_VERIFICATION" || status == "RESUBMITTED"
            }.distinctBy { it.optString("id") }
                .sortedByDescending { it.optString("createdAt") }
        } finally {
            loading = false
        }
    }

    LaunchedEffect(refreshKey) { load() }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            PageHeader("Provider verification", "Review driver and guide applications before activation.", { refreshKey++ }, loading)
        }
        error?.let { item { ErrorBanner(it) { refreshKey++ } } }
        if (loading && rows.isEmpty()) item { LoadingState("Loading provider applications…") }
        if (!loading && rows.isEmpty() && error == null) item { EmptyState("No provider applications are waiting for review.") }

        items(rows, key = { it.optString("id") }) { row ->
            val user = row.optJSONObject("user")
            val role = row.optString("role", "PROVIDER")
            val documents = row.optJSONArray("documents")?.length() ?: 0
            val live = row.optJSONObject("liveSession")?.optString("status").orEmpty()
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(user?.optString("name").orEmpty().ifBlank { "Provider" }, style = MaterialTheme.typography.titleMedium)
                            Text(role, style = MaterialTheme.typography.labelMedium, color = AdminInfo)
                        }
                        StatusBadge(row.optString("status", "PENDING"))
                    }
                    Text("Documents: $documents • Live verification: " + live.ifBlank { "Not started" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val id = row.optString("id")
                            scope.launch { api.get("/admin/verification/requests/" + id).onSuccess { selected = dataObject(it) }.onFailure { error = it.message } }
                        }) { Text("Review") }
                        OutlinedButton(onClick = {
                            val phone = user?.optString("phone").orEmpty()
                            val url = if (phone.isNotBlank()) "https://wa.me/" + phone.filter { it.isDigit() } else "https://www.whatsapp.com/"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }) { Text("WhatsApp") }
                        Button(onClick = {
                            val id = row.optString("id")
                            scope.launch {
                                val phone = user?.optString("phone").orEmpty()
                                api.post("/admin/verification/requests/$id/live-whatsapp", JSONObject().put("phone", phone))
                                    .onSuccess { refreshKey++ }
                                    .onFailure { error = it.message ?: "Unable to start live verification" }
                            }
                        }) { Text("Start Live") }
                        Button(onClick = { decisionId = row.optString("id") }) { Text("Approve") }
                        OutlinedButton(onClick = { decisionId = "REJECT:" + row.optString("id") }) { Text("Reject") }
                    }
                }
            }
        }
    }

    decisionId?.let { token ->
        val reject = token.startsWith("REJECT:")
        val id = if (reject) token.removePrefix("REJECT:") else token
        AlertDialog(
            onDismissRequest = { decisionId = null; rejectionReason = "" },
            title = { Text(if (reject) "Reject verification" else "Approve verification") },
            text = {
                if (reject) {
                    OutlinedTextField(
                        rejectionReason,
                        { rejectionReason = it },
                        label = { Text("Rejection reason") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("The backend will validate documents and live verification before approval.")
                }
            },
            confirmButton = {
                Button(
                    enabled = !reject || rejectionReason.isNotBlank(),
                    onClick = {
                        val body = JSONObject().put("status", if (reject) "REJECTED" else "VERIFIED")
                        if (reject) body.put("rejectionReason", rejectionReason.trim())
                        scope.launch {
                            api.patch("/admin/verification/requests/$id", body)
                                .onSuccess { refreshKey++ }
                                .onFailure { error = it.message ?: "Unable to update verification" }
                        }
                        decisionId = null
                        rejectionReason = ""
                    }
                ) { Text(if (reject) "Reject" else "Approve") }
            },
            dismissButton = { TextButton(onClick = { decisionId = null; rejectionReason = "" }) { Text("Cancel") } }
        )
    }

    selected?.let { row ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Verification details") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { Text("Applicant: " + row.optJSONObject("user")?.optString("name", "—")) }
                    item { Text("Role: " + row.optString("role", "—")) }
                    item { Text("Status: " + row.optString("status", "—")) }
                    item { Text("Documents: " + (row.optJSONArray("documents")?.length() ?: 0)) }
                    item { Text("Phone: " + row.optJSONObject("user")?.optString("phone", "—")) }
                    item { Text("Live session reference: " + row.optJSONObject("liveSession")?.optString("providerReference", "—")) }
                    item { Text("Live session started: " + row.optJSONObject("liveSession")?.optString("startedAt", "—")) }
                    item {
                        Text("Documents", style = MaterialTheme.typography.titleSmall)
                        val docs = row.optJSONArray("documents")
                        if (docs == null || docs.length() == 0) Text("No documents uploaded yet.")
                        else for (i in 0 until docs.length()) {
                            val d = docs.optJSONObject(i)
                            Text("• " + d?.optString("documentType", "Document") + " — " + d?.optString("verificationStatus", "PENDING") +
                                (d?.optString("expiryDate")?.takeIf { it.isNotBlank() }?.let { " • Expiry: $it" } ?: ""))
                        }
                    }
                    item { Text("Live session: " + row.optJSONObject("liveSession")?.optString("status", "—")) }
                    item { Text("Created: " + row.optString("createdAt", "—")) }
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }
        )
    }
}

