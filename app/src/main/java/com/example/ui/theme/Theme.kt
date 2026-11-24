package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldDeep,
    onPrimary = Color.White,
    primaryContainer = EmeraldLight,
    onPrimaryContainer = EmeraldDark,
    secondary = GoldRoyal,
    onSecondary = Color(0xFF1F1604),
    secondaryContainer = GoldContainer,
    onSecondaryContainer = GoldDeep,
    tertiary = EmeraldMedium,
    onTertiary = Color.White,
    background = IvoryBackground,
    onBackground = InkPrimary,
    surface = IvorySurface,
    onSurface = InkPrimary,
    surfaceVariant = IvoryVariant,
    onSurfaceVariant = InkSecondary,
    outline = Color(0xFFCFC7B8),
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = GoldBright,
    onPrimary = EmeraldDark,
    primaryContainer = EmeraldMedium,
    onPrimaryContainer = IvoryBackground,
    secondary = GoldRoyal,
    onSecondary = Color(0xFF1F1604),
    secondaryContainer = Color(0xFF3A2E14),
    onSecondaryContainer = GoldBright,
    tertiary = EmeraldLight,
    onTertiary = EmeraldDark,
    background = Color(0xFF08120E),
    onBackground = DarkTextIvory,
    surface = DarkEmeraldSurface,
    onSurface = DarkTextIvory,
    surfaceVariant = Color(0xFF162721),
    onSurfaceVariant = Color(0xFFB4C3BC),
    outline = Color(0xFF2D453C),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

val NoorShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

enum class ReaderThemeMode(
    val id: String,
    val labelAr: String,
    val bgColor: Color,
    val textColor: Color,
    val accentColor: Color,
    val isDark: Boolean
) {
    IVORY("ivory", "ورق عاجي كلاسيكي", IvoryBackground, InkPrimary, EmeraldDeep, false),
    SEPIA("sepia", "ورق قديم (سيبيا)", SepiaBackground, SepiaText, GoldDeep, false),
    SOFT_GRAY("soft_gray", "رمادي هادئ", SoftGrayBackground, SoftGrayText, EmeraldMedium, false),
    EMERALD_NIGHT("emerald_night", "ليلي زمردي", Color(0xFF0A1914), DarkTextIvory, GoldBright, true),
    OLED_DARK("oled_dark", "ليلي OLED داكن", OledBlackBackground, DarkTextIvory, GoldBright, true);

    companion object {
        fun fromId(id: String): ReaderThemeMode =
            entries.firstOrNull { it.id == id } ?: IVORY
    }
}

@Composable
fun NoorAlQuranTheme(
    darkModeSetting: String = "system",
    content: @Composable () -> Unit
) {
    val isDark = when (darkModeSetting) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NoorTypography,
            shapes = NoorShapes,
            content = content
        )
    }
}
