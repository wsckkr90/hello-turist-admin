package com.hellokurukshetra.admin.feature.promotions
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
@Composable fun PromotionsScreen(api:ApiClient){var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var error by remember{mutableStateOf<String?>(null)};var create by remember{mutableStateOf(false)};var code by remember{mutableStateOf("")};var title by remember{mutableStateOf("")};var value by remember{mutableStateOf("10")};LaunchedEffect(Unit){api.get("/admin/promotions?limit=100").onSuccess{rows=extract(it,"items")}.onFailure{error=it.message}};LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{PageHeader("Promotions","Create and deactivate customer offers.");Button({create=true}){Text("Create promotion")}};error?.let{item{ErrorBanner(it)}};items(rows,key={it.optString("id")}){r->val id=r.optString("id");Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(displayName(r),style=MaterialTheme.typography.titleMedium);StatusBadge(r.optString("status",if(r.optBoolean("isActive",true))"ACTIVE" else "INACTIVE"))};Text(summary(r));TextButton({api.post("/admin/promotions/"+id+"/deactivate")}){Text("Deactivate")}}}}};if(create)AlertDialog(onDismissRequest={create=false},title={Text("Create promotion")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(code,{code=it},label={Text("Code")});OutlinedTextField(title,{title=it},label={Text("Title")});OutlinedTextField(value,{value=it},label={Text("Discount %")})}},confirmButton={Button({api.post("/admin/promotions",JSONObject().put("code",code.trim().uppercase()).put("title",title.trim()).put("discountType","PERCENTAGE").put("discountValue",value.toDoubleOrNull()?:0).put("isActive",true));create=false}){Text("Save")}},dismissButton={TextButton({create=false}){Text("Cancel")}})}}
