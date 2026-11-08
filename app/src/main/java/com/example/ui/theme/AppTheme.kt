package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized AppTheme object providing clean access to MaterialTheme colorScheme,
 * typography, shapes, and standardized spacing constants across the entire application.
 */
object AppTheme {
    val colors: ColorScheme
        @Composable
        get() = MaterialTheme.colorScheme

    val typography: Typography
        @Composable
        get() = MaterialTheme.typography

    val shapes
        @Composable
        get() = MaterialTheme.shapes

    // Standardized Spacing Constants
    val spacingNone: Dp = 0.dp
    val spacingXXS: Dp = 2.dp
    val spacingXS: Dp = 4.dp
    val spacingSmall: Dp = 8.dp
    val spacingMedium: Dp = 12.dp
    val spacingNormal: Dp = 16.dp
    val spacingLarge: Dp = 20.dp
    val spacingXL: Dp = 24.dp
    val spacingXXL: Dp = 32.dp
    val spacingGiant: Dp = 48.dp
}
