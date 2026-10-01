package com.hellokurukshetra.admin.feature.rides
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
@Composable fun RidesScreen(api:ApiClient){var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var error by remember{mutableStateOf<String?>(null)};var action by remember{mutableStateOf<Pair<String,String>?>(null)};var input by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};LaunchedEffect(Unit){api.get("/admin/rides?limit=100").onSuccess{rows=extract(it,"items")}.onFailure{error=it.message}};LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{PageHeader("Ride operations","Monitor rides and perform controlled admin interventions.")};error?.let{item{ErrorBanner(it)}};items(rows,key={it.optString("id")}){r->val id=r.optString("id");Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(displayName(r),style=MaterialTheme.typography.titleMedium);StatusBadge(r.optString("status","UNKNOWN"))};Text(summary(r));Row{OutlinedButton({action=id to "assign"}){Text("Assign")};Spacer(Modifier.width(6.dp));OutlinedButton({action=id to "cancel"}){Text("Cancel")};Spacer(Modifier.width(6.dp));OutlinedButton({action=id to "recover"}){Text("Recover")}}}}}};action?.let{(id,type)->AlertDialog(onDismissRequest={if(!busy)action=null},title={Text(type.replaceFirstChar{it.uppercase()}+" ride")},text={OutlinedTextField(input,{input=it},label={Text(if(type=="assign")"Driver ID" else "Reason")},enabled=!busy)},confirmButton={Button(enabled=!busy,onClick={busy=true}){Text("Confirm")}},dismissButton={TextButton(enabled=!busy,onClick={action=null}){Text("Close")}})}}}
