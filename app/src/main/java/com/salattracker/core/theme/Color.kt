package com.salattracker.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Every brand/surface/status color the app draws with, as one snapshot — either the light or
 * the dark set. [SalatTrackerTheme] provides the right one via [LocalSalatColors] based on the
 * resolved [com.salattracker.domain.model.ThemeMode]; every `SalatXxx` property below just reads
 * the current one, so every existing call site (`color = SalatPrimary`, etc., all over the app)
 * stays exactly as written and picks up dark mode automatically — only this file and Theme.kt
 * needed to change.
 */
data class SalatColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val primaryFixed: Color,
    val primaryFixedDim: Color,
    val inversePrimary: Color,

    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    val onHero: Color,

    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,

    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,

    val background: Color,
    val onBackground: Color,

    val surface: Color,
    val surfaceBright: Color,
    val surfaceDim: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val surfaceVariant: Color,
    val surfaceTint: Color,

    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,

    val outline: Color,
    val outlineVariant: Color,

    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,

    val inverseSurface: Color,
    val inverseOnSurface: Color,

    // "Fixed" roles are deliberately the same value in both [LightSalatColors] and
    // [DarkSalatColors] — that's what makes them "fixed" per the Material3 spec: a surface like
    // DayDetailBottomSheet's icon chip that must look the same regardless of theme.
    val secondaryFixed: Color,
    val onSecondaryFixedVariant: Color,
    val onPrimaryFixedVariant: Color,
    val onSecondaryFixed: Color,

    // Status tones (Calendar rings, Stats bars, Qaza pills) are vivid enough to read on both a
    // light and a near-black background, so these are the same in both palettes too.
    val statusCongregation: Color,
    val statusIndividual: Color,
    val statusQazaPending: Color,
    val statusQazaCompleted: Color,
    val statusExcused: Color,
    val statusNotRecorded: Color,
)

// ── Light palette (matches Tailwind tokens in the HTML) ─────────────────────

val LightSalatColors = SalatColors(
    primary              = Color(0xFF003527),
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFF064E3B),
    onPrimaryContainer   = Color(0xFF95D3BA),
    primaryFixed         = Color(0xFFB0F0D6),
    primaryFixedDim      = Color(0xFF95D3BA),
    inversePrimary       = Color(0xFF95D3BA),
    heroGradientStart    = Color(0xFF064E3B),
    heroGradientEnd      = Color(0xFF003527),
    onHero               = Color(0xFFFFFFFF),

    secondary            = Color(0xFF52625C),
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFD3E3DC),
    onSecondaryContainer = Color(0xFF566660),

    tertiary             = Color(0xFF4A2400),
    onTertiary           = Color(0xFFFFFFFF),
    tertiaryContainer    = Color(0xFF6A3700),
    onTertiaryContainer  = Color(0xFFFF9939),

    background           = Color(0xFFF8FAF6),
    onBackground         = Color(0xFF191C1B),

    surface              = Color(0xFFF8FAF6),
    surfaceBright        = Color(0xFFF8FAF6),
    surfaceDim           = Color(0xFFD8DBD7),
    onSurface            = Color(0xFF191C1B),
    onSurfaceVariant     = Color(0xFF404944),
    surfaceVariant       = Color(0xFFE1E3E0),
    surfaceTint          = Color(0xFF2B6954),

    surfaceContainerLowest  = Color(0xFFFFFFFF),
    surfaceContainerLow     = Color(0xFFF2F4F1),
    surfaceContainer        = Color(0xFFECEEEB),
    surfaceContainerHigh    = Color(0xFFE7E9E5),
    surfaceContainerHighest = Color(0xFFE1E3E0),

    outline              = Color(0xFF707974),
    outlineVariant       = Color(0xFFBFC9C3),

    error                = Color(0xFFBA1A1A),
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF93000A),

    inverseSurface       = Color(0xFF2E312F),
    inverseOnSurface     = Color(0xFFEFF1EE),

    secondaryFixed           = Color(0xFFD5E6DF),
    onSecondaryFixedVariant  = Color(0xFF3B4A44),
    onPrimaryFixedVariant    = Color(0xFF0B513D),
    onSecondaryFixed         = Color(0xFF101E1A),

    statusCongregation   = Color(0xFF10B981),
    statusIndividual     = Color(0xFFEAB308),
    statusQazaPending    = Color(0xFFEF4444),
    statusQazaCompleted  = Color(0xFFA855F7),
    statusExcused        = Color(0xFFF472B6),
    statusNotRecorded    = Color(0xFFCBD5E1),
)

// ── Dark palette ──────────────────────────────────────────────────────────────
// Surfaces invert to near-black, growing lighter with elevation (the opposite direction from
// light mode); primary/secondary swap to their lighter tone so they stay legible on a dark
// background, with onPrimary/onSecondary flipping to dark text. Error uses the standard M3 dark
// error pair (BA1A1A is too low-contrast on a near-black surface).

val DarkSalatColors = SalatColors(
    primary              = Color(0xFF95D3BA),
    onPrimary            = Color(0xFF003527),
    primaryContainer     = Color(0xFF0B513D),
    onPrimaryContainer   = Color(0xFFB0F0D6),
    primaryFixed         = Color(0xFFB0F0D6),
    primaryFixedDim      = Color(0xFF95D3BA),
    inversePrimary       = Color(0xFF003527),
    heroGradientStart    = Color(0xFF0B513D),
    heroGradientEnd      = Color(0xFF06382A),
    onHero               = Color(0xFFFFFFFF),

    secondary            = Color(0xFFBBCBC4),
    onSecondary          = Color(0xFF263430),
    secondaryContainer   = Color(0xFF3B4A44),
    onSecondaryContainer = Color(0xFFD5E6DF),

    tertiary             = Color(0xFFFF9939),
    onTertiary           = Color(0xFF2B1400),
    tertiaryContainer    = Color(0xFF6A3700),
    onTertiaryContainer  = Color(0xFFFFDAB3),

    background           = Color(0xFF191C1B),
    onBackground         = Color(0xFFE1E3E0),

    surface              = Color(0xFF191C1B),
    surfaceBright        = Color(0xFF333633),
    surfaceDim           = Color(0xFF0F1210),
    onSurface            = Color(0xFFE1E3E0),
    onSurfaceVariant     = Color(0xFFBFC9C3),
    surfaceVariant       = Color(0xFF404944),
    surfaceTint          = Color(0xFF95D3BA),

    surfaceContainerLowest  = Color(0xFF0F1210),
    surfaceContainerLow     = Color(0xFF171A18),
    surfaceContainer        = Color(0xFF1D201E),
    surfaceContainerHigh    = Color(0xFF282B29),
    surfaceContainerHighest = Color(0xFF333633),

    outline              = Color(0xFF8A938D),
    outlineVariant       = Color(0xFF3A423D),

    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),

    inverseSurface       = Color(0xFFE1E3E0),
    inverseOnSurface     = Color(0xFF191C1B),

    secondaryFixed           = Color(0xFFD5E6DF),
    onSecondaryFixedVariant  = Color(0xFF3B4A44),
    onPrimaryFixedVariant    = Color(0xFF0B513D),
    onSecondaryFixed         = Color(0xFF101E1A),

    statusCongregation   = Color(0xFF10B981),
    statusIndividual     = Color(0xFFEAB308),
    statusQazaPending    = Color(0xFFEF4444),
    statusQazaCompleted  = Color(0xFFA855F7),
    statusExcused        = Color(0xFFF472B6),
    statusNotRecorded    = Color(0xFFCBD5E1),
)

val LocalSalatColors = staticCompositionLocalOf { LightSalatColors }

// ── Flat accessors — every existing call site (`color = SalatPrimary`) is unchanged ──────────

val SalatPrimary: Color @Composable get() = LocalSalatColors.current.primary
val SalatOnPrimary: Color @Composable get() = LocalSalatColors.current.onPrimary
val SalatPrimaryContainer: Color @Composable get() = LocalSalatColors.current.primaryContainer
val SalatOnPrimaryContainer: Color @Composable get() = LocalSalatColors.current.onPrimaryContainer
val SalatPrimaryFixed: Color @Composable get() = LocalSalatColors.current.primaryFixed
val SalatPrimaryFixedDim: Color @Composable get() = LocalSalatColors.current.primaryFixedDim
val SalatInversePrimary: Color @Composable get() = LocalSalatColors.current.inversePrimary
val SalatHeroGradientStart: Color @Composable get() = LocalSalatColors.current.heroGradientStart
val SalatHeroGradientEnd: Color @Composable get() = LocalSalatColors.current.heroGradientEnd
val SalatOnHero: Color @Composable get() = LocalSalatColors.current.onHero

val SalatSecondary: Color @Composable get() = LocalSalatColors.current.secondary
val SalatOnSecondary: Color @Composable get() = LocalSalatColors.current.onSecondary
val SalatSecondaryContainer: Color @Composable get() = LocalSalatColors.current.secondaryContainer
val SalatOnSecondaryContainer: Color @Composable get() = LocalSalatColors.current.onSecondaryContainer

val SalatTertiary: Color @Composable get() = LocalSalatColors.current.tertiary
val SalatOnTertiary: Color @Composable get() = LocalSalatColors.current.onTertiary
val SalatTertiaryContainer: Color @Composable get() = LocalSalatColors.current.tertiaryContainer
val SalatOnTertiaryContainer: Color @Composable get() = LocalSalatColors.current.onTertiaryContainer

val SalatBackground: Color @Composable get() = LocalSalatColors.current.background
val SalatOnBackground: Color @Composable get() = LocalSalatColors.current.onBackground

val SalatSurface: Color @Composable get() = LocalSalatColors.current.surface
val SalatSurfaceBright: Color @Composable get() = LocalSalatColors.current.surfaceBright
val SalatSurfaceDim: Color @Composable get() = LocalSalatColors.current.surfaceDim
val SalatOnSurface: Color @Composable get() = LocalSalatColors.current.onSurface
val SalatOnSurfaceVariant: Color @Composable get() = LocalSalatColors.current.onSurfaceVariant
val SalatSurfaceVariant: Color @Composable get() = LocalSalatColors.current.surfaceVariant
val SalatSurfaceTint: Color @Composable get() = LocalSalatColors.current.surfaceTint

val SalatSurfaceContainerLowest: Color @Composable get() = LocalSalatColors.current.surfaceContainerLowest
val SalatSurfaceContainerLow: Color @Composable get() = LocalSalatColors.current.surfaceContainerLow
val SalatSurfaceContainer: Color @Composable get() = LocalSalatColors.current.surfaceContainer
val SalatSurfaceContainerHigh: Color @Composable get() = LocalSalatColors.current.surfaceContainerHigh
val SalatSurfaceContainerHighest: Color @Composable get() = LocalSalatColors.current.surfaceContainerHighest

val SalatOutline: Color @Composable get() = LocalSalatColors.current.outline
val SalatOutlineVariant: Color @Composable get() = LocalSalatColors.current.outlineVariant

val SalatError: Color @Composable get() = LocalSalatColors.current.error
val SalatOnError: Color @Composable get() = LocalSalatColors.current.onError
val SalatErrorContainer: Color @Composable get() = LocalSalatColors.current.errorContainer
val SalatOnErrorContainer: Color @Composable get() = LocalSalatColors.current.onErrorContainer

val SalatInverseSurface: Color @Composable get() = LocalSalatColors.current.inverseSurface
val SalatInverseOnSurface: Color @Composable get() = LocalSalatColors.current.inverseOnSurface

// ── Extra tokens used by DayDetailBottomSheet (Fixed roles — same in both themes) ───────────
val SalatSecondaryFixed: Color @Composable get() = LocalSalatColors.current.secondaryFixed
val SalatOnSecondaryFixedVariant: Color @Composable get() = LocalSalatColors.current.onSecondaryFixedVariant
val SalatOnPrimaryFixedVariant: Color @Composable get() = LocalSalatColors.current.onPrimaryFixedVariant
val SalatOnSecondaryFixed: Color @Composable get() = LocalSalatColors.current.onSecondaryFixed

// ── Status tone palette — shared by the Calendar rings/year tiles, the Stats trend
// chart and their legends, so a status always reads the same colour everywhere ──
val SalatStatusCongregation: Color @Composable get() = LocalSalatColors.current.statusCongregation
val SalatStatusIndividual: Color @Composable get() = LocalSalatColors.current.statusIndividual
val SalatStatusQazaPending: Color @Composable get() = LocalSalatColors.current.statusQazaPending
val SalatStatusQazaCompleted: Color @Composable get() = LocalSalatColors.current.statusQazaCompleted
val SalatStatusExcused: Color @Composable get() = LocalSalatColors.current.statusExcused
val SalatStatusNotRecorded: Color @Composable get() = LocalSalatColors.current.statusNotRecorded
