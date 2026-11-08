package com.mhj.player.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mhj.player.core.model.AppTheme

val LocalMhjColors = staticCompositionLocalOf { MidnightGoldColors }

object MhjTheme {
    val colors: MhjThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMhjColors.current
}

private val MhjTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        letterSpacing = 0.5.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.4.sp,
    ),
)

@Composable
fun MhjPlayerTheme(
    theme: AppTheme = AppTheme.MIDNIGHT_GOLD,
    content: @Composable () -> Unit,
) {
    val colors = when (theme) {
        AppTheme.MIDNIGHT_GOLD -> MidnightGoldColors
        AppTheme.PORCELAIN_WHITE -> PorcelainWhiteColors
    }

    val scheme = if (colors.isDark) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.background,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.surface,
            onSurface = colors.onSurface,
            secondary = colors.primarySoft,
            onSecondary = colors.background,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.elevated,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.surface,
            onSurface = colors.onSurface,
            secondary = colors.primarySoft,
            onSecondary = colors.elevated,
        )
    }

    CompositionLocalProvider(LocalMhjColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MhjTypography,
            content = content,
        )
    }
}
