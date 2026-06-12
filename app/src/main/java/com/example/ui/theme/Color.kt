package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalThemeOption = staticCompositionLocalOf { ThemeOption.COSMIC_AMBER }

// Cosmic Slate Color Palette
val CosmicBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalThemeOption.current.backgroundColor

val CosmicSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalThemeOption.current.surfaceColor

val CosmicSurfaceHeader: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalThemeOption.current.surfaceHeaderColor

val CosmicPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalThemeOption.current.primaryColor

val CosmicSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalThemeOption.current.secondaryColor

val CosmicTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalThemeOption.current.isLight) Color(0xFF7C3AED) else Color(0xFFA855F7)   // Dynamic Purple Accent

val CosmicWhite: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalThemeOption.current.whiteColor

val CosmicGrayText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalThemeOption.current.grayTextColor

val CosmicCardBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalThemeOption.current.cardBorderColor

val AccentGreen = Color(0xFF10B981)      // Success green (compatible)
val AccentRed = Color(0xFFEF4444)        // Error red (incompatible)
val AccentOrange = Color(0xFFF97316)     // Warning orange (TDP overload)


