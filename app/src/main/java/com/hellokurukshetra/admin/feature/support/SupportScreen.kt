package com.hellokurukshetra.admin.feature.support
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
@Composable fun SupportScreen(api:ApiClient){var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var error by remember{mutableStateOf<String?>(null)};var target by remember{mutableStateOf<String?>(null)};var message by remember{mutableStateOf("")};LaunchedEffect(Unit){api.get("/admin/support/tickets?limit=100").onSuccess{rows=extract(it,"items")}.onFailure{error=it.message}};LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{PageHeader("Support tickets","Handle customer issues with a clear audit trail.")};error?.let{item{ErrorBanner(it)}};items(rows,key={it.optString("id")}){r->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(displayName(r),style=MaterialTheme.typography.titleMedium);StatusBadge(r.optString("status","OPEN"))};Text(summary(r));TextButton({target=r.optString("id")}){Text("Reply")}}}}};target?.let{id->AlertDialog(onDismissRequest={target=null},title={Text("Reply to ticket")},text={OutlinedTextField(message,{message=it},label={Text("Message")},minLines=3)},confirmButton={Button({api.post("/admin/support/tickets/"+id+"/reply",JSONObject().put("message",message.trim()));target=null;message=""}){Text("Send")}},dismissButton={TextButton({target=null}){Text("Cancel")}})}}}
