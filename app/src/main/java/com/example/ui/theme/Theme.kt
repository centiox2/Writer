package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode {
  SYSTEM,
  DARK,
  LIGHT
}

private val DarkColorScheme = darkColorScheme(
  primary = StudioBlue,
  onPrimary = StudioBlack,
  primaryContainer = StudioBlueSubtle,
  onPrimaryContainer = StudioTextHighDark,
  secondary = StudioMint,
  onSecondary = StudioBlack,
  secondaryContainer = StudioMintSubtle,
  onSecondaryContainer = StudioTextHighDark,
  tertiary = StudioAmber,
  background = StudioBlack,
  onBackground = StudioTextHighDark,
  surface = StudioSurfaceDark,
  onSurface = StudioTextHighDark,
  surfaceVariant = StudioSurfaceVariantDark,
  onSurfaceVariant = StudioTextMediumDark,
  outline = StudioBorderDark,
  error = StudioRed,
  errorContainer = StudioRedSubtle,
  onError = StudioTextHighDark
)

private val LightColorScheme = lightColorScheme(
  primary = StudioBlueDarker,
  onPrimary = StudioLightSurface,
  primaryContainer = StudioLightSurfaceVariant,
  onPrimaryContainer = StudioLightTextHigh,
  secondary = StudioMint,
  onSecondary = StudioLightSurface,
  secondaryContainer = StudioLightSurfaceVariant,
  onSecondaryContainer = StudioLightTextHigh,
  tertiary = StudioAmber,
  background = StudioLightBackground,
  onBackground = StudioLightTextHigh,
  surface = StudioLightSurface,
  onSurface = StudioLightTextHigh,
  surfaceVariant = StudioLightSurfaceVariant,
  onSurfaceVariant = StudioLightTextMedium,
  outline = StudioLightBorder,
  error = StudioRed,
  onError = StudioLightSurface
)

@Composable
fun SongwriterTheme(
  themeMode: ThemeMode = ThemeMode.DARK,
  dynamicColor: Boolean = false, // Keep precision studio palette by default
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val darkTheme = when (themeMode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> systemDark
  }

  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

// Retain alias for test compatibility
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  SongwriterTheme(
    themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
    dynamicColor = dynamicColor,
    content = content
  )
}
