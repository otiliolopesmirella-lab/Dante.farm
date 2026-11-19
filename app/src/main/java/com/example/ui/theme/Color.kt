package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Pitch Obsidian & Crimson Ember Surfaces
val ObsidianBg = Color(0xFF060203)
val CyberDeckSurface = Color(0xFF0E0507)
val CyberCardSurface = Color(0xFF17080C)
val CyberElevatedSurface = Color(0xFF230B11)
val CyberBorder = Color(0xFF5A1622)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFFCBD5E1)
val TextMuted = Color(0xFF8C6B73)

// Dante Crimson + Active Matrix Green + PRO Gold Accents
val CrimsonNeon = Color(0xFFFF1E2D)
val CrimsonGlow = Color(0xFFFF4655)
val DeepCrimsonContainer = Color(0xFF36070F)

val MatrixGreen = Color(0xFF19FF66)
val DeepMatrixGreenContainer = Color(0xFF052411)

// Semantic aliases kept cohesive across sheets & slots
val ElectricCyan = Color(0xFF00E5FF)
val DeepCyanContainer = Color(0xFF072B36)
val EmeraldNeon = MatrixGreen
val DeepEmeraldContainer = DeepMatrixGreenContainer
val AmberGold = Color(0xFFFFB020)
val DeepAmberContainer = Color(0xFF3A2003)
val CyberMagenta = Color(0xFFFF3D7F)
val DeepMagentaContainer = Color(0xFF3D081B)
val CrimsonError = Color(0xFFFF2A3A)

data class SlotAccentPalette(
    val primary: Color,
    val container: Color,
    val glowBorder: Color,
)

val SlotAccents: List<SlotAccentPalette> = listOf(
    SlotAccentPalette(
        primary = CrimsonNeon,
        container = DeepCrimsonContainer,
        glowBorder = CrimsonNeon.copy(alpha = 0.85f),
    ),
    SlotAccentPalette(
        primary = MatrixGreen,
        container = DeepMatrixGreenContainer,
        glowBorder = MatrixGreen.copy(alpha = 0.78f),
    ),
    SlotAccentPalette(
        primary = AmberGold,
        container = DeepAmberContainer,
        glowBorder = AmberGold.copy(alpha = 0.78f),
    ),
    SlotAccentPalette(
        primary = CyberMagenta,
        container = DeepMagentaContainer,
        glowBorder = CyberMagenta.copy(alpha = 0.78f),
    ),
)
