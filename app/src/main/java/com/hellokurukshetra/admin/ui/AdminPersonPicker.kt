package com.hellokurukshetra.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hellokurukshetra.admin.data.ApiClient
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPersonPicker(
    api: ApiClient,
    role: String? = null,
    label: String,
    selectedId: String,
    onSelected: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var people by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadPeople(query: String = search) {
        scope.launch {
            loading = true
            error = null
            val params = buildString {
                append("/admin/people?limit=100")
                if (!role.isNullOrBlank()) append("&role=").append(role)
                if (query.isNotBlank()) append("&search=").append(URLEncoder.encode(query.trim(), "UTF-8"))
            }
            api.get(params)
                .onSuccess { people = extract(it, "items") }
                .onFailure { error = it.message ?: "Unable to load people" }
            loading = false
        }
    }

    val selected = people.firstOrNull { it.optString("id") == selectedId }
    val selectedText = selected?.let { personLabel(it) } ?: selectedId.takeIf { it.isNotBlank() }?.let { "Selected ID: $it" }.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (!enabled) return@ExposedDropdownMenuBox
            expanded = !expanded
            if (!expanded || people.isNotEmpty()) return@ExposedDropdownMenuBox
            loadPeople()
        },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text("Select from registered users") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 420.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = {
                    search = it
                    loadPeople(it)
                },
                singleLine = true,
                label = { Text("Search name, username or email") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )

            if (loading) {
                DropdownMenuItem(
                    text = { Text("Loading…") },
                    onClick = {},
                    enabled = false
                )
            } else if (people.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(error ?: "No matching users") },
                    onClick = {},
                    enabled = false
                )
            } else {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(people, key = { it.optString("id") }) { person ->
                        val id = person.optString("id")
                        DropdownMenuItem(
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(personLabel(person))
                                    Text(
                                        personSecondaryLabel(person),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onSelected(id)
                                expanded = false
                                search = ""
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun personLabel(person: JSONObject): String =
    person.optString("name").ifBlank {
        person.optString("username").ifBlank { "Unnamed user" }
    }

private fun personSecondaryLabel(person: JSONObject): String {
    val username = person.optString("username")
    val email = person.optString("email")
    val id = person.optString("id")
    return listOf(username.takeIf { it.isNotBlank() }, email.takeIf { it.isNotBlank() }, "ID: $id")
        .joinToString(" • ")
}
