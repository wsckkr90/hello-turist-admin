package com.hellokurukshetra.admin.feature.payments
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
@Composable fun PaymentsScreen(api:ApiClient){var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var error by remember{mutableStateOf<String?>(null)};LaunchedEffect(Unit){api.get("/admin/finance/payments?limit=100").onSuccess{rows=extract(it,"items")}.onFailure{error=it.message}};LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{PageHeader("Payments & refunds","Review payment status and initiate authorised refunds.")};error?.let{item{ErrorBanner(it)}};items(rows,key={it.optString("id")}){r->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(displayName(r),style=MaterialTheme.typography.titleMedium);StatusBadge(r.optString("status","UNKNOWN"))};Text(summary(r));TextButton(onClick={api.post("/admin/finance/payments/"+r.optString("id")+"/refund",JSONObject().put("reason","Admin refund"))}){Text("Request refund")}}}}}}
