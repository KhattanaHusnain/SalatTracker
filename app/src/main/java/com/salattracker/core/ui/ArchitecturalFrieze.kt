package com.salattracker.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import com.salattracker.core.theme.SalatPrimary

/** Repeating mihrab ornament from the Home v27 identity, reusable in shared app chrome. */
@Composable
fun ArchitecturalFrieze(
    modifier: Modifier = Modifier,
    color: Color? = null,
    alpha: Float = .55f,
) {
    val ink = color ?: SalatPrimary
    val path = remember {
        PathParser.createPathFromPathData(
            "M3 16V9.5C3 6 11 5 14 0C17 5 25 6 25 9.5V16 M14 7.5V10.5",
        ).asComposePath()
    }
    Canvas(modifier) {
        val unit = 1.dp.toPx()
        for (column in 0..(size.width / (28 * unit)).toInt()) {
            translate(column * 28 * unit) {
                scale(unit, unit, Offset.Zero) {
                    drawPath(path, ink.copy(alpha = alpha), style = Stroke(1f, cap = StrokeCap.Round))
                    drawCircle(ink.copy(alpha = alpha), 1.3f, Offset(14f, 12.6f))
                }
            }
        }
    }
}
