package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DigiQuadDarkColorScheme = darkColorScheme(
    primary = CrimsonNeon,
    onPrimary = Color.White,
    primaryContainer = DeepCrimsonContainer,
    onPrimaryContainer = CrimsonGlow,
    secondary = MatrixGreen,
    onSecondary = ObsidianBg,
    secondaryContainer = DeepMatrixGreenContainer,
    onSecondaryContainer = MatrixGreen,
    tertiary = AmberGold,
    onTertiary = ObsidianBg,
    tertiaryContainer = DeepAmberContainer,
    onTertiaryContainer = AmberGold,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = CyberDeckSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberCardSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = CyberDeckSurface,
    surfaceContainer = CyberCardSurface,
    surfaceContainerHigh = CyberElevatedSurface,
    outline = CyberBorder,
    outlineVariant = Color(0xFF330D14),
    error = CrimsonError,
    onError = Color.White,
)

@Composable
fun DigiQuadTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DigiQuadDarkColorScheme,
        typography = DigiQuadTypography,
        content = content,
    )
}
