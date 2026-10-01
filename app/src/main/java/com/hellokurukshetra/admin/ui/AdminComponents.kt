package com.hellokurukshetra.admin.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
val AdminNavy=Color(0xFF07111F); val AdminGold=Color(0xFFFFC107); val AdminSurface=Color(0xFFF7F8FA)
@Composable fun PageHeader(title:String,subtitle:String,onRefresh:(()->Unit)?=null){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(subtitle,color=Color.Gray)};if(onRefresh!=null)OutlinedButton(onClick=onRefresh){Text("Refresh")}}}
@Composable fun ErrorBanner(message:String)=Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFFFE8E8))){Text(message,Modifier.padding(14.dp),color=Color(0xFF9A1A1A))}
@Composable fun EmptyState(message:String="No records found")=Card(Modifier.fillMaxWidth()){Column(Modifier.fillMaxWidth().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(message,color=Color.Gray)}}
@Composable fun StatusBadge(value:String){val c=when(value.uppercase()){"VERIFIED","APPROVED","COMPLETED","RESOLVED","ACTIVE"->Color(0xFFDDF7E5);"REJECTED","FAILED","CANCELLED","CLOSED"->Color(0xFFFFE3E3);else->Color(0xFFFFF3CD)};Surface(color=c,shape=MaterialTheme.shapes.small){Text(value.replace('_',' '),Modifier.padding(horizontal=10.dp,vertical=6.dp),fontWeight=FontWeight.Bold)}}
fun extract(root:JSONObject,key:String):List<JSONObject>{val data=root.optJSONObject("data");val a=root.optJSONArray(key)?:data?.optJSONArray(key)?:return emptyList();return (0 until a.length()).mapNotNull{a.optJSONObject(it)}}
fun displayName(j:JSONObject)=j.optString("name").ifBlank{j.optString("title").ifBlank{j.optString("code").ifBlank{j.optString("id","Record")}}}
fun summary(j:JSONObject):String{val p=mutableListOf<String>();for(k in j.keys()){val v=j.opt(k);if(v !is JSONObject&&v !is JSONArray)p.add(k+"="+v)};return p.take(7).joinToString(" • ")}
fun dataObject(root:JSONObject)=root.optJSONObject("data")?:root
