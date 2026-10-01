package com.hellokurukshetra.admin.feature.adminusers
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
@Composable fun AdminUsersScreen(api:ApiClient){var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var error by remember{mutableStateOf<String?>(null)};var selected by remember{mutableStateOf<String?>(null)};var permissions by remember{mutableStateOf("")};LaunchedEffect(Unit){api.get("/admin/users?limit=100").onSuccess{rows=extract(it,"items")}.onFailure{error=it.message}};LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{PageHeader("Admin users","Manage administrator access and permissions.")};error?.let{item{ErrorBanner(it)}};items(rows,key={it.optString("id")}){r->val id=r.optString("id");Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(displayName(r),style=MaterialTheme.typography.titleMedium);StatusBadge(r.optString("status","ACTIVE"))};Text(summary(r));TextButton({api.get("/admin/rbac/users/"+id+"/permissions").onSuccess{val d=dataObject(it);val a=d.optJSONArray("permissions");permissions=if(a==null)"" else (0 until a.length()).joinToString(","){i->a.optString(i)};selected=id}.onFailure{error=it.message}}){Text("Edit permissions")}}}}};selected?.let{id->AlertDialog(onDismissRequest={selected=null},title={Text("Permissions")},text={OutlinedTextField(permissions,{permissions=it},label={Text("Comma-separated permissions")},minLines=4)},confirmButton={Button({api.put("/admin/rbac/users/"+id+"/permissions",JSONObject().put("permissions",permissions.split(",").map{it.trim()}.filter{it.isNotBlank()}));selected=null}){Text("Save")}},dismissButton={TextButton({selected=null}){Text("Cancel")}})}}}
