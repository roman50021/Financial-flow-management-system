package com.example.financialflow.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAEAEA),
    onPrimaryContainer = Color(0xFF111111),
    secondary = Color(0xFF5E5E64),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDEDF0),
    onSecondaryContainer = Color(0xFF1B1B1E),
    tertiary = Color(0xFF6557E8),
    tertiaryContainer = Color(0xFFE6E1FF),
    background = Color(0xFFF7F7F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDEDF0),
    onSurface = Color(0xFF151517),
    onSurfaceVariant = Color(0xFF67676D),
    outline = Color(0xFF8A8A91),
    outlineVariant = Color(0xFFD9D9DE),
    error = Color(0xFFD92D4F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF4F4F5),
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF29292D),
    onPrimaryContainer = Color(0xFFF4F4F5),
    secondary = Color(0xFFC6C6CC),
    onSecondary = Color(0xFF1E1E21),
    secondaryContainer = Color(0xFF303034),
    onSecondaryContainer = Color(0xFFE7E7EA),
    tertiary = Color(0xFFBDB4FF),
    tertiaryContainer = Color(0xFF40358F),
    background = Color(0xFF070708),
    surface = Color(0xFF121214),
    surfaceVariant = Color(0xFF232326),
    onSurface = Color(0xFFF3F3F5),
    onSurfaceVariant = Color(0xFFA7A7AE),
    outline = Color(0xFF77777F),
    outlineVariant = Color(0xFF343438),
    error = Color(0xFFFF6B83),
)

@Composable
fun FinancialFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
