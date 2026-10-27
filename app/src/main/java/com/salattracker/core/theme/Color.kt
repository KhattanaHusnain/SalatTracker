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

// ── Home v27 light palette ───────────────────────────────────────────────────

val LightSalatColors = SalatColors(
    primary              = Color(0xFF087E83),
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFF07595C),
    onPrimaryContainer   = Color(0xFFE8F7F6),
    primaryFixed         = Color(0xFFBDE5E2),
    primaryFixedDim      = Color(0xFF8DD0CC),
    inversePrimary       = Color(0xFF8DD0CC),
    heroGradientStart    = Color(0xFF086E72),
    heroGradientEnd      = Color(0xFF05474B),
    onHero               = Color(0xFFFFFFFF),

    secondary            = Color(0xFF6B7270),
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFEEF1EE),
    onSecondaryContainer = Color(0xFF39413F),

    tertiary             = Color(0xFFD9A441),
    onTertiary           = Color(0xFF2D2108),
    tertiaryContainer    = Color(0xFFFFF8E8),
    onTertiaryContainer  = Color(0xFF60450C),

    background           = Color(0xFFFFFFFF),
    onBackground         = Color(0xFF111111),

    surface              = Color(0xFFFFFFFF),
    surfaceBright        = Color(0xFFFFFFFF),
    surfaceDim           = Color(0xFFE3E6E2),
    onSurface            = Color(0xFF111111),
    onSurfaceVariant     = Color(0xFF6B7270),
    surfaceVariant       = Color(0xFFF1F3F1),
    surfaceTint          = Color(0xFF087E83),

    surfaceContainerLowest  = Color(0xFFFFFFFF),
    surfaceContainerLow     = Color(0xFFF8FAF8),
    surfaceContainer        = Color(0xFFF3F5F3),
    surfaceContainerHigh    = Color(0xFFEEF1EE),
    surfaceContainerHighest = Color(0xFFE7EBE8),

    outline              = Color(0xFF8A9490),
    outlineVariant       = Color(0xFFE3E6E2),

    error                = Color(0xFFBA1A1A),
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF93000A),

    inverseSurface       = Color(0xFF10292B),
    inverseOnSurface     = Color(0xFFF4F7F6),

    secondaryFixed           = Color(0xFFE4F1F0),
    onSecondaryFixedVariant  = Color(0xFF40504E),
    onPrimaryFixedVariant    = Color(0xFF07595C),
    onSecondaryFixed         = Color(0xFF172523),

    statusCongregation   = Color(0xFF087E83),
    statusIndividual     = Color(0xFF6DBBB6),
    statusQazaPending    = Color(0xFFD9603B),
    statusQazaCompleted  = Color(0xFFD9A441),
    statusExcused        = Color(0xFFC9338E),
    statusNotRecorded    = Color(0xFFDADFDA),
)

// ── Home v27 dark palette ────────────────────────────────────────────────────
// Surfaces invert to near-black, growing lighter with elevation (the opposite direction from
// light mode); primary/secondary swap to their lighter tone so they stay legible on a dark
// background, with onPrimary/onSecondary flipping to dark text. Error uses the standard M3 dark
// error pair (BA1A1A is too low-contrast on a near-black surface).

val DarkSalatColors = SalatColors(
    primary              = Color(0xFF73CBC8),
    onPrimary            = Color(0xFF062F31),
    primaryContainer     = Color(0xFF07595C),
    onPrimaryContainer   = Color(0xFFBDE5E2),
    primaryFixed         = Color(0xFFBDE5E2),
    primaryFixedDim      = Color(0xFF8DD0CC),
    inversePrimary       = Color(0xFF087E83),
    heroGradientStart    = Color(0xFF086E72),
    heroGradientEnd      = Color(0xFF04383B),
    onHero               = Color(0xFFFFFFFF),

    secondary            = Color(0xFFB7C5C5),
    onSecondary          = Color(0xFF243334),
    secondaryContainer   = Color(0xFF344447),
    onSecondaryContainer = Color(0xFFDDE8E7),

    tertiary             = Color(0xFFF2C86B),
    onTertiary           = Color(0xFF342600),
    tertiaryContainer    = Color(0xFF5C4311),
    onTertiaryContainer  = Color(0xFFFFE4A3),

    background           = Color(0xFF101C1D),
    onBackground         = Color(0xFFF1F5F4),

    surface              = Color(0xFF101C1D),
    surfaceBright        = Color(0xFF2A393A),
    surfaceDim           = Color(0xFF091112),
    onSurface            = Color(0xFFF1F5F4),
    onSurfaceVariant     = Color(0xFFB7C5C5),
    surfaceVariant       = Color(0xFF344447),
    surfaceTint          = Color(0xFF73CBC8),

    surfaceContainerLowest  = Color(0xFF091112),
    surfaceContainerLow     = Color(0xFF142224),
    surfaceContainer        = Color(0xFF19292B),
    surfaceContainerHigh    = Color(0xFF203234),
    surfaceContainerHighest = Color(0xFF2A3C3E),

    outline              = Color(0xFF849392),
    outlineVariant       = Color(0xFF344447),

    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),

    inverseSurface       = Color(0xFFF1F5F4),
    inverseOnSurface     = Color(0xFF102021),

    secondaryFixed           = Color(0xFFE4F1F0),
    onSecondaryFixedVariant  = Color(0xFF40504E),
    onPrimaryFixedVariant    = Color(0xFF07595C),
    onSecondaryFixed         = Color(0xFF172523),

    statusCongregation   = Color(0xFF73CBC8),
    statusIndividual     = Color(0xFF83D2CE),
    statusQazaPending    = Color(0xFFE77B58),
    statusQazaCompleted  = Color(0xFFF2C86B),
    statusExcused        = Color(0xFFE873BC),
    statusNotRecorded    = Color(0xFF7D8987),
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
