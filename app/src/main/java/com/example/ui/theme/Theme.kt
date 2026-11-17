package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = ZevoraPrimaryLight,
  onPrimary = Color.White,
  primaryContainer = ZevoraPrimaryDark,
  onPrimaryContainer = Color(0xFFEDE9FE),
  secondary = ZevoraSecondary,
  onSecondary = Color.Black,
  secondaryContainer = Color(0xFF0E7490),
  onSecondaryContainer = Color(0xFFCFFAFE),
  tertiary = ZevoraTertiary,
  background = ZevoraDarkBackground,
  onBackground = Color(0xFFF1F5F9),
  surface = ZevoraDarkSurface,
  onSurface = Color(0xFFF1F5F9),
  surfaceVariant = ZevoraDarkSurfaceVariant,
  onSurfaceVariant = Color(0xFFCBD5E1),
  outline = Color(0xFF475569),
  error = ZevoraError,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = ZevoraPrimary,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFEDE9FE),
  onPrimaryContainer = ZevoraPrimaryDark,
  secondary = Color(0xFF0891B2),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFCFFAFE),
  onSecondaryContainer = Color(0xFF164E63),
  tertiary = ZevoraTertiary,
  background = ZevoraLightBackground,
  onBackground = Color(0xFF0F172A),
  surface = ZevoraLightSurface,
  onSurface = Color(0xFF0F172A),
  surfaceVariant = ZevoraLightSurfaceVariant,
  onSurfaceVariant = Color(0xFF475569),
  outline = Color(0xFFCBD5E1),
  error = ZevoraError,
  onError = Color.White
)

@Composable
fun ZevoraTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

// Backwards compatibility alias
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  ZevoraTheme(darkTheme = darkTheme, content = content)
}
