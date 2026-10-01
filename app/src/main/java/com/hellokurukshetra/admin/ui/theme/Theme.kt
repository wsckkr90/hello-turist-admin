package com.hellokurukshetra.admin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AdminLightScheme = lightColorScheme(
    primary = AdminPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = AdminGoldTheme,
    onPrimaryContainer = AdminPrimaryDark,
    secondary = AdminGoldTheme,
    onSecondary = AdminPrimaryDark,
    background = AdminBackground,
    onBackground = AdminText,
    surface = AdminSurfaceTheme,
    onSurface = AdminText,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE9EEF5),
    onSurfaceVariant = AdminMuted,
    error = AdminDangerTheme
)

private val AdminDarkScheme = darkColorScheme(
    primary = AdminGoldTheme,
    onPrimary = AdminPrimaryDark,
    secondary = AdminGoldTheme,
    onSecondary = AdminPrimaryDark,
    background = AdminPrimaryDark,
    onBackground = androidx.compose.ui.graphics.Color.White,
    surface = AdminPrimary,
    onSurface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1B3047),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB9C5D3),
    error = Color(0xFFFF8A80)
)

@Composable
fun HelloKurukshetraAdminTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) AdminDarkScheme else AdminLightScheme,
        typography = Typography,
        content = content
    )
}
