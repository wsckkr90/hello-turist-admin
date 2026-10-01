package com.hellokurukshetra.admin.feature.emergency
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
@Composable fun EmergencyScreen(api:ApiClient){var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var error by remember{mutableStateOf<String?>(null)};LaunchedEffect(Unit){api.get("/admin/emergency/incidents?limit=100").onSuccess{rows=extract(it,"items").ifEmpty{extract(it,"incidents")}}.onFailure{error=it.message}};LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{PageHeader("Emergency response","Monitor incidents and record response actions.")};error?.let{item{ErrorBanner(it)}};items(rows,key={it.optString("id")}){r->val id=r.optString("id");Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(displayName(r),style=MaterialTheme.typography.titleMedium);StatusBadge(r.optString("status","OPEN"))};Text(summary(r));Row{TextButton({api.post("/admin/emergency/incidents/"+id+"/acknowledge")}){Text("Acknowledge")};TextButton({api.post("/admin/emergency/incidents/"+id+"/escalate")}){Text("Escalate")};TextButton({api.post("/admin/emergency/incidents/"+id+"/resolve")}){Text("Resolve")}}}}}}}
