package com.hellokurukshetra.admin.feature.content

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hellokurukshetra.admin.data.ApiClient
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

private data class ContentItem(val id:String,val name:String,val description:String?,val address:String?,val city:String?,val latitude:Double?,val longitude:Double?,val imageUrl:String?,val phone:String?,val email:String?,val website:String?,val isActive:Boolean)

@Composable
fun ContentScreen(api: ApiClient) {
    var type by rememberSaveable { mutableStateOf("homestays") }
    var items by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ContentItem?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun reload() { scope.launch { loading=true; api.get("/admin/content/$type").onSuccess { root ->
        val a=root.optJSONArray("data"); items=(0 until (a?.length()?:0)).map { i -> val o=a!!.getJSONObject(i); ContentItem(o.optString("id"),o.optString("name"),o.optString("description").ifBlank{null},o.optString("address").ifBlank{null},o.optString("city").ifBlank{null},o.optDoubleOrNull("latitude"),o.optDoubleOrNull("longitude"),o.optString("imageUrl").ifBlank{null},o.optString("phone").ifBlank{null},o.optString("email").ifBlank{null},o.optString("website").ifBlank{null},o.optBoolean("isActive",true)) }
    }.onFailure{error=it.message};loading=false } }
    LaunchedEffect(type){reload()}
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("homestays" to "Home Stays","sponsors" to "Sponsors","business-partners" to "Business Partners").forEach{(k,l)->FilterChip(type==k,{type=k},{Text(l)})} }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({editing=null;showForm=true}){Icon(Icons.Default.Add,null);Spacer(Modifier.width(6.dp));Text("Add")};OutlinedButton({reload()},enabled=!loading){Text(if(loading)"Refreshing…" else "Refresh")}}
        error?.let{Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(vertical=8.dp))}
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(items,key={it.id}){item->Card{Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){Column(Modifier.weight(1f)){Text(item.name,style=MaterialTheme.typography.titleMedium);Text(listOfNotNull(item.city,item.address).joinToString(" • "),style=MaterialTheme.typography.bodySmall);item.imageUrl?.let{Text("Image attached",style=MaterialTheme.typography.labelSmall)};if(item.latitude!=null&&item.longitude!=null)Text("Location: "+String.format(Locale.US,"%.5f",item.latitude)+", "+String.format(Locale.US,"%.5f",item.longitude),style=MaterialTheme.typography.labelSmall)};IconButton({editing=item;showForm=true}){Icon(Icons.Default.Edit,"Edit")};IconButton({scope.launch{api.delete("/admin/content/$type/${item.id}").onSuccess{reload()}.onFailure{error=it.message}}}){Icon(Icons.Default.Delete,"Deactivate")}}}}}
    }
    if(showForm)ContentForm(api,type,editing,{showForm=false;reload()},{error=it})
}
@Composable
private fun ContentForm(
    api: ApiClient,
    type: String,
    existing: ContentItem?,
    onDone: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var address by remember { mutableStateOf(existing?.address.orEmpty()) }
    var city by remember { mutableStateOf(existing?.city.orEmpty()) }
    var lat by remember { mutableStateOf(existing?.latitude?.toString().orEmpty()) }
    var lon by remember { mutableStateOf(existing?.longitude?.toString().orEmpty()) }
    var imageUrl by remember { mutableStateOf(existing?.imageUrl.orEmpty()) }
    var phone by remember { mutableStateOf(existing?.phone.orEmpty()) }
    var email by remember { mutableStateOf(existing?.email.orEmpty()) }
    var website by remember { mutableStateOf(existing?.website.orEmpty()) }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(if (existing == null) "Add " + typeLabel(type) else "Edit " + typeLabel(type)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 520.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = lat,
                            onValueChange = { lat = it },
                            label = { Text("Latitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lon,
                            onValueChange = { lon = it },
                            label = { Text("Longitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val query = if (lat.isNotBlank() && lon.isNotBlank()) lat + "," + lon else address
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query)))
                                )
                            }
                        ) {
                            Icon(Icons.Default.Map, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Open Map")
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Public image URL (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = website,
                        onValueChange = { website = it },
                        label = { Text("Website") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && name.isNotBlank(),
                onClick = {
                    scope.launch {
                        busy = true
                        val body = JSONObject()
                            .put("name", name.trim())
                            .put("description", description)
                            .put("address", address)
                            .put("city", city)
                            .put("imageUrl", imageUrl)
                            .put("phone", phone)
                            .put("email", email)
                            .put("website", website)
                        lat.toDoubleOrNull()?.let { body.put("latitude", it) }
                        lon.toDoubleOrNull()?.let { body.put("longitude", it) }
                        val result = if (existing == null) {
                            api.post("/admin/content/" + type, body)
                        } else {
                            api.patch("/admin/content/" + type + "/" + existing.id, body)
                        }
                        result
                            .onSuccess { onDone() }
                            .onFailure { onError(it.message ?: "Save failed") }
                        busy = false
                    }
                }
            ) {
                Text(if (existing == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDone) { Text("Cancel") }
        }
    )
}

private fun typeLabel(type: String) = when (type) {
    "homestays" -> "Home Stay"
    "sponsors" -> "Sponsor"
    else -> "Business Partner"
}

private fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (has(key) && !isNull(key)) optDouble(key).takeIf { !it.isNaN() } else null
