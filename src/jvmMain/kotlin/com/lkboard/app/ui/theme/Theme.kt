package com.lkboard.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lkboard.app.data.AppTheme

val LocalAppTheme = staticCompositionLocalOf { AppTheme.LIGHT }

/**
 * Современное системное семейство шрифтов (Segoe UI на Windows / SF Pro на macOS),
 * обеспечивающее чёткую, стильную типографику интерфейса.
 */
val ModernFontFamily = FontFamily.SansSerif

val LkboardTypography = Typography(
    displayLarge = TextStyle(fontFamily = ModernFontFamily, fontWeight = FontWeight.Normal, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = ModernFontFamily, fontWeight = FontWeight.Normal, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = ModernFontFamily, fontWeight = FontWeight.Normal, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = ModernFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = ModernFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = ModernFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = ModernFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = ModernFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = ModernFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = ModernFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = ModernFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = ModernFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = ModernFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = ModernFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = ModernFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF0D0F12),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0D0F12),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFF4F4F5),
    surface = Color(0xFF181B20),
    onSurface = Color(0xFFF4F4F5),
    surfaceVariant = Color(0xFF242830),
    outline = Color(0xFF2E333D)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF1E293B),
    surface = Color.White,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFF1F5F9),
    outline = Color(0xFFE2E8F0)
)

private val NotebookColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun LkboardTheme(
    appTheme: AppTheme = AppTheme.LIGHT,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        appTheme == AppTheme.NOTEBOOK -> NotebookColorScheme
        appTheme.isDark -> DarkColorScheme
        else -> LightColorScheme
    }
    CompositionLocalProvider(LocalAppTheme provides appTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LkboardTypography,
            content = content
        )
    }
}
