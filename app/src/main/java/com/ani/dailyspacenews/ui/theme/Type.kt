package com.ani.dailyspacenews.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.ani.dailyspacenews.R

val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val SpaceGroteskFont = GoogleFont("Space Grotesk")
val IBMPlexSansFont = GoogleFont("IBM Plex Sans")
val JetBrainsMonoFont = GoogleFont("JetBrains Mono")

val SpaceGroteskFamily = FontFamily(
    Font(googleFont = SpaceGroteskFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = SpaceGroteskFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = SpaceGroteskFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

val IBMPlexSansFamily = FontFamily(
    Font(googleFont = IBMPlexSansFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = IBMPlexSansFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = IBMPlexSansFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

val JetBrainsMonoFamily = FontFamily(
    Font(googleFont = JetBrainsMonoFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = JetBrainsMonoFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = JetBrainsMonoFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

// Monospace Telemetry style token
val TelemetryMonoStyle = TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontWeight = FontWeight.Bold,
    color = AccentAmber,
    letterSpacing = 1.sp
)

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimary
    ),
    headlineSmall = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = TextPrimary
    ),
    titleSmall = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = IBMPlexSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = IBMPlexSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.15.sp,
        color = TextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = IBMPlexSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextTertiary
    ),
    labelLarge = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.5.sp,
        color = TextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
        color = TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp,
        color = TextTertiary
    )
)