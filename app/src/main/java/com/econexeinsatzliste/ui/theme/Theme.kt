package com.econexeinsatzliste.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    onPrimary = Color.White,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = SecondarySlateDark,
    onSecondary = Color.White,
    tertiary = TertiaryGreenDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = SecondarySlateLight,
    onSecondary = Color.White,
    tertiary = TertiaryGreenLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xD1D1FAEE),
    onTertiaryContainer = Color(0xFF065F46),
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun EconexEinsatzlisteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val targetColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val animatedPrimary = animateColorAsState(targetColorScheme.primary, animationSpec = tween(400), label = "primary").value
    val animatedOnPrimary = animateColorAsState(targetColorScheme.onPrimary, animationSpec = tween(400), label = "onPrimary").value
    val animatedPrimaryContainer = animateColorAsState(targetColorScheme.primaryContainer, animationSpec = tween(400), label = "primaryContainer").value
    val animatedOnPrimaryContainer = animateColorAsState(targetColorScheme.onPrimaryContainer, animationSpec = tween(400), label = "onPrimaryContainer").value
    val animatedSecondary = animateColorAsState(targetColorScheme.secondary, animationSpec = tween(400), label = "secondary").value
    val animatedOnSecondary = animateColorAsState(targetColorScheme.onSecondary, animationSpec = tween(400), label = "onSecondary").value
    val animatedTertiary = animateColorAsState(targetColorScheme.tertiary, animationSpec = tween(400), label = "tertiary").value
    val animatedOnTertiary = animateColorAsState(targetColorScheme.onTertiary, animationSpec = tween(400), label = "onTertiary").value
    val animatedTertiaryContainer = animateColorAsState(targetColorScheme.tertiaryContainer, animationSpec = tween(400), label = "tertiaryContainer").value
    val animatedOnTertiaryContainer = animateColorAsState(targetColorScheme.onTertiaryContainer, animationSpec = tween(400), label = "onTertiaryContainer").value
    val animatedBackground = animateColorAsState(targetColorScheme.background, animationSpec = tween(400), label = "background").value
    val animatedSurface = animateColorAsState(targetColorScheme.surface, animationSpec = tween(400), label = "surface").value
    val animatedSurfaceVariant = animateColorAsState(targetColorScheme.surfaceVariant, animationSpec = tween(400), label = "surfaceVariant").value
    val animatedOnBackground = animateColorAsState(targetColorScheme.onBackground, animationSpec = tween(400), label = "onBackground").value
    val animatedOnSurface = animateColorAsState(targetColorScheme.onSurface, animationSpec = tween(400), label = "onSurface").value
    val animatedOnSurfaceVariant = animateColorAsState(targetColorScheme.onSurfaceVariant, animationSpec = tween(400), label = "onSurfaceVariant").value

    val animatedColorScheme = targetColorScheme.copy(
        primary = animatedPrimary,
        onPrimary = animatedOnPrimary,
        primaryContainer = animatedPrimaryContainer,
        onPrimaryContainer = animatedOnPrimaryContainer,
        secondary = animatedSecondary,
        onSecondary = animatedOnSecondary,
        tertiary = animatedTertiary,
        onTertiary = animatedOnTertiary,
        tertiaryContainer = animatedTertiaryContainer,
        onTertiaryContainer = animatedOnTertiaryContainer,
        background = animatedBackground,
        surface = animatedSurface,
        surfaceVariant = animatedSurfaceVariant,
        onBackground = animatedOnBackground,
        onSurface = animatedOnSurface,
        onSurfaceVariant = animatedOnSurfaceVariant
    )

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = Typography,
        content = content
    )
}
