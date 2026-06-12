package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
  themeOption: ThemeOption = ThemeOption.COSMIC_AMBER,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (themeOption.isLight) {
    lightColorScheme(
      primary = themeOption.primaryColor,
      secondary = themeOption.secondaryColor,
      tertiary = themeOption.primaryColor,
      background = themeOption.backgroundColor,
      surface = themeOption.surfaceColor,
      onPrimary = Color.White,
      onSecondary = Color.White,
      onBackground = themeOption.whiteColor,
      onSurface = themeOption.whiteColor,
      outline = themeOption.cardBorderColor
    )
  } else {
    darkColorScheme(
      primary = themeOption.primaryColor,
      secondary = themeOption.secondaryColor,
      tertiary = themeOption.primaryColor,
      background = themeOption.backgroundColor,
      surface = themeOption.surfaceColor,
      onPrimary = themeOption.backgroundColor,
      onSecondary = themeOption.backgroundColor,
      onBackground = themeOption.whiteColor,
      onSurface = themeOption.whiteColor,
      outline = themeOption.cardBorderColor
    )
  }

  CompositionLocalProvider(LocalThemeOption provides themeOption) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}

