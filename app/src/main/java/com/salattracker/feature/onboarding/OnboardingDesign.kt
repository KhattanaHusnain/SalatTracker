@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.salattracker.feature.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.ThemeMode
import com.salattracker.domain.model.isDark

internal val OnboardingTitleFont = FontFamily(Font(R.font.onboarding_cormorant, FontWeight.SemiBold,
    variationSettings = FontVariation.Settings(FontVariation.weight(600))))
internal val OnboardingBodyFont = FontFamily(
    Font(R.font.onboarding_outfit, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.onboarding_outfit, FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)
internal val OnboardingArabicFont = FontFamily(Font(R.font.onboarding_scheherazade))
internal val OnboardingGold = Color(0xFFD9A441)
internal val LocalOnboardingRowMinHeight = staticCompositionLocalOf { 64.dp }

/** The export's identity is local to onboarding; other features keep their own design tokens. */
@Composable
internal fun OnboardingDesign(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = mode.isDark(isSystemInDarkTheme())
    val colors = if (dark) DarkSalatColors.copy(
        primary = Color(0xFF75D3D6), onPrimary = Color(0xFF003638),
        background = Color(0xFF101C1D), surface = Color(0xFF101C1D),
        onSurface = Color(0xFFF5F5F5), onSurfaceVariant = Color(0xFFB7C5C5),
        outlineVariant = Color(0xFF354748), surfaceContainerLow = Color(0xFF233132),
    ) else LightSalatColors.copy(
        primary = Color(0xFF087E83), onPrimary = Color.White,
        background = Color.White, surface = Color.White, onSurface = Color.Black,
        onSurfaceVariant = Color(0xFF737373), outlineVariant = Color(0xFFE0E0E0),
        surfaceContainerLow = Color(0xFFF0F0F0),
    )
    CompositionLocalProvider(LocalSalatColors provides colors) {
        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
            primary = colors.primary, onPrimary = colors.onPrimary,
            background = colors.background, surface = colors.surface,
            onSurface = colors.onSurface, onSurfaceVariant = colors.onSurfaceVariant,
            outline = colors.outlineVariant,
        ), content = content)
    }
}

/** Arch lantern paths and the staggered three-lantern tile from the supplied HTML's arcL/trio.
 * Draw as vector paths so the reference artwork stays crisp at any density, then apply its fades.
 */
@Composable
internal fun OnboardingLanterns(modifier: Modifier = Modifier) {
    val outline = remember { PathParser.createPathFromPathData(
        "M0 3.8V6 M0 6L5 13.2 M0 13.2H5.8V14.8H0 M5 14.8V36 M0 36H5.8V37.6H0 M5 37.6L3.4 40.4H0"
    ).asComposePath() }
    val flame = remember { PathParser.createPathFromPathData(
        "M-3 36V22.5C-3 20 -1.2 18.4 0 16.6C1.2 18.4 3 20 3 22.5V36Z"
    ).asComposePath() }
    val pendant = remember { PathParser.createPathFromPathData(
        "M0 40.4V44.4 M0 44.4C-.8 45.4 -.8 46.4 0 47.2C.8 46.4 .8 45.4 0 44.4Z"
    ).asComposePath() }
    Canvas(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val unit = 1.15.dp.toPx()
        val tileWidth = 116 * unit
        val tileHeight = 140 * unit
        val stroke = Stroke(1.05f)
        for (row in 0..(size.height / tileHeight).toInt()) {
            for (column in 0..(size.width / tileWidth).toInt()) {
                translate(column * tileWidth, row * tileHeight) {
                    scale(unit, unit, Offset.Zero) {
                        listOf(20f to 52f, 58f to 30f, 96f to 10f).forEach { (x, y) ->
                            translate(x, y) {
                                val gold = OnboardingGold.copy(alpha = .55f)
                                drawLine(gold, Offset(0f, -y), Offset(0f, .2f), 1.05f)
                                drawOval(gold, Offset(-1.4f, .2f), Size(2.8f, 3.6f), style = stroke)
                                drawPath(outline, gold, style = stroke)
                                scale(-1f, 1f, Offset.Zero) { drawPath(outline, gold, style = stroke) }
                                drawPath(flame, OnboardingGold.copy(alpha = .22f))
                                drawPath(flame, gold, style = stroke)
                                drawPath(pendant, gold, style = stroke)
                                val end = 110 - y
                                drawLine(gold, Offset(0f, 47.6f), Offset(0f, end - 1.6f), 1.05f)
                                drawCircle(gold, 1.5f, Offset(0f, end), style = stroke)
                            }
                        }
                    }
                }
            }
        }
        // Elliptical radial fade intersected with the export's vertical 60%-100% fade.
        scale(1f, size.height / size.width, Offset.Zero) {
            drawRect(Brush.radialGradient(
                0f to Color.White, .14f to Color.White, .9f to Color.Transparent,
                center = Offset(size.width * .74f, size.width * .36f), radius = size.width,
            ), size = Size(size.width, size.width), blendMode = BlendMode.DstIn)
        }
        drawRect(Brush.verticalGradient(0f to Color.White, .6f to Color.White, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
    }
}
