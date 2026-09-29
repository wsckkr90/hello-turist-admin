package com.hellokurukshetra.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hellokurukshetra.admin.data.ApiClient
import com.hellokurukshetra.admin.ui.theme.HelloKurukshetraAdminTheme

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);enableEdgeToEdge();val api=ApiClient(applicationContext);setContent{HelloKurukshetraAdminTheme{AdminApp(api)}}}
}