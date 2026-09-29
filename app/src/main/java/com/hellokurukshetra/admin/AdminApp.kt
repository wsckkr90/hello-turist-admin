package com.hellokurukshetra.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hellokurukshetra.admin.data.ApiClient
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Navy=Color(0xFF07111F);private val Yellow=Color(0xFFFFC107)
private enum class Section(val title:String,val icon:androidx.compose.ui.graphics.vector.ImageVector){DASHBOARD("Dashboard",Icons.Default.Dashboard),PEOPLE("People",Icons.Default.People),VERIFICATION("Verification",Icons.Default.VerifiedUser),RIDES("Rides",Icons.Default.DirectionsCar),PAYMENTS("Payments",Icons.Default.Payments),EMERGENCY("Emergency",Icons.Default.Warning),SUPPORT("Support",Icons.Default.SupportAgent),PROMOTIONS("Promotions",Icons.Default.LocalOffer),NOTIFICATIONS("Notifications",Icons.Default.Notifications),ADMIN_USERS("Admin Users",Icons.Default.AdminPanelSettings),AUDIT("Audit Logs",Icons.Default.History),SETTINGS("Branding & Pricing",Icons.Default.Settings)}

@Composable fun AdminApp(api:ApiClient){var logged by remember{mutableStateOf(api.isLoggedIn())};if(logged)Shell(api){api.logout();logged=false}else Login(api){logged=true}}

@Composable private fun Login(api:ApiClient,onLogin:()->Unit){val scope=rememberCoroutineScope();var id by remember{mutableStateOf("")};var pass by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)};Box(Modifier.fillMaxSize().background(Navy),contentAlignment=Alignment.Center){Card(Modifier.padding(24.dp).fillMaxWidth().widthIn(max=460.dp)){Column(Modifier.padding(28.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Text("HELLO KURUKSHETRA",color=Navy,fontWeight=FontWeight.Bold);Text("Admin Control Center",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Secure administrator sign-in",color=Color.Gray);OutlinedTextField(id,{id=it},label={Text("Username or email")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(pass,{pass=it},label={Text("Password")},singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth());error?.let{Text(it,color=MaterialTheme.colorScheme.error)};Button(onClick={if(id.isBlank()||pass.isBlank()){error="Enter username and password";return@Button};busy=true;scope.launch{api.login(id,pass).onSuccess{root->val d=root.optJSONObject("data")?:JSONObject();api.saveSession(d);api.get("/admin/analytics").onSuccess{onLogin()}.onFailure{api.logout();error="Account does not have active admin access."}}.onFailure{error=it.message};busy=false}},enabled=!busy,colors=ButtonDefaults.buttonColors(containerColor=Yellow,contentColor=Navy),modifier=Modifier.fillMaxWidth().height(52.dp)){if(busy)CircularProgressIndicator(strokeWidth=2.dp,modifier=Modifier.size(22.dp))else Text("SIGN IN",fontWeight=FontWeight.Bold)}}}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun Shell(api:ApiClient,logout:()->Unit){var section by remember{mutableStateOf(Section.DASHBOARD)};var open by remember{mutableStateOf(false)};val drawerState=rememberDrawerState(DrawerValue.Closed);LaunchedEffect(open){if(open)drawerState.open() else drawerState.close()};ModalNavigationDrawer(drawerState=drawerState,drawerContent={ModalDrawerSheet{Column(Modifier.background(Navy).fillMaxHeight().width(300.dp).padding(16.dp)){Text("Hello Kurukshetra",color=Yellow,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("ADMIN PANEL",color=Color.White);Spacer(Modifier.height(20.dp));Section.values().forEach{item->NavigationDrawerItem(label={Text(item.title)},selected=section==item,onClick={section=item;open=false},icon={Icon(item.icon,null)},colors=NavigationDrawerItemDefaults.colors(selectedContainerColor=Yellow,selectedTextColor=Navy,selectedIconColor=Navy,unselectedTextColor=Color.White,unselectedIconColor=Color.White))};Spacer(Modifier.weight(1f));NavigationDrawerItem(label={Text("Logout")},selected=false,onClick=logout,icon={Icon(Icons.Default.Logout,null)})}}}){Scaffold(topBar={TopAppBar(title={Text(section.title,fontWeight=FontWeight.Bold)},navigationIcon={IconButton({open=true}){Icon(Icons.Default.Menu,"Menu")}},actions={IconButton(logout){Icon(Icons.Default.Logout,"Logout")}},colors=TopAppBarDefaults.topAppBarColors(containerColor=Navy,titleContentColor=Color.White,navigationIconContentColor=Color.White,actionIconContentColor=Color.White))}){p->Box(Modifier.padding(p).fillMaxSize()){when(section){Section.DASHBOARD->Dashboard(api);Section.PEOPLE->PeopleAdmin(api);Section.VERIFICATION->DriverVerification(api);Section.RIDES->RidesAdmin(api);Section.PAYMENTS->ActionList(api,"Payments","/admin/finance/payments?limit=100","items","Refund" to {id->api.post("/admin/finance/payments/"+id+"/refund",JSONObject().put("reason","Admin refund"))});Section.EMERGENCY->EmergencyAdmin(api);Section.SUPPORT->SupportAdmin(api);Section.PROMOTIONS->PromotionsAdmin(api);Section.NOTIFICATIONS->NotificationsAdmin(api);Section.ADMIN_USERS->AdminUsersAdmin(api);Section.AUDIT->ListScreen(api,"Audit Logs","/admin/audit-logs","items");Section.SETTINGS->Settings(api)}}}}}

@Composable private fun Dashboard(api:ApiClient){var data by remember{mutableStateOf<JSONObject?>(null)};var err by remember{mutableStateOf<String?>(null)};LaunchedEffect(Unit){api.get("/admin/analytics").onSuccess{data=it}.onFailure{err=it.message}};val k=data?.optJSONObject("kpis");LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text("Operations overview",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)};err?.let{item{Error(it)}};item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Kpi("Users",k?.optInt("users",0)?:0,Modifier.weight(1f));Kpi("Active",k?.optInt("activeUsers",0)?:0,Modifier.weight(1f))}};item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Kpi("Drivers",k?.optInt("approvedDrivers",0)?:0,Modifier.weight(1f));Kpi("Guides",k?.optInt("approvedGuides",0)?:0,Modifier.weight(1f))}};item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Kpi("Rides",k?.optInt("rides",0)?:0,Modifier.weight(1f));Kpi("Payments",k?.optInt("payments",0)?:0,Modifier.weight(1f))}};item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Kpi("Verification",k?.optInt("verificationRequests",0)?:0,Modifier.weight(1f));Kpi("Support",k?.optInt("supportTickets",0)?:0,Modifier.weight(1f))}};item{Breakdown("Ride status",data?.optJSONObject("breakdowns")?.optJSONObject("rides"))};item{Breakdown("Payment status",data?.optJSONObject("breakdowns")?.optJSONObject("payments"))};item{Breakdown("Verification status",data?.optJSONObject("breakdowns")?.optJSONObject("verificationRequests"))}}}

@Composable private fun ListScreen(api:ApiClient,title:String,endpoint:String,key:String){var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var err by remember{mutableStateOf<String?>(null)};LaunchedEffect(endpoint){api.get(endpoint).onSuccess{rows=extract(it,key)}.onFailure{err=it.message}};LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){err?.let{item{Error(it)}};items(rows){JsonCard(title,it)};if(rows.isEmpty()&&err==null)item{Empty()}}}

@Composable
private fun DriverVerification(api: ApiClient) {
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var rejectId by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            val endpoints = listOf(
                "/admin/verification/requests?role=DRIVER&status=UNDER_VERIFICATION&limit=100",
                "/admin/verification/requests?role=DRIVER&status=RESUBMITTED&limit=100",
                "/admin/verification/requests?role=DRIVER&status=PENDING&limit=100"
            )
            var loaded = false
            for (endpoint in endpoints) {
                if (loaded) break
                api.get(endpoint).onSuccess {
                    val found = extractVerificationItems(it)
                    if (found.isNotEmpty()) {
                        rows = found
                        error = null
                        loaded = true
                    }
                }.onFailure { if (error == null) error = it.message }
            }
            loading = false
        }
    }

    fun decide(id: String, status: String, reason: String? = null) {
        scope.launch {
            busy = true
            val body = JSONObject().put("status", status)
            if (status == "REJECTED") body.put("rejectionReason", reason ?: "Rejected by admin")
            api.patch("/admin/verification/requests/" + id, body)
                .onSuccess { selected = null; rejectId = null; load() }
                .onFailure { error = it.message }
            busy = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Driver Verification", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Review driver documents and live verification.", color = Color.Gray)
                    }
                    OutlinedButton(onClick = { load() }, enabled = !loading) {
                        Text(if (loading) "Loading..." else "Refresh")
                    }
                }
            }
            error?.let { item { Error(it) } }
            if (!loading && rows.isEmpty()) {
                item { Empty() }
            }
            items(rows, key = { it.optString("id") }) { row ->
                val user = row.optJSONObject("user")
                val name = user?.optString("name").orEmpty().ifBlank { "Driver" }
                val username = user?.optString("username").orEmpty()
                val status = row.optString("status", "UNKNOWN")
                val documents = row.optJSONArray("documents")
                val live = row.optJSONObject("liveSession")
                val liveStatus = live?.optString("status").orEmpty().ifBlank { "NOT_STARTED" }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                if (username.isNotBlank()) Text("@" + username, color = Color.Gray)
                            }
                            StatusChip(status)
                        }
                        Text("Documents: " + (documents?.length() ?: 0))
                        Text("Live verification: " + liveStatus)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    api.get("/admin/verification/requests/" + row.optString("id"))
                                        .onSuccess { selected = it.optJSONObject("data") ?: it }
                                        .onFailure { error = it.message }
                                }
                            }) { Text("Details") }
                            OutlinedButton(
                                onClick = { rejectId = row.optString("id") },
                                enabled = !busy
                            ) { Text("Reject") }
                            Button(
                                onClick = { decide(row.optString("id"), "VERIFIED") },
                                enabled = !busy && (documents?.length() ?: 0) > 0 && liveStatus == "IN_PROGRESS",
                                colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy)
                            ) { Text("Approve") }
                        }
                    }
                }
            }
        }
        selected?.let { detail ->
            VerificationDetailsDialog(
                request = detail,
                busy = busy,
                onDismiss = { selected = null },
                onReject = { rejectId = detail.optString("id") },
                onApprove = { decide(detail.optString("id"), "VERIFIED") }
            )
        }
        rejectId?.let { id ->
            RejectDialog(
                busy = busy,
                onDismiss = { rejectId = null },
                onConfirm = { reason -> decide(id, "REJECTED", reason) }
            )
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    Surface(
        color = when (status) {
            "VERIFIED" -> Color(0xFFDDF7E5)
            "REJECTED" -> Color(0xFFFFE3E3)
            else -> Color(0xFFFFF3CD)
        },
        shape = MaterialTheme.shapes.small
    ) {
        Text(status.replace('_', ' '), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VerificationDetailsDialog(
    request: JSONObject,
    busy: Boolean,
    onDismiss: () -> Unit,
    onReject: () -> Unit,
    onApprove: () -> Unit
) {
    val user = request.optJSONObject("user")
    val docs = request.optJSONArray("documents")
    val steps = request.optJSONArray("steps")
    val live = request.optJSONObject("liveSession")
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(user?.optString("name").orEmpty().ifBlank { "Driver Verification" }) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { Text("Status: " + request.optString("status", "UNKNOWN")) }
                item { Text("Username: " + user?.optString("username").orEmpty().ifBlank { "—" }) }
                item { Text("Email: " + user?.optString("email").orEmpty().ifBlank { "—" }) }
                item { Text("Live verification: " + live?.optString("status").orEmpty().ifBlank { "NOT_STARTED" }) }
                item { Text("Documents") }
                if (docs != null) for (i in 0 until docs.length()) {
                    val d = docs.optJSONObject(i)
                    item { Text("• " + d?.optString("documentType", "Document") + " — " + d?.optString("verificationStatus", "PENDING")) }
                }
                item { Text("Verification steps") }
                if (steps != null) for (i in 0 until steps.length()) {
                    val s = steps.optJSONObject(i)
                    item { Text("• " + s?.optString("step", "STEP") + " — " + s?.optString("status", "PENDING")) }
                }
                request.optString("rejectionReason").takeIf { it.isNotBlank() }?.let {
                    item { Text("Previous rejection: " + it, color = Color(0xFF9A0000)) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApprove,
                enabled = !busy && (docs?.length() ?: 0) > 0 && live?.optString("status") == "IN_PROGRESS",
                colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy)
            ) { Text(if (busy) "Processing..." else "Approve") }
        },
        dismissButton = {
            Row {
                OutlinedButton(onClick = onReject, enabled = !busy) { Text("Reject") }
                TextButton(onClick = onDismiss, enabled = !busy) { Text("Close") }
            }
        }
    )
}

@Composable
private fun RejectDialog(
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Reject Driver Verification") },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Rejection reason") },
                minLines = 3,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason.trim()) },
                enabled = reason.trim().isNotBlank() && !busy
            ) { Text(if (busy) "Processing..." else "Reject") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
        }
    )
}


@Composable private fun PeopleAdmin(api:ApiClient){
    val scope=rememberCoroutineScope(); var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())}; var err by remember{mutableStateOf<String?>(null)}; var selected by remember{mutableStateOf<JSONObject?>(null)}
    fun load(){scope.launch{api.get("/admin/people?limit=100").onSuccess{rows=extract(it,"items");err=null}.onFailure{err=it.message}}}; LaunchedEffect(Unit){load()}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("People",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);OutlinedButton({load()}){Text("Refresh")}}}
        err?.let{item{Error(it)}};items(rows,key={it.optString("id")}){row->
            val id=row.optString("id");Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text(pretty(row),fontWeight=FontWeight.Bold);Text(compact(row),color=Color.DarkGray)
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedButton({scope.launch{api.get("/admin/people/"+id).onSuccess{selected=it.optJSONObject("data")?:it}.onFailure{err=it.message}}}){Text("Details")};TextButton({scope.launch{api.patch("/admin/users/"+id+"/roles/DRIVER",JSONObject().put("verificationStatus","APPROVED")).onSuccess{load()}.onFailure{err=it.message}}}){Text("Approve Driver")};TextButton({scope.launch{api.patch("/admin/users/"+id+"/roles/DRIVER",JSONObject().put("verificationStatus","SUSPENDED")).onSuccess{load()}.onFailure{err=it.message}}}){Text("Suspend Driver")}}
            }}
        }
    }
    selected?.let{j->AlertDialog(onDismissRequest={selected=null},title={Text(pretty(j))},text={Text(compact(j)+"\n\n"+j.toString())},confirmButton={TextButton({selected=null}){Text("Close")}})}
}

@Composable private fun RidesAdmin(api:ApiClient){
    val scope=rememberCoroutineScope();var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var err by remember{mutableStateOf<String?>(null)};var selectedAction by remember{mutableStateOf<Pair<String,String>?>(null)};var input by remember{mutableStateOf("")}
    fun load(){scope.launch{api.get("/admin/rides?limit=100").onSuccess{rows=extract(it,"items");err=null}.onFailure{err=it.message}}};LaunchedEffect(Unit){load()}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Operational Rides",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);OutlinedButton({load()}){Text("Refresh")}}};err?.let{item{Error(it)}};items(rows,key={it.optString("id")}){r->
        val id=r.optString("id");Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(pretty(r),fontWeight=FontWeight.Bold);Text(compact(r),maxLines=5,color=Color.DarkGray);Row{OutlinedButton({selectedAction=id to "assign"}){Text("Assign")};OutlinedButton({selectedAction=id to "cancel"}){Text("Cancel")};OutlinedButton({selectedAction=id to "interrupt"}){Text("Interrupt")};OutlinedButton({selectedAction=id to "recover"}){Text("Recover")}}}}
    }}
    selectedAction?.let{(id,action)->AlertDialog(onDismissRequest={selectedAction=null},title={Text(action.replaceFirstChar{it.uppercase()}+" Ride")},text={OutlinedTextField(input,{input=it},label={Text(if(action=="assign")"Driver ID" else "Reason")},modifier=Modifier.fillMaxWidth())},confirmButton={Button({scope.launch{val body=if(action=="assign")JSONObject().put("driverId",input.trim()) else JSONObject().put("reason",input.trim().ifBlank{"Admin intervention"});val result=when(action){"assign"->api.post("/admin/rides/"+id+"/assign",body);"cancel"->api.post("/admin/rides/"+id+"/cancel",body);"interrupt"->api.post("/admin/rides/"+id+"/interrupt",body);else->api.post("/admin/rides/"+id+"/recover",body)};result.onSuccess{selectedAction=null;input="";load()}.onFailure{err=it.message}}}){Text("Confirm")}},dismissButton={TextButton({selectedAction=null}){Text("Close")}})}
}

@Composable private fun EmergencyAdmin(api:ApiClient){
    val scope=rememberCoroutineScope();var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var err by remember{mutableStateOf<String?>(null)};var dialog by remember{mutableStateOf<Pair<String,String>?>(null)};var input by remember{mutableStateOf("")}
    fun load(){scope.launch{api.get("/admin/emergency/incidents?limit=100").onSuccess{rows=extract(it,"items");if(rows.isEmpty())rows=extract(it,"incidents");err=null}.onFailure{err=it.message}}};LaunchedEffect(Unit){load()}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Emergency Incidents",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);OutlinedButton({load()}){Text("Refresh")}}};err?.let{item{Error(it)}};items(rows,key={it.optString("id")}){r->val id=r.optString("id");Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(pretty(r),fontWeight=FontWeight.Bold);Text(compact(r),maxLines=5,color=Color.DarkGray);Row{OutlinedButton({scope.launch{api.post("/admin/emergency/incidents/"+id+"/acknowledge").onSuccess{load()}.onFailure{err=it.message}}}){Text("Acknowledge")};OutlinedButton({scope.launch{api.post("/admin/emergency/incidents/"+id+"/escalate").onSuccess{load()}.onFailure{err=it.message}}}){Text("Escalate")};OutlinedButton({scope.launch{api.post("/admin/emergency/incidents/"+id+"/resolve").onSuccess{load()}.onFailure{err=it.message}}}){Text("Resolve")};OutlinedButton({dialog=id to "responder"}){Text("Responder")}}}}}}
    dialog?.let{(id,kind)->AlertDialog(onDismissRequest={dialog=null},title={Text("Assign Responder")},text={OutlinedTextField(input,{input=it},label={Text("Responder ID")})},confirmButton={Button({scope.launch{api.post("/admin/emergency/incidents/"+id+"/responders",JSONObject().put("responderId",input.trim())).onSuccess{dialog=null;input="";load()}.onFailure{err=it.message}}}){Text("Assign")}},dismissButton={TextButton({dialog=null}){Text("Close")}})}
}

@Composable private fun SupportAdmin(api:ApiClient){
    val scope=rememberCoroutineScope();var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var err by remember{mutableStateOf<String?>(null)};var dialog by remember{mutableStateOf<Pair<String,String>?>(null)};var input by remember{mutableStateOf("")}
    fun load(){scope.launch{api.get("/admin/support/tickets?limit=100").onSuccess{rows=extract(it,"items");err=null}.onFailure{err=it.message}}};LaunchedEffect(Unit){load()}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Support Tickets",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)};err?.let{item{Error(it)}};items(rows,key={it.optString("id")}){r->val id=r.optString("id");Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(pretty(r),fontWeight=FontWeight.Bold);Text(compact(r),maxLines=5);Row{OutlinedButton({dialog=id to "reply"}){Text("Reply")};OutlinedButton({dialog=id to "status"}){Text("Status")}}}}}}
    dialog?.let{(id,kind)->AlertDialog(onDismissRequest={dialog=null},title={Text(if(kind=="reply")"Reply to Ticket" else "Update Ticket Status")},text={OutlinedTextField(input,{input=it},label={Text(if(kind=="reply")"Message" else "OPEN / IN_PROGRESS / CLOSED")},minLines=if(kind=="reply")3 else 1)},confirmButton={Button({scope.launch{val result=if(kind=="reply")api.post("/admin/support/tickets/"+id+"/reply",JSONObject().put("message",input.trim())) else api.patch("/admin/support/tickets/"+id+"/status",JSONObject().put("status",input.trim().uppercase()));result.onSuccess{dialog=null;input="";load()}.onFailure{err=it.message}}}){Text("Save")}},dismissButton={TextButton({dialog=null}){Text("Close")}})}
}

@Composable private fun PromotionsAdmin(api:ApiClient){
    val scope=rememberCoroutineScope();var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var err by remember{mutableStateOf<String?>(null)};var create by remember{mutableStateOf(false)};var code by remember{mutableStateOf("")};var title by remember{mutableStateOf("")};var value by remember{mutableStateOf("10")};var starts by remember{mutableStateOf("")};var expires by remember{mutableStateOf("")}
    fun load(){scope.launch{api.get("/admin/promotions?limit=100").onSuccess{rows=extract(it,"items");err=null}.onFailure{err=it.message}}};LaunchedEffect(Unit){load()}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Promotions",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Button({create=true}){Text("Create")}}};err?.let{item{Error(it)}};items(rows,key={it.optString("id")}){r->val id=r.optString("id");Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(pretty(r),fontWeight=FontWeight.Bold);Text(compact(r),maxLines=5);Row{OutlinedButton({scope.launch{api.post("/admin/promotions/"+id+"/deactivate").onSuccess{load()}.onFailure{err=it.message}}}){Text("Deactivate")};OutlinedButton({create=true;code=r.optString("code");title=r.optString("title");value=r.optString("discountValue","10")}){Text("Edit")}}}}}}
    if(create)AlertDialog(onDismissRequest={create=false},title={Text("Promotion")},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(code,{code=it},label={Text("Code")});OutlinedTextField(title,{title=it},label={Text("Title")});OutlinedTextField(value,{value=it},label={Text("Discount value")});OutlinedTextField(starts,{starts=it},label={Text("Starts ISO date")});OutlinedTextField(expires,{expires=it},label={Text("Expires ISO date")})}},confirmButton={Button({scope.launch{val body=JSONObject().put("code",code.trim().uppercase()).put("title",title.trim()).put("discountType","PERCENTAGE").put("discountValue",value.toDoubleOrNull()?:0).put("startsAt",starts.trim()).put("expiresAt",expires.trim()).put("isActive",true);val result=if(rows.any{it.optString("code").equals(code.trim(),true)})api.patch("/admin/promotions/"+rows.first{it.optString("code").equals(code.trim(),true)}.optString("id"),body) else api.post("/admin/promotions",body);result.onSuccess{create=false;load()}.onFailure{err=it.message}}}){Text("Save")}},dismissButton={TextButton({create=false}){Text("Close")}})}

@Composable private fun NotificationsAdmin(api:ApiClient){
    val scope=rememberCoroutineScope();var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var err by remember{mutableStateOf<String?>(null)};var send by remember{mutableStateOf(false)};var userId by remember{mutableStateOf("")};var title by remember{mutableStateOf("")};var body by remember{mutableStateOf("")}
    fun load(){scope.launch{api.get("/admin/notifications?limit=100").onSuccess{rows=extract(it,"notifications");err=null}.onFailure{err=it.message}}};LaunchedEffect(Unit){load()}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Notifications",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Button({send=true}){Text("Send Notification")}}};err?.let{item{Error(it)}};items(rows){JsonCard("Notification",it)}}
    if(send)AlertDialog(onDismissRequest={send=false},title={Text("Send Notification")},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(userId,{userId=it},label={Text("User ID")});OutlinedTextField(title,{title=it},label={Text("Title")});OutlinedTextField(body,{body=it},label={Text("Message")},minLines=3)}},confirmButton={Button({scope.launch{api.post("/admin/notifications",JSONObject().put("userId",userId.trim()).put("title",title.trim()).put("body",body.trim())).onSuccess{send=false;load()}.onFailure{err=it.message}}}){Text("Send")}},dismissButton={TextButton({send=false}){Text("Close")}})

@Composable private fun AdminUsersAdmin(api:ApiClient){
    val scope=rememberCoroutineScope();var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var err by remember{mutableStateOf<String?>(null)};var target by remember{mutableStateOf<String?>(null)};var permissions by remember{mutableStateOf("")}
    fun load(){scope.launch{api.get("/admin/users?limit=100").onSuccess{rows=extract(it,"users")}.onFailure{err=it.message}}};LaunchedEffect(Unit){load()}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Admin Users",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)};err?.let{item{Error(it)}};items(rows,key={it.optString("id")}){u->val id=u.optString("id");Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(pretty(u),fontWeight=FontWeight.Bold);Text(compact(u),maxLines=5);OutlinedButton({scope.launch{api.get("/admin/rbac/users/"+id+"/permissions").onSuccess{val d=it.optJSONObject("data")?:it;permissions=d.optJSONArray("permissions")?.let{a->(0 until a.length()).joinToString(","){i->a.optString(i)}}?:"";target=id}.onFailure{err=it.message}}}){Text("Edit Permissions")}}}}}
    target?.let{id->AlertDialog(onDismissRequest={target=null},title={Text("Admin Permissions")},text={OutlinedTextField(permissions,{permissions=it},label={Text("Permissions, comma separated")},minLines=4,modifier=Modifier.fillMaxWidth())},confirmButton={Button({scope.launch{api.put("/admin/rbac/users/"+id+"/permissions",JSONObject().put("permissions",permissions.split(",").map{it.trim()}.filter{it.isNotBlank()})).onSuccess{target=null;load()}.onFailure{err=it.message}}}){Text("Save")}},dismissButton={TextButton({target=null}){Text("Close")}})}
}

@Composable private fun ActionList(api:ApiClient,title:String,endpoint:String,key:String,vararg actionPairs:Pair<String,suspend(String)->Result<JSONObject>>){var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var err by remember{mutableStateOf<String?>(null)};val scope=rememberCoroutineScope();fun load(){scope.launch{api.get(endpoint).onSuccess{rows=extract(it,key)}.onFailure{err=it.message}}};LaunchedEffect(endpoint){load()};LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){err?.let{item{Error(it)}};items(rows){row->val id=row.optString("id");Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(pretty(row),fontWeight=FontWeight.Bold);Text(compact(row),maxLines=5,color=Color.DarkGray);if(id.isNotBlank())Row{actionPairs.forEach{a->OutlinedButton(onClick={scope.launch{a.second(id).onFailure{err=it.message}.onSuccess{load()}}},modifier=Modifier.padding(end=6.dp)){Text(a.first)}}}}}}}}

@Composable private fun Settings(api:ApiClient){
    val scope=rememberCoroutineScope();var branding by remember{mutableStateOf<JSONObject?>(null)};var pricing by remember{mutableStateOf<JSONObject?>(null)};var banner by remember{mutableStateOf<JSONObject?>(null)};var err by remember{mutableStateOf<String?>(null)}
    var appName by remember{mutableStateOf("")};var logoUrl by remember{mutableStateOf("")};var iconUrl by remember{mutableStateOf("")};var base by remember{mutableStateOf("")};var perKm by remember{mutableStateOf("")};var minimum by remember{mutableStateOf("")};var bannerTitle by remember{mutableStateOf("")};var bannerSubtitle by remember{mutableStateOf("")};var bannerImage by remember{mutableStateOf("")};var bannerCta by remember{mutableStateOf("")};var bannerAction by remember{mutableStateOf("")}
    fun load(){scope.launch{api.get("/admin/branding").onSuccess{val d=it.optJSONObject("data")?:it;branding=d;appName=d.optString("appName");logoUrl=d.optString("logoUrl");iconUrl=d.optString("iconUrl")}.onFailure{err=it.message};api.get("/admin/pricing/ride").onSuccess{val d=it.optJSONObject("data")?:it;pricing=d;base=d.optString("baseFare");perKm=d.optString("perKm");minimum=d.optString("minimumFare")}.onFailure{err=it.message};api.get("/admin/home-banner").onSuccess{val d=it.optJSONObject("data")?:it;banner=d;bannerTitle=d.optString("title");bannerSubtitle=d.optString("subtitle");bannerImage=d.optString("imageUrl");bannerCta=d.optString("ctaLabel");bannerAction=d.optString("ctaAction")}.onFailure{err=it.message}}};LaunchedEffect(Unit){load()}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("Branding, Home Banner & Pricing",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)};err?.let{item{Error(it)}}
        item{Card{Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text("App Branding",fontWeight=FontWeight.Bold);OutlinedTextField(appName,{appName=it},label={Text("App name")},modifier=Modifier.fillMaxWidth());OutlinedTextField(logoUrl,{logoUrl=it},label={Text("Logo HTTPS URL")},modifier=Modifier.fillMaxWidth());OutlinedTextField(iconUrl,{iconUrl=it},label={Text("Icon HTTPS URL")},modifier=Modifier.fillMaxWidth());Button({scope.launch{api.patch("/admin/branding",JSONObject().put("appName",appName.trim()).put("logoUrl",logoUrl.trim().ifBlank{JSONObject.NULL}).put("iconUrl",iconUrl.trim().ifBlank{JSONObject.NULL})).onSuccess{load()}.onFailure{err=it.message}}}){Text("Save Branding")}}}}
        item{Card{Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text("Home Banner",fontWeight=FontWeight.Bold);OutlinedTextField(bannerTitle,{bannerTitle=it},label={Text("Title")},modifier=Modifier.fillMaxWidth());OutlinedTextField(bannerSubtitle,{bannerSubtitle=it},label={Text("Subtitle")},modifier=Modifier.fillMaxWidth());OutlinedTextField(bannerImage,{bannerImage=it},label={Text("Image HTTPS URL")},modifier=Modifier.fillMaxWidth());OutlinedTextField(bannerCta,{bannerCta=it},label={Text("CTA label")},modifier=Modifier.fillMaxWidth());OutlinedTextField(bannerAction,{bannerAction=it},label={Text("CTA action")},modifier=Modifier.fillMaxWidth());Button({scope.launch{api.patch("/admin/home-banner",JSONObject().put("title",bannerTitle.trim()).put("subtitle",bannerSubtitle).put("imageUrl",bannerImage.trim().ifBlank{JSONObject.NULL}).put("ctaLabel",bannerCta.trim()).put("ctaAction",bannerAction.trim()).put("isActive",true)).onSuccess{load()}.onFailure{err=it.message}}}){Text("Save Banner")}}}}
        item{Card{Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text("Ride Pricing (INR)",fontWeight=FontWeight.Bold);OutlinedTextField(base,{base=it},label={Text("Base fare")});OutlinedTextField(perKm,{perKm=it},label={Text("Per KM")});OutlinedTextField(minimum,{minimum=it},label={Text("Minimum fare")});Button({scope.launch{api.patch("/admin/pricing/ride",JSONObject().put("baseFare",base.toDoubleOrNull()?:0).put("perKm",perKm.toDoubleOrNull()?:0).put("minimumFare",minimum.toDoubleOrNull()?:0)).onSuccess{load()}.onFailure{err=it.message}}}){Text("Save Pricing")}}}}
    }
}

@Composable private fun Kpi(label:String,value:Int,m:Modifier){Card(m){Column(Modifier.padding(16.dp)){Text(label,color=Color.Gray);Text(value.toString(),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,color=Navy)}}}
@Composable private fun Breakdown(title:String,json:JSONObject?){if(json==null)return;Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(title,fontWeight=FontWeight.Bold);for(k in json.keys())Text(k+": "+json.opt(k),Modifier.padding(top=5.dp))}}}
@Composable private fun JsonCard(title:String,j:JSONObject){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(title,fontWeight=FontWeight.Bold);Text(compact(j),color=Color.DarkGray)}}}
@Composable private fun Error(s:String){Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFFFEAEA))){Text(s,Modifier.padding(14.dp),color=Color(0xFF9A0000))}}
@Composable private fun Empty(){Card(Modifier.fillMaxWidth()){Text("No records found",Modifier.padding(24.dp),color=Color.Gray)}}
private fun extractVerificationItems(root:JSONObject):List<JSONObject>{
    val candidates = listOf("items","requests","verificationRequests")
    for (key in candidates) {
        val found = extract(root,key)
        if (found.isNotEmpty()) return found
    }
    return emptyList()
}
private fun extract(root:JSONObject,key:String):List<JSONObject>{val out=mutableListOf<JSONObject>();val data=root.optJSONObject("data");val a=root.optJSONArray(key)?:data?.optJSONArray(key);if(a!=null)for(i in 0 until a.length())a.optJSONObject(i)?.let{out.add(it)};return out}
private fun pretty(j:JSONObject)=j.optString("name").ifBlank{j.optString("title").ifBlank{j.optString("code").ifBlank{j.optString("id","Record")}}}
private fun compact(j:JSONObject):String{val p=mutableListOf<String>();for(k in j.keys()){val v=j.opt(k);if(v !is JSONObject&&v !is JSONArray)p.add(k+"="+v)};return p.take(8).joinToString(" • ")}
