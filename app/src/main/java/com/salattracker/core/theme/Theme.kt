@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.salattracker.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
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

/** Home v27's editorial type system, shared by every app screen. */
val OutfitFamily = FontFamily(
    Font(
        R.font.onboarding_outfit,
        FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.onboarding_outfit,
        FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.onboarding_outfit,
        FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.onboarding_outfit,
        FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

val CormorantFamily = FontFamily(
    Font(
        R.font.onboarding_cormorant,
        FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.onboarding_cormorant,
        FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

val ScheherazadeFamily = FontFamily(Font(R.font.onboarding_scheherazade))

// Quran-specific display faces — not used for app chrome, only the reader screens. Both are
// SIL Open Font License (OFL-1.1), credited in QURAN_CONTENT_SOURCES.md and QuranCreditsScreen.
// Amiri Quran: Khaled Hosny's classical Naskh revival, tuned for Quranic text — used for the
// surah-name banner, since the real per-page QCF glyph fonts only cover their own per-word
// PUA codes (see MushafRepository) and can't render arbitrary Unicode text like a chapter name;
// the official Uthmani Unicode alternative (KFGQPC Hafs) carries a no-reproduction license, so
// isn't an option. Noto Nastaliq Urdu: Google's Nastaliq-script Urdu face, for the Urdu
// translation text, replacing the plain system-fallback rendering used until now.
val AmiriQuranFamily = FontFamily(Font(R.font.amiri_quran, FontWeight.Normal))
val NastaliqUrduFamily = FontFamily(Font(R.font.noto_nastaliq_urdu, FontWeight.Normal))

/**
 * The Urdu translations use Noto Nastaliq, so Urdu-localized app chrome should use the same
 * familiar, legible script rather than falling back to Inter/Montserrat.  Keeping the switch
 * here gives every ordinary app label the appropriate face while preserving the dedicated
 * Arabic Quran font above.
 */
val AppUiFontFamily: FontFamily
    @Composable get() = when (LocalConfiguration.current.locales[0]?.language) {
        "ur" -> NastaliqUrduFamily
        "ar" -> ScheherazadeFamily
        else -> OutfitFamily
    }

val AppTitleFontFamily: FontFamily
    @Composable get() = when (LocalConfiguration.current.locales[0]?.language) {
        "ur", "ar" -> AppUiFontFamily
        else -> CormorantFamily
    }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
private fun appTypography(): Typography {
    val body = AppUiFontFamily
    val title = AppTitleFontFamily
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = title),
        displayMedium = base.displayMedium.copy(fontFamily = title),
        displaySmall = base.displaySmall.copy(fontFamily = title),
        headlineLarge = base.headlineLarge.copy(fontFamily = title),
        headlineMedium = base.headlineMedium.copy(fontFamily = title),
        headlineSmall = base.headlineSmall.copy(fontFamily = title),
        titleLarge = base.titleLarge.copy(fontFamily = title),
        titleMedium = base.titleMedium.copy(fontFamily = body),
        titleSmall = base.titleSmall.copy(fontFamily = body),
        bodyLarge = base.bodyLarge.copy(fontFamily = body),
        bodyMedium = base.bodyMedium.copy(fontFamily = body),
        bodySmall = base.bodySmall.copy(fontFamily = body),
        labelLarge = base.labelLarge.copy(fontFamily = body),
        labelMedium = base.labelMedium.copy(fontFamily = body),
        labelSmall = base.labelSmall.copy(fontFamily = body),
    )
}

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
            typography  = appTypography(),
            shapes      = AppShapes,
            content     = content,
        )
    }
}
