package com.mhj.player.core.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object MhjColors {
    val GoldBright = Color(0xFFF0D78C)
    val Gold = Color(0xFFD4AF37)
    val GoldDeep = Color(0xFFB8860B)
    val GoldMuted = Color(0xFFA67C52)
    val GoldSoft = Color(0xFFC9A96E)
    val GoldGlow = Color(0x66E8C547)
    val GoldOrange = Color(0xFFE8A03C)

    val MidnightBg = Color(0xFF050505)
    val MidnightSurface = Color(0xFF101010)
    val MidnightElevated = Color(0xFF1A1A1A)
    val MidnightRecess = Color(0xFF080808)
    val MidnightGlass = Color(0xD9141414)
    val MidnightHighlight = Color(0x33FFFFFF)
    val MidnightShadow = Color(0xCC000000)

    val PorcelainBg = Color(0xFFF5EFE6)
    val PorcelainSurface = Color(0xFFF8F3ED)
    val PorcelainElevated = Color(0xFFFFFBF6)
    val PorcelainRecess = Color(0xFFE8DFD2)
    val PorcelainGlass = Color(0xE6F7F1E8)
    val PorcelainInk = Color(0xFF2F261C)
    val PorcelainMuted = Color(0xFF7A6A55)
    val PorcelainHighlight = Color(0xFFFFFFFF)
    val PorcelainShadow = Color(0x33A0896A)

    val MetallicGold = Brush.linearGradient(
        listOf(GoldBright, Gold, GoldDeep, Gold),
    )
    val MetallicGoldVertical = Brush.verticalGradient(
        listOf(GoldBright, Gold, GoldDeep),
    )
}

data class MhjThemeColors(
    val background: Color,
    val surface: Color,
    val elevated: Color,
    val recess: Color,
    val glass: Color,
    val primary: Color,
    val primarySoft: Color,
    val onBackground: Color,
    val onSurface: Color,
    val muted: Color,
    val border: Color,
    val controlFill: Color,
    val visualizer: Color,
    val lightShadow: Color,
    val darkShadow: Color,
    val glow: Color,
    val isDark: Boolean,
)

val MidnightGoldColors = MhjThemeColors(
    background = MhjColors.MidnightBg,
    surface = MhjColors.MidnightSurface,
    elevated = MhjColors.MidnightElevated,
    recess = MhjColors.MidnightRecess,
    glass = MhjColors.MidnightGlass,
    primary = MhjColors.Gold,
    primarySoft = MhjColors.GoldSoft,
    onBackground = MhjColors.GoldBright,
    onSurface = MhjColors.Gold,
    muted = MhjColors.GoldMuted,
    border = MhjColors.Gold.copy(alpha = 0.45f),
    controlFill = MhjColors.Gold.copy(alpha = 0.14f),
    visualizer = MhjColors.GoldBright,
    lightShadow = MhjColors.MidnightHighlight,
    darkShadow = MhjColors.MidnightShadow,
    glow = MhjColors.GoldGlow,
    isDark = true,
)

val PorcelainWhiteColors = MhjThemeColors(
    background = MhjColors.PorcelainBg,
    surface = MhjColors.PorcelainSurface,
    elevated = MhjColors.PorcelainElevated,
    recess = MhjColors.PorcelainRecess,
    glass = MhjColors.PorcelainGlass,
    primary = MhjColors.GoldDeep,
    primarySoft = MhjColors.GoldMuted,
    onBackground = MhjColors.PorcelainInk,
    onSurface = MhjColors.GoldDeep,
    muted = MhjColors.PorcelainMuted,
    border = MhjColors.GoldMuted.copy(alpha = 0.35f),
    controlFill = MhjColors.GoldMuted.copy(alpha = 0.10f),
    visualizer = MhjColors.GoldDeep,
    lightShadow = MhjColors.PorcelainHighlight,
    darkShadow = MhjColors.PorcelainShadow,
    glow = MhjColors.Gold.copy(alpha = 0.28f),
    isDark = false,
)
