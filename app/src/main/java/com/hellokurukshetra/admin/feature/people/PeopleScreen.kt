package com.hellokurukshetra.admin.feature.people
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
@Composable fun PeopleScreen(api:ApiClient){var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var error by remember{mutableStateOf<String?>(null)};var selected by remember{mutableStateOf<JSONObject?>(null)};fun load(){LaunchedEffect(Unit){}};LaunchedEffect(Unit){api.get("/admin/people?limit=100").onSuccess{rows=extract(it,"items")}.onFailure{error=it.message}};LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{PageHeader("People","View riders, drivers, guides and account information.")};error?.let{item{ErrorBanner(it)}};if(rows.isEmpty()&&error==null)item{EmptyState()};items(rows,key={it.optString("id")}){r->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(displayName(r),style=MaterialTheme.typography.titleMedium);StatusBadge(r.optString("status","ACTIVE"))};Text(summary(r),color=MaterialTheme.colorScheme.onSurfaceVariant);TextButton(onClick={selected=r}){Text("View details")}}}}};selected?.let{r->AlertDialog(onDismissRequest={selected=null},title={Text(displayName(r))},text={Text(summary(r)+"\n\nAccount ID: "+r.optString("id"))},confirmButton={TextButton({selected=null}){Text("Close")}})}}}
