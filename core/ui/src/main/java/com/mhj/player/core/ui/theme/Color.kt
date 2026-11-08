package com.mhj.player.core.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object MhjColors {
    // Metallic gold (boards)
    val GoldHi = Color(0xFFF2E0A8)
    val Gold = Color(0xFFD4AF37)
    val GoldMid = Color(0xFFC9A227)
    val GoldLo = Color(0xFF9A7420)
    val GoldGlow = Color(0x66E8C547)
    val GoldTrack = Color(0xFFE0B84A)

    // Midnight
    val NightBg = Color(0xFF070707)
    val NightSurface = Color(0xFF121212)
    val NightElevated = Color(0xFF1C1C1C)
    val NightRecess = Color(0xFF0A0A0A)
    val NightGlass = Color(0xCC161616)
    val NightText = Color(0xFFF5F5F5)
    val NightMuted = Color(0xFFB8A57A)
    val NightLight = Color(0x33D4AF37)
    val NightDark = Color(0x66000000)

    // Porcelain
    val PorcBg = Color(0xFFF4EEE6)
    val PorcSurface = Color(0xFFF7F1E9)
    val PorcElevated = Color(0xFFFFFBF6)
    val PorcRecess = Color(0xFFE6DDD1)
    val PorcGlass = Color(0xE6F8F2EA)
    val PorcInk = Color(0xFF2C241C)
    val PorcMuted = Color(0xFF8A7A66)
    val PorcLight = Color(0xCCFFFFFF)
    val PorcDark = Color(0x338B7355)

    val GoldMetal = Brush.linearGradient(listOf(GoldHi, Gold, GoldLo, GoldMid))
    val GoldMetalV = Brush.verticalGradient(listOf(GoldHi, Gold, GoldLo))
    val GoldRing = Brush.sweepGradient(listOf(GoldHi, GoldLo, Gold, GoldHi))
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
    background = MhjColors.NightBg,
    surface = MhjColors.NightSurface,
    elevated = MhjColors.NightElevated,
    recess = MhjColors.NightRecess,
    glass = MhjColors.NightGlass,
    primary = MhjColors.Gold,
    primarySoft = MhjColors.GoldHi,
    onBackground = MhjColors.NightText,
    onSurface = MhjColors.GoldHi,
    muted = MhjColors.NightMuted,
    border = MhjColors.Gold.copy(alpha = 0.35f),
    controlFill = MhjColors.Gold.copy(alpha = 0.12f),
    visualizer = MhjColors.GoldTrack,
    lightShadow = MhjColors.NightLight,
    darkShadow = MhjColors.NightDark,
    glow = MhjColors.GoldGlow,
    isDark = true,
)

val PorcelainWhiteColors = MhjThemeColors(
    background = MhjColors.PorcBg,
    surface = MhjColors.PorcSurface,
    elevated = MhjColors.PorcElevated,
    recess = MhjColors.PorcRecess,
    glass = MhjColors.PorcGlass,
    primary = MhjColors.GoldMid,
    primarySoft = MhjColors.Gold,
    onBackground = MhjColors.PorcInk,
    onSurface = MhjColors.PorcInk,
    muted = MhjColors.PorcMuted,
    border = MhjColors.GoldLo.copy(alpha = 0.28f),
    controlFill = MhjColors.Gold.copy(alpha = 0.10f),
    visualizer = MhjColors.GoldMid,
    lightShadow = MhjColors.PorcLight,
    darkShadow = MhjColors.PorcDark,
    glow = MhjColors.Gold.copy(alpha = 0.22f),
    isDark = false,
)
