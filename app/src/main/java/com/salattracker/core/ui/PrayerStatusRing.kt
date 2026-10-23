package com.salattracker.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.salattracker.core.theme.SalatStatusCongregation
import com.salattracker.core.theme.SalatStatusExcused
import com.salattracker.core.theme.SalatStatusIndividual
import com.salattracker.core.theme.SalatStatusNotRecorded
import com.salattracker.core.theme.SalatStatusQazaCompleted
import com.salattracker.core.theme.SalatStatusQazaPending
import com.salattracker.R
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType

/**
 * The 5-segment status ring used throughout the app (Calendar's day cells, Home's hero and week
 * strip): one arc per [PrayerType] in clockwise order starting at the top, coloured by that
 * prayer's status for the day. [emptyColor] lets Home's hero tint unrecorded segments white
 * instead of the default grey. [sweepFraction] shrinks every arc toward its start angle (0f..1f)
 * for a one-shot "fill in" animation; callers drive it with their own [androidx.compose.animation.core.Animatable].
 */
@Composable
fun PrayerStatusRing(
    statuses: Map<PrayerType, PrayerStatus>,
    modifier: Modifier = Modifier,
    emptyColor: Color = SalatStatusNotRecorded,
    strokeFraction: Float = 0.12f,
    sweepFraction: Float = 1f,
) {
    // Resolved once per recomposition, in composable scope — Canvas's onDraw lambda below isn't
    // itself @Composable, so every color it uses must already be a plain value by the time it
    // runs (this is also why SalatStatusXxx, now theme-aware composable getters, can't be read
    // directly inside that lambda).
    val ringColors = PrayerType.values().map { type -> statusRingColor(statuses[type], emptyColor) }
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * strokeFraction
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        val segmentSweep = 360f / PrayerType.values().size
        val gap = 4f
        PrayerType.values().forEachIndexed { index, type ->
            drawArc(
                color = ringColors[index],
                startAngle = -90f + index * segmentSweep + gap / 2f,
                sweepAngle = (segmentSweep - gap) * sweepFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
internal fun statusRingColor(status: PrayerStatus?, emptyColor: Color = SalatStatusNotRecorded): Color = when (status) {
    PrayerStatus.CONGREGATION -> SalatStatusCongregation
    PrayerStatus.INDIVIDUAL -> SalatStatusIndividual
    PrayerStatus.QAZA_PENDING -> SalatStatusQazaPending
    PrayerStatus.QAZA_COMPLETED -> SalatStatusQazaCompleted
    PrayerStatus.MENSTRUATION -> SalatStatusExcused
    PrayerStatus.NOT_RECORDED, null -> emptyColor
}

/** Canonical status icon, shared by pickers, calendar cells, and history chips. */
val PrayerStatus.iconRes: Int get() = when (this) {
    PrayerStatus.CONGREGATION -> R.drawable.ic_groups
    PrayerStatus.INDIVIDUAL -> R.drawable.ic_person
    PrayerStatus.QAZA_PENDING -> R.drawable.ic_pending_actions
    PrayerStatus.QAZA_COMPLETED -> R.drawable.ic_check_circle_filled
    PrayerStatus.MENSTRUATION -> R.drawable.ic_female
    PrayerStatus.NOT_RECORDED -> R.drawable.ic_radio_unchecked
}
