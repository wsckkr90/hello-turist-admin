package com.hellokurukshetra.admin.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hellokurukshetra.admin.data.ApiClient
import com.hellokurukshetra.admin.feature.dashboard.DashboardScreen
import com.hellokurukshetra.admin.feature.people.PeopleScreen
import com.hellokurukshetra.admin.feature.verification.VerificationScreen
import com.hellokurukshetra.admin.feature.rides.RidesScreen
import com.hellokurukshetra.admin.feature.payments.PaymentsScreen
import com.hellokurukshetra.admin.feature.emergency.EmergencyScreen
import com.hellokurukshetra.admin.feature.support.SupportScreen
import com.hellokurukshetra.admin.feature.promotions.PromotionsScreen
import com.hellokurukshetra.admin.feature.notifications.NotificationsScreen
import com.hellokurukshetra.admin.feature.adminusers.AdminUsersScreen
import com.hellokurukshetra.admin.feature.audit.AuditScreen
import com.hellokurukshetra.admin.feature.settings.SettingsScreen
enum class AdminSection(val title:String,val icon:androidx.compose.ui.graphics.vector.ImageVector){DASHBOARD("Dashboard",Icons.Default.Dashboard),PEOPLE("People",Icons.Default.People),VERIFICATION("Provider Verification",Icons.Default.VerifiedUser),RIDES("Rides",Icons.Default.DirectionsCar),PAYMENTS("Payments",Icons.Default.Payments),EMERGENCY("Emergency",Icons.Default.Warning),SUPPORT("Support",Icons.Default.SupportAgent),PROMOTIONS("Promotions",Icons.Default.LocalOffer),NOTIFICATIONS("Notifications",Icons.Default.Notifications),ADMIN_USERS("Admin Users",Icons.Default.AdminPanelSettings),AUDIT("Audit Logs",Icons.Default.History),SETTINGS("Branding & Pricing",Icons.Default.Settings)}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AdminShell(api:ApiClient,onLogout:()->Unit){var section by remember{mutableStateOf(AdminSection.DASHBOARD)};val drawer=rememberDrawerState(DrawerValue.Closed);var open by remember{mutableStateOf(false)};LaunchedEffect(open){if(open)drawer.open()else drawer.close()};ModalNavigationDrawer(drawerState=drawer,drawerContent={ModalDrawerSheet(Modifier.width(300.dp)){Column(Modifier.fillMaxHeight().background(AdminNavy).padding(16.dp)){Text("Hello Kurukshetra",color=AdminGold,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("ADMIN CONTROL CENTER",color=Color.White,style=MaterialTheme.typography.labelMedium);Spacer(Modifier.height(20.dp));AdminSection.values().forEach{item->NavigationDrawerItem(label={Text(item.title)},selected=section==item,onClick={section=item;open=false},icon={Icon(item.icon,null)},colors=NavigationDrawerItemDefaults.colors(selectedContainerColor=AdminGold,selectedTextColor=AdminNavy,selectedIconColor=AdminNavy,unselectedTextColor=Color.White,unselectedIconColor=Color.White))};Spacer(Modifier.weight(1f));NavigationDrawerItem(label={Text("Sign out")},selected=false,onClick=onLogout,icon={Icon(Icons.Default.Logout,null)})}}}){Scaffold(topBar={TopAppBar(title={Text(section.title,fontWeight=FontWeight.Bold)},navigationIcon={IconButton({open=true}){Icon(Icons.Default.Menu,"Menu")}},actions={IconButton(onLogout){Icon(Icons.Default.Logout,"Sign out")}},colors=TopAppBarDefaults.topAppBarColors(containerColor=AdminNavy,titleContentColor=Color.White,navigationIconContentColor=Color.White,actionIconContentColor=Color.White))}){p->Box(Modifier.padding(p).fillMaxSize().background(AdminSurface)){when(section){AdminSection.DASHBOARD->DashboardScreen(api);AdminSection.PEOPLE->PeopleScreen(api);AdminSection.VERIFICATION->VerificationScreen(api);AdminSection.RIDES->RidesScreen(api);AdminSection.PAYMENTS->PaymentsScreen(api);AdminSection.EMERGENCY->EmergencyScreen(api);AdminSection.SUPPORT->SupportScreen(api);AdminSection.PROMOTIONS->PromotionsScreen(api);AdminSection.NOTIFICATIONS->NotificationsScreen(api);AdminSection.ADMIN_USERS->AdminUsersScreen(api);AdminSection.AUDIT->AuditScreen(api);AdminSection.SETTINGS->SettingsScreen(api)}}}}}
