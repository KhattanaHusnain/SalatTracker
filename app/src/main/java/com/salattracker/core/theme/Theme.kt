package com.salattracker.core.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

// ── Material 3 color scheme ───────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary              = SalatPrimary,
    onPrimary            = SalatOnPrimary,
    primaryContainer     = SalatPrimaryContainer,
    onPrimaryContainer   = SalatPrimaryFixed,
    secondary            = SalatSecondary,
    onSecondary          = SalatOnSecondary,
    secondaryContainer   = SalatSecondaryContainer,
    onSecondaryContainer = SalatOnSecondaryContainer,
    tertiary             = SalatTertiary,
    onTertiary           = SalatOnTertiary,
    tertiaryContainer    = SalatTertiaryContainer,
    onTertiaryContainer  = SalatOnTertiaryContainer,
    background           = SalatBackground,
    onBackground         = SalatOnBackground,
    surface              = SalatSurface,
    onSurface            = SalatOnSurface,
    onSurfaceVariant     = SalatOnSurfaceVariant,
    surfaceVariant       = SalatSurfaceVariant,
    outline              = SalatOutline,
    outlineVariant       = SalatOutlineVariant,
    error                = SalatError,
    onError              = SalatOnError,
    errorContainer       = SalatErrorContainer,
    onErrorContainer     = SalatOnErrorContainer,
    inverseSurface       = SalatInverseSurface,
    inverseOnSurface     = SalatInverseOnSurface,
    inversePrimary       = SalatInversePrimary,
    surfaceTint          = SalatSurfaceTint,
)

@Composable
fun SalatTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content     = content
    )
}