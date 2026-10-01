package com.hellokurukshetra.admin.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hellokurukshetra.admin.data.ApiClient
import com.hellokurukshetra.admin.ui.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun SettingsScreen(api: ApiClient) {
    var appName by remember { mutableStateOf("") }
    var logo by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("") }
    var base by remember { mutableStateOf("") }
    var perKm by remember { mutableStateOf("") }
    var minimum by remember { mutableStateOf("") }
    var bannerTitle by remember { mutableStateOf("") }
    var bannerSubtitle by remember { mutableStateOf("") }
    var bannerImage by remember { mutableStateOf("") }
    var ctaLabel by remember { mutableStateOf("") }
    var ctaAction by remember { mutableStateOf("") }
    var bannerActive by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        api.get("/admin/branding").onSuccess {
            val d = dataObject(it)
            appName = d.optString("appName")
            logo = d.optString("logoUrl")
            icon = d.optString("iconUrl")
        }.onFailure { error = it.message }

        api.get("/admin/pricing/ride").onSuccess {
            val d = dataObject(it)
            base = d.optString("baseFare")
            perKm = d.optString("perKm")
            minimum = d.optString("minimumFare")
        }.onFailure { error = it.message }

        api.get("/admin/home-banner").onSuccess {
            val d = dataObject(it)
            bannerTitle = d.optString("title")
            bannerSubtitle = d.optString("subtitle")
            bannerImage = d.optString("imageUrl")
            ctaLabel = d.optString("ctaLabel")
            ctaAction = d.optString("ctaAction")
            bannerActive = d.optBoolean("isActive", true)
        }.onFailure { error = it.message }
        loading = false
    }

    fun showSuccess(message: String) {
        success = message
        error = null
    }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader("Branding & pricing", "Manage app identity, home banner and ride fare rules.", { refreshKey++ }, loading) }
        success?.let { item { SuccessBanner(it) } }
        error?.let { item { ErrorBanner(it) { refreshKey++ } } }

        item {
            SectionCard("App branding") {
                OutlinedTextField(appName, { appName = it }, label = { Text("App name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(logo, { logo = it }, label = { Text("Logo HTTPS URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(icon, { icon = it }, label = { Text("Icon HTTPS URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(enabled = !saving, onClick = {
                    scope.launch {
                        saving = true
                        api.patch("/admin/branding", JSONObject()
                            .put("appName", appName.trim())
                            .put("logoUrl", logo.trim().ifBlank { JSONObject.NULL })
                            .put("iconUrl", icon.trim().ifBlank { JSONObject.NULL }))
                            .onSuccess { showSuccess("Branding saved.") }
                            .onFailure { error = it.message ?: "Unable to save branding" }
                        saving = false
                    }
                }) { Text("Save branding") }
            }
        }

        item {
            SectionCard("Home banner") {
                OutlinedTextField(bannerTitle, { bannerTitle = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(bannerSubtitle, { bannerSubtitle = it }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(bannerImage, { bannerImage = it }, label = { Text("Image HTTPS URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(ctaLabel, { ctaLabel = it }, label = { Text("CTA label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(ctaAction, { ctaAction = it }, label = { Text("CTA action") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Switch(checked = bannerActive, onCheckedChange = { bannerActive = it })
                    Text(if (bannerActive) "Banner active" else "Banner hidden")
                }
                Button(enabled = !saving, onClick = {
                    scope.launch {
                        saving = true
                        api.patch("/admin/home-banner", JSONObject()
                            .put("title", bannerTitle.trim())
                            .put("subtitle", bannerSubtitle.trim())
                            .put("imageUrl", bannerImage.trim().ifBlank { JSONObject.NULL })
                            .put("ctaLabel", ctaLabel.trim())
                            .put("ctaAction", ctaAction.trim())
                            .put("isActive", bannerActive))
                            .onSuccess { showSuccess("Home banner saved.") }
                            .onFailure { error = it.message ?: "Unable to save home banner" }
                        saving = false
                    }
                }) { Text("Save banner") }
            }
        }

        item {
            SectionCard("Ride pricing (INR)") {
                OutlinedTextField(base, { base = it }, label = { Text("Base fare") }, singleLine = true)
                OutlinedTextField(perKm, { perKm = it }, label = { Text("Per KM") }, singleLine = true)
                OutlinedTextField(minimum, { minimum = it }, label = { Text("Minimum fare") }, singleLine = true)
                Button(enabled = !saving, onClick = {
                    val baseValue = base.toDoubleOrNull()
                    val kmValue = perKm.toDoubleOrNull()
                    val minimumValue = minimum.toDoubleOrNull()
                    if (baseValue == null || kmValue == null || minimumValue == null || baseValue < 0 || kmValue < 0 || minimumValue < 0) {
                        error = "Enter valid non-negative pricing values."
                        return@Button
                    }
                    scope.launch {
                        saving = true
                        api.patch("/admin/pricing/ride", JSONObject()
                            .put("baseFare", baseValue)
                            .put("perKm", kmValue)
                            .put("minimumFare", minimumValue))
                            .onSuccess { showSuccess("Ride pricing saved.") }
                            .onFailure { error = it.message ?: "Unable to save pricing" }
                        saving = false
                    }
                }) { Text("Save pricing") }
            }
        }
    }
}
