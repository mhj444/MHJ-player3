package com.mhj.player.core.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object MhjColors {
    val GoldBright = Color(0xFFE8C547)
    val Gold = Color(0xFFD4AF37)
    val GoldDeep = Color(0xFFB8860B)
    val GoldMuted = Color(0xFFA67C52)
    val GoldSoft = Color(0xFFC9A96E)

    val MidnightBg = Color(0xFF000000)
    val MidnightSurface = Color(0xFF0D0D0D)
    val MidnightElevated = Color(0xFF161616)
    val MidnightGlass = Color(0xCC121212)

    val PorcelainBg = Color(0xFFF8F3ED)
    val PorcelainSurface = Color(0xFFF3EDE4)
    val PorcelainElevated = Color(0xFFFFFBF6)
    val PorcelainGlass = Color(0xE6F7F1E8)
    val PorcelainInk = Color(0xFF3A2E22)
    val PorcelainMuted = Color(0xFF7A6A55)

    val GoldGradient = Brush.linearGradient(
        listOf(GoldBright, Gold, GoldDeep),
    )
}

data class MhjThemeColors(
    val background: Color,
    val surface: Color,
    val elevated: Color,
    val glass: Color,
    val primary: Color,
    val primarySoft: Color,
    val onBackground: Color,
    val onSurface: Color,
    val muted: Color,
    val border: Color,
    val controlFill: Color,
    val visualizer: Color,
    val isDark: Boolean,
)

val MidnightGoldColors = MhjThemeColors(
    background = MhjColors.MidnightBg,
    surface = MhjColors.MidnightSurface,
    elevated = MhjColors.MidnightElevated,
    glass = MhjColors.MidnightGlass,
    primary = MhjColors.Gold,
    primarySoft = MhjColors.GoldSoft,
    onBackground = MhjColors.GoldBright,
    onSurface = MhjColors.Gold,
    muted = MhjColors.GoldMuted,
    border = MhjColors.GoldDeep.copy(alpha = 0.55f),
    controlFill = MhjColors.Gold.copy(alpha = 0.18f),
    visualizer = MhjColors.GoldBright,
    isDark = true,
)

val PorcelainWhiteColors = MhjThemeColors(
    background = MhjColors.PorcelainBg,
    surface = MhjColors.PorcelainSurface,
    elevated = MhjColors.PorcelainElevated,
    glass = MhjColors.PorcelainGlass,
    primary = MhjColors.GoldDeep,
    primarySoft = MhjColors.GoldMuted,
    onBackground = MhjColors.PorcelainInk,
    onSurface = MhjColors.GoldDeep,
    muted = MhjColors.PorcelainMuted,
    border = MhjColors.GoldMuted.copy(alpha = 0.45f),
    controlFill = MhjColors.GoldMuted.copy(alpha = 0.12f),
    visualizer = MhjColors.GoldDeep,
    isDark = false,
)
