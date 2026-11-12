package com.mhj.player.core.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object MhjColors {
    // Metallic gold (boards)
    val GoldHi = Color(0xFFEFDFB4)
    val Gold = Color(0xFFC9A96A)
    val GoldMid = Color(0xFFBE9C5C)
    val GoldLo = Color(0xFF8A6D35)
    val GoldGlow = Color(0x66D9BF86)
    val GoldTrack = Color(0xFFD2B57C)

    // Midnight
    val NightBg = Color(0xFF070707)
    val NightSurface = Color(0xFF121212)
    val NightElevated = Color(0xFF1C1C1C)
    val NightRecess = Color(0xFF0A0A0A)
    val NightGlass = Color(0xCC161616)
    val NightText = Color(0xFFF5F5F5)
    val NightMuted = Color(0xFFB8A57A)
    val NightLight = Color(0x33C9A96A)
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
    // Drawn surfaces (page, raised faces, panels, sunken tracks).
    val bgTop: Color = Color(0xFF141416),
    val bgMid: Color = Color(0xFF0B0B0C),
    val bgBottom: Color = Color(0xFF060606),
    val faceTop: Color = Color(0xFF26262A),
    val faceBottom: Color = Color(0xFF161618),
    val sheet: Color = Color(0xFF0E0E10),
    val track: Color = Color(0xFF2C2C31),
    val groove: Color = Color(0xFF0A0A0B),
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

/** Deep emerald green with gold. */
val EmeraldNightColors = MidnightGoldColors.copy(
    background = Color(0xFF06100C),
    surface = Color(0xFF0C1C16),
    elevated = Color(0xFF13281F),
    recess = Color(0xFF05100B),
    glass = Color(0xCC0E1F18),
    muted = Color(0xFFB9A97C),
    bgTop = Color(0xFF0E2219),
    bgMid = Color(0xFF08160F),
    bgBottom = Color(0xFF040B08),
    faceTop = Color(0xFF1A3328),
    faceBottom = Color(0xFF0E2019),
    sheet = Color(0xFF0A1913),
    track = Color(0xFF1C352B),
    groove = Color(0xFF04100A),
)

/** Royal navy blue with champagne gold. */
val RoyalNavyColors = MidnightGoldColors.copy(
    background = Color(0xFF070B18),
    surface = Color(0xFF0E1530),
    elevated = Color(0xFF16203F),
    recess = Color(0xFF060A16),
    glass = Color(0xCC111A36),
    muted = Color(0xFFBCAE88),
    bgTop = Color(0xFF121C3A),
    bgMid = Color(0xFF0A1124),
    bgBottom = Color(0xFF050814),
    faceTop = Color(0xFF1E2A50),
    faceBottom = Color(0xFF121A36),
    sheet = Color(0xFF0C132B),
    track = Color(0xFF223058),
    groove = Color(0xFF050916),
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
    bgTop = Color(0xFFFBF7F1),
    bgMid = Color(0xFFF2EBE1),
    bgBottom = Color(0xFFE9E0D3),
    faceTop = Color(0xFFFFFDF9),
    faceBottom = Color(0xFFF0E8DD),
    sheet = Color(0xFFF4EEE6),
    track = Color(0xFFD9CFC1),
    groove = Color(0xFFE2D8CA),
)
