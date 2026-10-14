package com.salattracker.feature.stats

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.salattracker.core.theme.InterFamily
import com.salattracker.core.theme.SalatOutline
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatStatusCongregation
import com.salattracker.core.theme.SalatStatusIndividual
import com.salattracker.core.theme.SalatStatusQazaPending
import com.salattracker.domain.model.GoalDailyPoint
import com.salattracker.domain.repository.StatusTrendPoint

/** Vico is only used here for the two time-series charts — Status mix and Daily goals. Every
 * other chart in Stats (per-prayer, per-goal, the summary bar) is a labelled category
 * comparison, which reads better as plain Compose horizontal bars than as thin columns. */

// ── Status mix: one stacked column per bucket (congregation / individual / qaza) ──

@Composable
fun StatusMixChart(points: List<StatusTrendPoint>, modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        if (points.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            columnSeries {
                series(points.map { it.congregationPct })
                series(points.map { it.individualPct })
                series(points.map { it.qazaPct })
            }
        }
    }
    val labels = remember(points) { points.map { it.label } }

    val congregationComponent = rememberLineComponent(fill = fill(SalatStatusCongregation))
    val individualComponent = rememberLineComponent(fill = fill(SalatStatusIndividual))
    val qazaComponent = rememberLineComponent(fill = fill(SalatStatusQazaPending))
    val axisLabel = rememberTextComponent(color = SalatOutline)
    val markerLabel = rememberTextComponent(color = SalatPrimary)

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(congregationComponent, individualComponent, qazaComponent),
                mergeMode = { ColumnCartesianLayer.MergeMode.Stacked },
            ),
            startAxis = VerticalAxis.rememberStart(label = axisLabel),
            bottomAxis = HorizontalAxis.rememberBottom(
                label = axisLabel,
                valueFormatter = CartesianValueFormatter { _, x, _ -> labels.getOrElse(x.toInt()) { "" } },
            ),
            marker = rememberDefaultCartesianMarker(label = markerLabel),
        ),
        modelProducer = modelProducer,
        modifier = modifier.fillMaxWidth().height(200.dp),
    )
}

// ── Daily goals: one column per day, average completion rate ──────────────────

@Composable
fun DailyGoalsChart(points: List<GoalDailyPoint>, modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        if (points.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            columnSeries { series(points.map { (it.completionRate * 100).toInt() }) }
        }
    }
    val labels = remember(points) { points.map { dayNumberFormatter.format(it.date) } }

    val goalComponent = rememberLineComponent(fill = fill(SalatPrimary))
    val axisLabel = rememberTextComponent(color = SalatOutline)
    val markerLabel = rememberTextComponent(color = SalatPrimary)

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(goalComponent),
            ),
            startAxis = VerticalAxis.rememberStart(label = axisLabel),
            bottomAxis = HorizontalAxis.rememberBottom(
                label = axisLabel,
                valueFormatter = CartesianValueFormatter { _, x, _ -> labels.getOrElse(x.toInt()) { "" } },
            ),
            marker = rememberDefaultCartesianMarker(label = markerLabel),
        ),
        modelProducer = modelProducer,
        modifier = modifier.fillMaxWidth().height(200.dp),
    )
}

private val dayNumberFormatter = java.time.format.DateTimeFormatter.ofPattern("d/M")
