package com.salattracker.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import com.salattracker.core.theme.*
import com.salattracker.core.ui.ArchitecturalFrieze
import com.salattracker.domain.model.PrayerStatus

internal val HomeTeal = Color(0xFF087E83)
internal val HomeGold = Color(0xFFD9A441)
internal val HomeCream = Color(0xFFFFF8E8)
internal val HomeLatin: FontFamily = OutfitFamily
internal val HomeSerif: FontFamily = CormorantFamily
internal val HomeBodyFont: FontFamily @Composable get() = AppUiFontFamily
internal val HomeTitleFont: FontFamily @Composable get() = AppTitleFontFamily

@Composable internal fun HomeDesign(content: @Composable () -> Unit) {
    content()
}

@Composable
internal fun homeStatusColor(status: PrayerStatus): Color = when (status) {
    PrayerStatus.CONGREGATION -> SalatStatusCongregation
    PrayerStatus.INDIVIDUAL -> SalatStatusIndividual
    PrayerStatus.QAZA_PENDING -> SalatStatusQazaPending
    PrayerStatus.QAZA_COMPLETED -> SalatStatusQazaCompleted
    PrayerStatus.MENSTRUATION -> SalatStatusExcused
    PrayerStatus.NOT_RECORDED -> SalatStatusNotRecorded
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
@Composable internal fun HomeFrieze(modifier: Modifier = Modifier, color: Color = SalatPrimary) {
    ArchitecturalFrieze(modifier = modifier, color = color)
}
