package com.hellokurukshetra.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.hellokurukshetra.admin.data.ApiClient
import com.hellokurukshetra.admin.ui.theme.HelloKurukshetraAdminTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HelloKurukshetraAdminTheme {
                var api by remember { mutableStateOf<ApiClient?>(null) }

                LaunchedEffect(Unit) {
                    api = withContext(Dispatchers.IO) {
                        ApiClient(applicationContext)
                    }
                }

                val client = api
                if (client == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    AdminApp(client)
                }
            }
        }
    }
}
