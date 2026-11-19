package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val ObsidianBg = Color(0xFF090D16)
val CyberDeckSurface = Color(0xFF111827)
val CyberCardSurface = Color(0xFF172033)
val CyberElevatedSurface = Color(0xFF1E293B)
val CyberBorder = Color(0xFF283548)

val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Primary & Secondary System Accents
val ElectricCyan = Color(0xFF00E5FF)
val DeepCyanContainer = Color(0xFF083344)
val EmeraldNeon = Color(0xFF10B981)
val DeepEmeraldContainer = Color(0xFF064E3B)
val AmberGold = Color(0xFFF59E0B)
val DeepAmberContainer = Color(0xFF451A03)
val CyberMagenta = Color(0xFFEC4899)
val DeepMagentaContainer = Color(0xFF500724)
val CrimsonError = Color(0xFFEF4444)

data class SlotAccentPalette(
    val primary: Color,
    val container: Color,
    val glowBorder: Color,
)

val SlotAccents: List<SlotAccentPalette> = listOf(
    SlotAccentPalette(
        primary = ElectricCyan,
        container = DeepCyanContainer,
        glowBorder = ElectricCyan.copy(alpha = 0.65f),
    ),
    SlotAccentPalette(
        primary = EmeraldNeon,
        container = DeepEmeraldContainer,
        glowBorder = EmeraldNeon.copy(alpha = 0.65f),
    ),
    SlotAccentPalette(
        primary = AmberGold,
        container = DeepAmberContainer,
        glowBorder = AmberGold.copy(alpha = 0.65f),
    ),
    SlotAccentPalette(
        primary = CyberMagenta,
        container = DeepMagentaContainer,
        glowBorder = CyberMagenta.copy(alpha = 0.65f),
    ),
)
