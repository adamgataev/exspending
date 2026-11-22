package com.example.exspending.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ExspendingDarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = AppBackground,
    primaryContainer = Color(0xFF1A3A52),
    onPrimaryContainer = TextPrimary,
    secondary = TextSecondary,
    onSecondary = AppBackground,
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = TextPrimary,
    tertiary = TransferAmber,
    onTertiary = AppBackground,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = AppBackground,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = OutlineMuted,
    outlineVariant = Color(0xFF333333),
    error = ExpenseRed,
    onError = AppBackground,
    errorContainer = Color(0xFF3D2929),
    onErrorContainer = ExpenseRed,
    inverseSurface = TextPrimary,
    inverseOnSurface = AppBackground,
    inversePrimary = Color(0xFF0B5A8C),
    surfaceTint = AccentBlue,
    scrim = Color.Black
)

@Composable
fun ExspendingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ExspendingDarkColorScheme,
        typography = Typography,
        shapes = ExspendingShapes,
        content = content
    )
}
