package com.hellokurukshetra.admin.feature.audit
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hellokurukshetra.admin.data.ApiClient
import com.hellokurukshetra.admin.ui.*
import org.json.JSONObject
@Composable fun AuditScreen(api:ApiClient){var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var error by remember{mutableStateOf<String?>(null)};LaunchedEffect(Unit){api.get("/admin/audit-logs?limit=100").onSuccess{rows=extract(it,"items")}.onFailure{error=it.message}};LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{PageHeader("Audit logs","A chronological record of administrator actions.")};error?.let{item{ErrorBanner(it)}};if(rows.isEmpty()&&error==null)item{EmptyState()};items(rows){r->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(displayName(r),style=MaterialTheme.typography.titleMedium);Text(summary(r),color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}
