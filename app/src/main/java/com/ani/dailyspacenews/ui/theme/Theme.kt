package com.ani.dailyspacenews.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// --- Observatory Design System Color Tokens ---
val BgBase = Color(0xFF0B1220)          // Deep space navy screen background (never pure black)
val BgElevated = Color(0xFF121B2E)      // Cards, list rows
val BgElevated2 = Color(0xFF1A2740)     // Sheets, dialogs, top app bar
val BorderHairline = Color(0xFF263752)  // 1px dividers & subtle borders
val TextPrimary = Color(0xFFF2F4F8)     // Off-white primary text
val TextSecondary = Color(0xFF93A1BD)   // Secondary text, captions
val TextTertiary = Color(0xFF5D6B87)    // Disabled states
val AccentAmber = Color(0xFFE8A657)     // Single signature accent (CTAs, active states, countdown digits)
val AccentAmberDim = Color(0xFFB8823F)  // Pressed/secondary emphasis
val SemanticSuccess = Color(0xFF6FA98A) // Desaturated success
val SemanticError = Color(0xFFD97D6C)   // Desaturated coral error

private val ObservatoryColorScheme = darkColorScheme(
    primary = AccentAmber,
    onPrimary = BgBase,
    primaryContainer = BgElevated2,
    onPrimaryContainer = TextPrimary,
    secondary = AccentAmberDim,
    onSecondary = BgBase,
    background = BgBase,
    onBackground = TextPrimary,
    surface = BgElevated,
    onSurface = TextPrimary,
    surfaceVariant = BgElevated2,
    onSurfaceVariant = TextSecondary,
    outline = BorderHairline,
    error = SemanticError,
    onError = BgBase
)

@Composable
fun DailySpaceNewsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ObservatoryColorScheme,
        typography = Typography,
        content = content
    )
}
