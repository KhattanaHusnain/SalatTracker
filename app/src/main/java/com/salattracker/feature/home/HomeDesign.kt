@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.salattracker.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.PrayerStatus

internal val HomeTeal = Color(0xFF087E83)
internal val HomeGold = Color(0xFFD9A441)
internal val HomeCream = Color(0xFFFFF8E8)
internal val HomeLatin = FontFamily(Font(R.font.onboarding_outfit,
    variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.onboarding_outfit, FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))))
internal val HomeSerif = FontFamily(Font(R.font.onboarding_cormorant, FontWeight.SemiBold,
    variationSettings = FontVariation.Settings(FontVariation.weight(600))))
internal val HomeBodyFont: FontFamily @Composable get() = when (LocalConfiguration.current.locales[0].language) {
    "ur" -> NastaliqUrduFamily
    "ar" -> FontFamily(Font(R.font.onboarding_scheherazade))
    else -> HomeLatin
}
internal val HomeTitleFont: FontFamily @Composable get() =
    if (LocalConfiguration.current.locales[0].language in listOf("ur", "ar")) HomeBodyFont else HomeSerif

@Composable internal fun HomeDesign(content: @Composable () -> Unit) {
    // The v27 phone canvas is intentionally a white editorial surface in both host
    // themes. Only the surrounding prototype page changes in dark mode.
    val colors = LightSalatColors.copy(
        primary = HomeTeal,
        background = Color.White,
        surface = Color.White,
        onSurface = Color(0xFF111111),
        onSurfaceVariant = Color(0xFF6B7270),
        outlineVariant = Color(0xFFE3E6E2),
        statusCongregation = HomeTeal, statusIndividual = Color(0xFF6DBBB6),
        statusQazaPending = Color(0xFFD9603B), statusQazaCompleted = HomeGold,
        statusExcused = Color(0xFFC9338E), statusNotRecorded = Color(0xFFDADFDA),
    )
    CompositionLocalProvider(LocalSalatColors provides colors) {
        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = colors.primary,
            background = colors.background, surface = colors.surface, onSurface = colors.onSurface,
            onSurfaceVariant = colors.onSurfaceVariant, outlineVariant = colors.outlineVariant), content = content)
    }
}

internal fun homeStatusColor(status: PrayerStatus) = when (status) {
    PrayerStatus.CONGREGATION -> HomeTeal
    PrayerStatus.INDIVIDUAL -> Color(0xFF6DBBB6)
    PrayerStatus.QAZA_PENDING -> Color(0xFFD9603B)
    PrayerStatus.QAZA_COMPLETED -> HomeGold
    PrayerStatus.MENSTRUATION -> Color(0xFFC9338E)
    PrayerStatus.NOT_RECORDED -> Color(0xFFDADFDA)
}

/** The exported v27 mihrab, including its earned fill and congregation inset. */
@Composable internal fun HomeArch(
    progress: Float,
    modifier: Modifier = Modifier,
    logged: Boolean = false,
    excused: Boolean = false,
    congregation: Boolean = false,
) {
    val arch = remember { PathParser.createPathFromPathData("M24 222V96C24 54 72 38 85 6C98 38 146 54 146 96V222").asComposePath() }
    val inner = remember { PathParser.createPathFromPathData("M32 222V100C32 64 76 50 85 22C94 50 138 64 138 100V222").asComposePath() }
    Canvas(modifier) {
        scale(size.width / 170f, size.height / 222f, Offset.Zero) {
            if (logged) {
                drawPath(
                    arch,
                    brush = Brush.verticalGradient(
                        listOf(
                            (if (excused) Color(0xFFC9338E) else HomeGold).copy(alpha = .06f),
                            (if (excused) Color(0xFFE873BC) else Color(0xFFF2C86B)).copy(alpha = .60f),
                        ),
                        startY = 6f,
                        endY = 222f,
                    ),
                )
            }
            drawPath(arch, Color.White.copy(alpha = .3f), style = Stroke(3f))
            val measure = PathMeasure().apply { setPath(arch, false) }
            val portion = Path()
            measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), portion, true)
            drawPath(portion, if (excused) Color(0xFFD63E9E) else HomeGold,
                style = Stroke(3.5f, cap = StrokeCap.Round))
            if (congregation) drawPath(inner, Color(0xFFF2C86B).copy(alpha = .85f),
                style = Stroke(1.6f))
        }
    }
}

@Composable internal fun HomePlaque(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val outer = remember { PathParser.createPathFromPathData("M2 49V21C2 12 12 10 20 1C28 10 38 12 38 21V49Z").asComposePath() }
    val inner = remember { PathParser.createPathFromPathData("M5.5 46V21.5C5.5 14.5 13 12.5 20 5C27 12.5 34.5 14.5 34.5 21.5V46Z").asComposePath() }
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) {
            scale(size.width / 40f, size.height / 50f, Offset.Zero) {
                drawPath(outer, HomeGold)
                drawPath(inner, HomeCream.copy(alpha = .8f), style = Stroke(1f))
            }
        }
        content()
    }
}

/** Small repeating arch ornament, taken from the export's --pt-w/--pt-t SVG. */
@Composable internal fun HomeFrieze(modifier: Modifier = Modifier, color: Color = HomeTeal) {
    val path = remember { PathParser.createPathFromPathData("M3 16V9.5C3 6 11 5 14 0C17 5 25 6 25 9.5V16 M14 7.5V10.5").asComposePath() }
    Canvas(modifier) {
        val unit = 1.dp.toPx()
        for (column in 0..(size.width / (28 * unit)).toInt()) {
            translate(column * 28 * unit) { scale(unit, unit, Offset.Zero) {
                drawPath(path, color.copy(alpha = .55f), style = Stroke(1f, cap = StrokeCap.Round))
                drawCircle(color.copy(alpha = .55f), 1.3f, Offset(14f, 12.6f))
            } }
        }
    }
}
