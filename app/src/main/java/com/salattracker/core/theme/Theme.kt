package com.salattracker.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.salattracker.R

// ── Font families ────────────────────────────────────────────────────────────

val MontserratFamily = FontFamily(
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
    Font(R.font.montserrat_bold,     FontWeight.Bold)
)

val InterFamily = FontFamily(
    Font(R.font.inter_regular,   FontWeight.Normal),
    Font(R.font.inter_medium,    FontWeight.Medium),
    Font(R.font.inter_semibold,  FontWeight.SemiBold)
)

// ── Material 3 color schemes ──────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary              = LightSalatColors.primary,
    onPrimary            = LightSalatColors.onPrimary,
    primaryContainer     = LightSalatColors.primaryContainer,
    onPrimaryContainer   = LightSalatColors.onPrimaryContainer,
    secondary            = LightSalatColors.secondary,
    onSecondary          = LightSalatColors.onSecondary,
    secondaryContainer   = LightSalatColors.secondaryContainer,
    onSecondaryContainer = LightSalatColors.onSecondaryContainer,
    tertiary             = LightSalatColors.tertiary,
    onTertiary           = LightSalatColors.onTertiary,
    tertiaryContainer    = LightSalatColors.tertiaryContainer,
    onTertiaryContainer  = LightSalatColors.onTertiaryContainer,
    background           = LightSalatColors.background,
    onBackground         = LightSalatColors.onBackground,
    surface              = LightSalatColors.surface,
    onSurface            = LightSalatColors.onSurface,
    onSurfaceVariant     = LightSalatColors.onSurfaceVariant,
    surfaceVariant       = LightSalatColors.surfaceVariant,
    outline              = LightSalatColors.outline,
    outlineVariant       = LightSalatColors.outlineVariant,
    error                = LightSalatColors.error,
    onError              = LightSalatColors.onError,
    errorContainer       = LightSalatColors.errorContainer,
    onErrorContainer     = LightSalatColors.onErrorContainer,
    inverseSurface       = LightSalatColors.inverseSurface,
    inverseOnSurface     = LightSalatColors.inverseOnSurface,
    inversePrimary       = LightSalatColors.inversePrimary,
    surfaceTint          = LightSalatColors.surfaceTint,
)

private val DarkColorScheme = darkColorScheme(
    primary              = DarkSalatColors.primary,
    onPrimary            = DarkSalatColors.onPrimary,
    primaryContainer     = DarkSalatColors.primaryContainer,
    onPrimaryContainer   = DarkSalatColors.onPrimaryContainer,
    secondary            = DarkSalatColors.secondary,
    onSecondary          = DarkSalatColors.onSecondary,
    secondaryContainer   = DarkSalatColors.secondaryContainer,
    onSecondaryContainer = DarkSalatColors.onSecondaryContainer,
    tertiary             = DarkSalatColors.tertiary,
    onTertiary           = DarkSalatColors.onTertiary,
    tertiaryContainer    = DarkSalatColors.tertiaryContainer,
    onTertiaryContainer  = DarkSalatColors.onTertiaryContainer,
    background           = DarkSalatColors.background,
    onBackground         = DarkSalatColors.onBackground,
    surface              = DarkSalatColors.surface,
    onSurface            = DarkSalatColors.onSurface,
    onSurfaceVariant     = DarkSalatColors.onSurfaceVariant,
    surfaceVariant       = DarkSalatColors.surfaceVariant,
    outline              = DarkSalatColors.outline,
    outlineVariant       = DarkSalatColors.outlineVariant,
    error                = DarkSalatColors.error,
    onError              = DarkSalatColors.onError,
    errorContainer       = DarkSalatColors.errorContainer,
    onErrorContainer     = DarkSalatColors.onErrorContainer,
    inverseSurface       = DarkSalatColors.inverseSurface,
    inverseOnSurface     = DarkSalatColors.inverseOnSurface,
    inversePrimary       = DarkSalatColors.inversePrimary,
    surfaceTint          = DarkSalatColors.surfaceTint,
)

/**
 * [darkTheme] resolves [com.salattracker.domain.model.ThemeMode]: `LIGHT`/`DARK` pass a fixed
 * boolean, `SYSTEM` passes [isSystemInDarkTheme] (the default, matching plain Compose
 * convention for a screen with no theme preference at all). Provides [LocalSalatColors] so
 * every `SalatXxx` color getter in [Color.kt] — i.e. every existing call site in the app —
 * resolves to the right palette underneath, with no call-site changes needed.
 */
@Composable
fun SalatTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val salatColors = if (darkTheme) DarkSalatColors else LightSalatColors
    CompositionLocalProvider(LocalSalatColors provides salatColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            content     = content,
        )
    }
}
