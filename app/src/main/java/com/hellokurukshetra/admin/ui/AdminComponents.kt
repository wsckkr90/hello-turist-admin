package com.hellokurukshetra.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

val AdminNavy = Color(0xFF07111F)
val AdminNavySoft = Color(0xFF102238)
val AdminGold = Color(0xFFFFC107)
val AdminSurface = Color(0xFFF6F8FB)
val AdminCard = Color.White
val AdminSuccess = Color(0xFF16834A)
val AdminDanger = Color(0xFFC62828)
val AdminWarning = Color(0xFF9A6700)
val AdminInfo = Color(0xFF1769AA)

@Composable
fun PageHeader(title: String, subtitle: String, onRefresh: (() -> Unit)? = null, refreshing: Boolean = false, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (action != null) action()
        if (onRefresh != null) {
            OutlinedButton(onClick = onRefresh, enabled = !refreshing) {
                if (refreshing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Refresh")
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AdminCard)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
fun LoadingState(message: String = "Loading…") {
    Box(Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator()
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ErrorBanner(message: String, onRetry: (() -> Unit)? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE8E8))) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), color = AdminDanger)
            if (onRetry != null) TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun SuccessBanner(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F7EE))) {
        Text(message, Modifier.padding(14.dp), color = AdminSuccess, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(message: String = "No records found") {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatusBadge(value: String) {
    val normalized = value.uppercase()
    val background = when (normalized) {
        "VERIFIED", "APPROVED", "COMPLETED", "RESOLVED", "ACTIVE", "CAPTURED", "IN_PROGRESS", "ACKNOWLEDGED", "ARRIVED", "EN_ROUTE" -> Color(0xFFDDF7E5)
        "REJECTED", "FAILED", "CANCELLED", "CLOSED", "EXPIRED" -> Color(0xFFFFE3E3)
        "TRIGGERED", "ESCALATED", "PENDING", "PROCESSING", "UNDER_VERIFICATION", "RESUBMITTED" -> Color(0xFFFFF3CD)
        else -> Color(0xFFE9EEF5)
    }
    val foreground = when (normalized) {
        "VERIFIED", "APPROVED", "COMPLETED", "RESOLVED", "ACTIVE", "CAPTURED", "IN_PROGRESS", "ACKNOWLEDGED", "ARRIVED", "EN_ROUTE" -> AdminSuccess
        "REJECTED", "FAILED", "CANCELLED", "CLOSED", "EXPIRED" -> AdminDanger
        "TRIGGERED", "ESCALATED", "PENDING", "PROCESSING", "UNDER_VERIFICATION", "RESUBMITTED" -> AdminWarning
        else -> AdminNavySoft
    }
    Surface(color = background, shape = MaterialTheme.shapes.small) {
        Text(normalized.replace('_', ' '), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = foreground, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun Metric(label: String, value: String, modifier: Modifier = Modifier, detail: String? = null) {
    Card(modifier) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (!detail.isNullOrBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PaginationBar(page: Int, totalPages: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    if (totalPages <= 1) return
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        Text("Page $page of $totalPages", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        OutlinedButton(onClick = onPrevious, enabled = page > 1) { Text("Previous") }
        Spacer(Modifier.width(6.dp))
        Button(onClick = onNext, enabled = page < totalPages) { Text("Next") }
    }
}

fun dataObject(root: JSONObject): JSONObject = root.optJSONObject("data") ?: JSONObject()

fun extract(root: JSONObject, key: String): List<JSONObject> {
    val data = root.optJSONObject("data")
    val array = root.optJSONArray(key) ?: data?.optJSONArray(key) ?: return emptyList()
    return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
}

fun pagination(root: JSONObject): JSONObject {
    val data = root.optJSONObject("data") ?: JSONObject()
    return data.optJSONObject("pagination") ?: root.optJSONObject("pagination") ?: JSONObject()
}

fun displayName(j: JSONObject): String =
    j.optString("name").ifBlank {
        j.optString("title").ifBlank {
            j.optString("subject").ifBlank {
                j.optString("code").ifBlank { j.optString("id", "Record") }
            }
        }
    }

fun summary(j: JSONObject): String {
    val preferred = listOf("email", "username", "subject", "pickupAddress", "dropoffAddress", "providerPaymentId", "reason", "category", "message", "preferredLanguage")
    val values = preferred.mapNotNull { key ->
        val value = j.opt(key)
        if (value == null || value == JSONObject.NULL || value.toString().isBlank()) null
        else "$key: $value"
    }
    return values.take(4).joinToString(" • ").ifBlank { "ID: \${j.optString("id")}" }
}

fun stringArray(json: JSONObject?, key: String): List<String> {
    val array: JSONArray = json?.optJSONArray(key) ?: return emptyList()
    return (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }
}
