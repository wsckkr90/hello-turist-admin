package com.hellokurukshetra.admin.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hellokurukshetra.admin.data.ApiClient
import com.hellokurukshetra.admin.ui.*

@Composable
fun DashboardScreen(api: ApiClient) {
    var data by remember { mutableStateOf<org.json.JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        api.get("/admin/analytics")
            .onSuccess { data = it }
            .onFailure { error = it.message ?: "Unable to load analytics" }
        loading = false
    }

    val kpis = data?.optJSONObject("kpis")
    val breakdowns = data?.optJSONObject("breakdowns")
    val users = (kpis?.optInt("users") ?: 0).toString()
    val active = (kpis?.optInt("activeUsers") ?: 0).toString()
    val drivers = (kpis?.optInt("approvedDrivers") ?: 0).toString()
    val guides = (kpis?.optInt("approvedGuides") ?: 0).toString()
    val rides = (kpis?.optInt("rides") ?: 0).toString()
    val payments = (kpis?.optInt("payments") ?: 0).toString()
    val verification = (kpis?.optInt("verificationRequests") ?: 0).toString()
    val support = (kpis?.optInt("supportTickets") ?: 0).toString()

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            PageHeader(
                "Operations overview",
                "Live platform health and activity from the admin backend.",
                onRefresh = { refreshKey++ },
                refreshing = loading
            )
        }
        error?.let { message -> item { ErrorBanner(message) { refreshKey++ } } }
        if (loading && data == null) {
            item { LoadingState("Loading dashboard…") }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Metric("Users", users, Modifier.weight(1f), "All accounts")
                    Metric("Active", active, Modifier.weight(1f), "Currently active")
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Metric("Drivers", drivers, Modifier.weight(1f), "Approved")
                    Metric("Guides", guides, Modifier.weight(1f), "Approved")
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Metric("Rides", rides, Modifier.weight(1f))
                    Metric("Payments", payments, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Metric("Verification", verification, Modifier.weight(1f))
                    Metric("Support", support, Modifier.weight(1f))
                }
            }
            item { BreakdownCard("Ride status", breakdowns?.optJSONObject("rides")) }
            item { BreakdownCard("Payment status", breakdowns?.optJSONObject("payments")) }
            item { BreakdownCard("Verification status", breakdowns?.optJSONObject("verificationRequests")) }
            item { BreakdownCard("Support status", breakdowns?.optJSONObject("supportTickets")) }
        }
    }
}

@Composable
private fun BreakdownCard(title: String, json: org.json.JSONObject?) {
    if (json == null) return
    SectionCard(title) {
        for (key in json.keys()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(key.replace('_', ' '), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(json.opt(key).toString(), fontWeight = FontWeight.Bold)
            }
        }
    }
}
