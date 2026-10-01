package com.hellokurukshetra.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hellokurukshetra.admin.data.ApiClient
import com.hellokurukshetra.admin.feature.adminusers.AdminUsersScreen
import com.hellokurukshetra.admin.feature.audit.AuditScreen
import com.hellokurukshetra.admin.feature.dashboard.DashboardScreen
import com.hellokurukshetra.admin.feature.emergency.EmergencyScreen
import com.hellokurukshetra.admin.feature.notifications.NotificationsScreen
import com.hellokurukshetra.admin.feature.payments.PaymentsScreen
import com.hellokurukshetra.admin.feature.people.PeopleScreen
import com.hellokurukshetra.admin.feature.promotions.PromotionsScreen
import com.hellokurukshetra.admin.feature.rides.RidesScreen
import com.hellokurukshetra.admin.feature.settings.SettingsScreen
import com.hellokurukshetra.admin.feature.support.SupportScreen
import com.hellokurukshetra.admin.feature.verification.VerificationScreen

enum class AdminSection(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    PEOPLE("People", Icons.Default.People),
    VERIFICATION("Provider Verification", Icons.Default.VerifiedUser),
    RIDES("Rides", Icons.Default.DirectionsCar),
    PAYMENTS("Payments", Icons.Default.Payments),
    EMERGENCY("Emergency", Icons.Default.Warning),
    SUPPORT("Support", Icons.Default.SupportAgent),
    PROMOTIONS("Promotions", Icons.Default.LocalOffer),
    NOTIFICATIONS("Notifications", Icons.Default.Notifications),
    ADMIN_USERS("Admin Users", Icons.Default.AdminPanelSettings),
    AUDIT("Audit Logs", Icons.Default.History),
    SETTINGS("Branding & Pricing", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminShell(api: ApiClient, onLogout: () -> Unit) {
    var section by rememberSaveable { mutableStateOf(AdminSection.DASHBOARD) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    LaunchedEffect(drawerOpen) {
        if (drawerOpen) drawerState.open() else drawerState.close()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                Modifier.width(310.dp),
                drawerContainerColor = AdminNavy
            ) {
                Column(Modifier.fillMaxHeight().padding(14.dp)) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
                        Text("HELLO KURUKSHETRA", color = AdminGold, fontWeight = FontWeight.Bold)
                        Text("ADMIN CONTROL CENTER", color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    AdminSection.values().forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.title) },
                            selected = section == item,
                            onClick = { section = item; drawerOpen = false },
                            icon = { Icon(item.icon, contentDescription = null) },
                            modifier = Modifier.padding(vertical = 2.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = AdminGold,
                                selectedTextColor = AdminNavy,
                                selectedIconColor = AdminNavy,
                                unselectedTextColor = Color.White,
                                unselectedIconColor = Color.White.copy(alpha = .8f)
                            )
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    HorizontalDivider(color = Color.White.copy(alpha = .12f))
                    NavigationDrawerItem(
                        label = { Text("Sign out") },
                        selected = false,
                        onClick = onLogout,
                        icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = Color.White,
                            unselectedIconColor = Color.White
                        )
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = AdminSurface,
            topBar = {
                TopAppBar(
                    title = { Text(section.title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { drawerOpen = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open navigation")
                        }
                    },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, contentDescription = "Sign out")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AdminNavy,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize().background(AdminSurface)) {
                when (section) {
                    AdminSection.DASHBOARD -> DashboardScreen(api)
                    AdminSection.PEOPLE -> PeopleScreen(api)
                    AdminSection.VERIFICATION -> VerificationScreen(api)
                    AdminSection.RIDES -> RidesScreen(api)
                    AdminSection.PAYMENTS -> PaymentsScreen(api)
                    AdminSection.EMERGENCY -> EmergencyScreen(api)
                    AdminSection.SUPPORT -> SupportScreen(api)
                    AdminSection.PROMOTIONS -> PromotionsScreen(api)
                    AdminSection.NOTIFICATIONS -> NotificationsScreen(api)
                    AdminSection.ADMIN_USERS -> AdminUsersScreen(api)
                    AdminSection.AUDIT -> AuditScreen(api)
                    AdminSection.SETTINGS -> SettingsScreen(api)
                }
            }
        }
    }
}
