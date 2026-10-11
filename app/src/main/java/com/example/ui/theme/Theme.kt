package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LaserCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004E57),
    onPrimaryContainer = Color(0xFF80F5FF),
    secondary = ElectricIndigo,
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Color(0xFFC7D2FE),
    tertiary = SafeEmerald,
    onTertiary = Color(0xFF064E3B),
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = ObsidianBase,
    onBackground = TextPrimary,
    surface = SurfaceElevated,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevatedHigh,
    onSurfaceVariant = TextMuted,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF4F46E5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEF2FF),
    onSecondaryContainer = Color(0xFF3730A3),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFECFDF5),
    onTertiaryContainer = Color(0xFF047857),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFE2E8F0)
)

@Composable
fun SwiftScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent branding by default
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // Viewfinder apps excel with high-contrast dark theme by default
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
