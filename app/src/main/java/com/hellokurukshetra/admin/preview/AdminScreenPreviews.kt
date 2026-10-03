package com.hellokurukshetra.admin.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.hellokurukshetra.admin.data.ApiClient
import com.hellokurukshetra.admin.feature.adminusers.AdminUsersScreen
import com.hellokurukshetra.admin.feature.audit.AuditScreen
import com.hellokurukshetra.admin.feature.content.ContentScreen
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
import com.hellokurukshetra.admin.ui.theme.HelloKurukshetraAdminTheme

@Composable
private fun PreviewApi(content: @Composable (ApiClient) -> Unit) {
    HelloKurukshetraAdminTheme { content(ApiClient(LocalContext.current)) }
}

@Preview(name = "Admin dashboard", showBackground = true, showSystemUi = true)
@Composable private fun DashboardPreview() = PreviewApi { DashboardScreen(it) }

@Preview(name = "Emergency", showBackground = true)
@Composable private fun EmergencyPreview() = PreviewApi { EmergencyScreen(it) }

@Preview(name = "Notifications", showBackground = true)
@Composable private fun NotificationsPreview() = PreviewApi { NotificationsScreen(it) }

@Preview(name = "Payments", showBackground = true)
@Composable private fun PaymentsPreview() = PreviewApi { PaymentsScreen(it) }

@Preview(name = "People", showBackground = true)
@Composable private fun PeoplePreview() = PreviewApi { PeopleScreen(it) }

@Preview(name = "Promotions", showBackground = true)
@Composable private fun PromotionsPreview() = PreviewApi { PromotionsScreen(it) }

@Preview(name = "Rides", showBackground = true)
@Composable private fun RidesPreview() = PreviewApi { RidesScreen(it) }

@Preview(name = "Settings", showBackground = true)
@Composable private fun SettingsPreview() = PreviewApi { SettingsScreen(it) }

@Preview(name = "Support", showBackground = true)
@Composable private fun SupportPreview() = PreviewApi { SupportScreen(it) }

@Preview(name = "Verification", showBackground = true)
@Composable private fun VerificationPreview() = PreviewApi { VerificationScreen(it) }

@Preview(name = "Admin users", showBackground = true)
@Composable private fun AdminUsersPreview() = PreviewApi { AdminUsersScreen(it) }

@Preview(name = "Audit", showBackground = true)
@Composable private fun AuditPreview() = PreviewApi { AuditScreen(it) }

@Preview(name = "Content", showBackground = true)
@Composable private fun ContentPreview() = PreviewApi { ContentScreen(it) }
