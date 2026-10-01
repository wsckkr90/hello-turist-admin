package com.hellokurukshetra.admin.feature.dashboard
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hellokurukshetra.admin.data.ApiClient
import com.hellokurukshetra.admin.ui.*
import org.json.JSONObject
@Composable fun DashboardScreen(api:ApiClient){var data by remember{mutableStateOf<JSONObject?>(null)};var error by remember{mutableStateOf<String?>(null)};LaunchedEffect(Unit){api.get("/admin/analytics").onSuccess{data=it}.onFailure{error=it.message}};val k=data?.optJSONObject("kpis");LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){item{PageHeader("Operations overview","A simple snapshot of platform activity.")};error?.let{item{ErrorBanner(it)}};item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Metric("Users",k?.optInt("users")?:0,Modifier.weight(1f));Metric("Active users",k?.optInt("activeUsers")?:0,Modifier.weight(1f))}};item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Metric("Drivers",k?.optInt("approvedDrivers")?:0,Modifier.weight(1f));Metric("Guides",k?.optInt("approvedGuides")?:0,Modifier.weight(1f))}};item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Metric("Rides",k?.optInt("rides")?:0,Modifier.weight(1f));Metric("Payments",k?.optInt("payments")?:0,Modifier.weight(1f))}};item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Metric("Verification",k?.optInt("verificationRequests")?:0,Modifier.weight(1f));Metric("Support",k?.optInt("supportTickets")?:0,Modifier.weight(1f))}};item{val b=data?.optJSONObject("breakdowns");if(b!=null){Breakdown("Ride status",b.optJSONObject("rides"));Breakdown("Payment status",b.optJSONObject("payments"));Breakdown("Verification status",b.optJSONObject("verificationRequests"))}}}}
@Composable private fun Metric(label:String,value:Int,modifier:Modifier)=Card(modifier){Column(Modifier.padding(18.dp)){Text(label,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value.toString(),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)}}
@Composable private fun Breakdown(title:String,json:JSONObject?){if(json==null)return;Card(Modifier.fillMaxWidth().padding(top=8.dp)){Column(Modifier.padding(16.dp)){Text(title,fontWeight=FontWeight.Bold);for(k in json.keys())Text(k+": "+json.opt(k),Modifier.padding(top=5.dp))}}}
