package com.hellokurukshetra.admin.feature.promotions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hellokurukshetra.admin.data.ApiClient
import com.hellokurukshetra.admin.ui.*
import org.json.JSONObject
@Composable
fun PromotionsScreen(api: ApiClient) {
 var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
 var error by remember { mutableStateOf<String?>(null) }
 var create by remember { mutableStateOf(false) }
 var code by remember { mutableStateOf("") }
 var title by remember { mutableStateOf("") }
 var value by remember { mutableStateOf("10") }
 var action by remember { mutableStateOf<String?>(null) }
 var submit by remember { mutableStateOf(false) }
 LaunchedEffect(Unit) { api.get("/admin/promotions?limit=100").onSuccess { rows = extract(it, "items") }.onFailure { error = it.message } }
 LaunchedEffect(action) {
  val id = action ?: return@LaunchedEffect
  api.post("/admin/promotions/" + id + "/deactivate").onFailure { error = it.message }
  action = null
 }
 LaunchedEffect(submit) {
  if (!submit) return@LaunchedEffect
  if (code.isNotBlank() && title.isNotBlank()) api.post("/admin/promotions", JSONObject().put("code", code.trim().uppercase()).put("title", title.trim()).put("discountType", "PERCENTAGE").put("discountValue", value.toDoubleOrNull() ?: 0).put("isActive", true)).onFailure { error = it.message }
  submit = false
  create = false
 }
 LaunchedEffect(create) {
  if (create && code.isNotBlank() && title.isNotBlank()) {
   api.post("/admin/promotions", JSONObject().put("code", code.trim().uppercase()).put("title", title.trim()).put("discountType", "PERCENTAGE").put("discountValue", value.toDoubleOrNull() ?: 0).put("isActive", true))
     .onFailure { error = it.message }
   create = false
  }
 }
 LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
  item { PageHeader("Promotions", "Create and deactivate customer offers."); Button(onClick = { create = true }) { Text("Create promotion") } }
  error?.let { item { ErrorBanner(it) } }
  items(rows, key = { it.optString("id") }) { row -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(displayName(row), style = MaterialTheme.typography.titleMedium); StatusBadge(row.optString("status", "ACTIVE")) }; Text(summary(row)); TextButton(onClick = { action = row.optString("id") }) { Text("Deactivate") } } } }
 }
 if (create) AlertDialog(onDismissRequest = { create = false }, title = { Text("Create promotion") },
  text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(code, { code = it }, label = { Text("Code") }); OutlinedTextField(title, { title = it }, label = { Text("Title") }); OutlinedTextField(value, { value = it }, label = { Text("Discount %") }) } },
  confirmButton = { Button(onClick = { submit = true }) { Text("Save") } }, dismissButton = { TextButton(onClick = { create = false }) { Text("Cancel") } })
}