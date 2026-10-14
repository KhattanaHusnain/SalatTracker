package com.salattracker.feature.stats

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.salattracker.core.theme.SalatOutline
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.domain.usecase.TrendBucket
import java.time.format.DateTimeFormatter

/** Vico is only used here, for the completion-trend line — the one time series left in Stats.
 * Every other number (summary, by prayer, by weekday, goals) is a labelled category comparison,
 * which reads better as plain Compose bars than as a chart. */

// ── Completion trend: one line, fixed 0–100 range, no scroll ──────────────────

@Composable
fun CompletionTrendChart(points: List<TrendPoint>, bucket: TrendBucket, modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        if (points.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            lineSeries { series(points.indices.toList(), points.map { it.completionPct }) }
        }
    }

    val locale = LocalConfiguration.current.locales[0]
    val labels = remember(points, bucket, locale) {
        val formatter = DateTimeFormatter.ofPattern(if (bucket == TrendBucket.MONTH) "MMM" else "d/M", locale)
        points.map { it.start.format(formatter) }
    }

    val line = LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(fill(SalatPrimary)))
    val lineProvider = LineCartesianLayer.LineProvider.series(line)
    val axisLabel = rememberTextComponent(color = SalatOutline)
    val markerLabel = rememberTextComponent(color = SalatPrimary)

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider  = lineProvider,
                rangeProvider = CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = 100.0),
            ),
            startAxis = VerticalAxis.rememberStart(label = axisLabel),
            bottomAxis = HorizontalAxis.rememberBottom(
                label = axisLabel,
                valueFormatter = CartesianValueFormatter { _, x, _ -> labels.getOrElse(x.toInt()) { "" } },
            ),
            marker = rememberDefaultCartesianMarker(label = markerLabel),
        ),
        modelProducer = modelProducer,
        // Fixed 0–100 Y and Zoom.Content (fit-all) X so the whole period is always visible at
        // once — no horizontal scroll, matching the "fits without scrolling" requirement.
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
        modifier = modifier.fillMaxWidth().height(180.dp),
    )
}
