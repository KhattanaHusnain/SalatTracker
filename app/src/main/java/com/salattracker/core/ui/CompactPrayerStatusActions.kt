package com.salattracker.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.SalatOutlineVariant
import com.salattracker.core.theme.SalatStatusCongregation
import com.salattracker.core.theme.SalatStatusExcused
import com.salattracker.core.theme.SalatStatusIndividual
import com.salattracker.core.theme.SalatStatusNotRecorded
import com.salattracker.core.theme.SalatStatusQazaCompleted
import com.salattracker.core.theme.SalatStatusQazaPending
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType

private val CompactDivider: Color @Composable get() = SalatOutlineVariant

/** Home's inline prayer-status control, shared with Calendar's selected-day rows. */
@Composable
internal fun CompactPrayerStatusActions(
    prayerType: PrayerType,
    status: PrayerStatus,
    gender: Gender?,
    enabled: Boolean,
    tagPrefix: String,
    onStatusSelected: (PrayerStatus) -> Unit,
) {
    AnimatedContent(targetState = status, label = "$tagPrefix-status-$prayerType") { current ->
        if (current != PrayerStatus.NOT_RECORDED) {
            val ink = if (current == PrayerStatus.QAZA_COMPLETED) Color(0xFF302408) else Color.White
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(compactPrayerStatusColor(current))
                    .clickable(enabled = enabled, role = Role.Button) {
                        onStatusSelected(
                            if (current == PrayerStatus.QAZA_COMPLETED) {
                                PrayerStatus.QAZA_PENDING
                            } else {
                                PrayerStatus.NOT_RECORDED
                            },
                        )
                    }
                    .padding(horizontal = 10.dp)
                    .testTag("$tagPrefix-status-row-$prayerType"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CompactPrayerStatusArtwork(
                    status = current,
                    gender = gender,
                    selected = true,
                    modifier = Modifier.size(22.dp),
                    vectorTint = ink,
                )
                Text(
                    text = compactPrayerStatusLabel(current, gender),
                    color = ink,
                    fontFamily = AppUiFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                compactOfferedStatuses(gender).forEach { choice ->
                    val description = compactPrayerStatusLabel(choice, gender)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .border(1.5.dp, CompactDivider, CircleShape)
                            .testTag("$tagPrefix-pick-row-$prayerType-$choice")
                            .semantics {
                                contentDescription = description
                                if (!enabled) disabled()
                            }
                            .clickable(enabled = enabled, role = Role.Button) {
                                onStatusSelected(choice)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        CompactPrayerStatusArtwork(
                            status = choice,
                            gender = gender,
                            selected = false,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun compactPrayerStatusLabel(status: PrayerStatus, gender: Gender?): String = when {
    status == PrayerStatus.INDIVIDUAL && gender == Gender.FEMALE -> stringResource(R.string.home_v8_prayed)
    status == PrayerStatus.MENSTRUATION -> stringResource(R.string.home_v8_excused)
    status == PrayerStatus.QAZA_PENDING -> stringResource(R.string.home_v8_qaza)
    status == PrayerStatus.QAZA_COMPLETED -> stringResource(R.string.home_v8_made_up)
    else -> status.label()
}

@Composable
internal fun compactPrayerStatusColor(status: PrayerStatus): Color = when (status) {
    PrayerStatus.CONGREGATION -> SalatStatusCongregation
    PrayerStatus.INDIVIDUAL -> SalatStatusIndividual
    PrayerStatus.QAZA_PENDING -> SalatStatusQazaPending
    PrayerStatus.QAZA_COMPLETED -> SalatStatusQazaCompleted
    PrayerStatus.MENSTRUATION -> SalatStatusExcused
    PrayerStatus.NOT_RECORDED -> SalatStatusNotRecorded
}

private fun compactOfferedStatuses(gender: Gender?): List<PrayerStatus> =
    if (gender == Gender.FEMALE) {
        listOf(PrayerStatus.INDIVIDUAL, PrayerStatus.MENSTRUATION, PrayerStatus.QAZA_PENDING)
    } else {
        listOf(PrayerStatus.CONGREGATION, PrayerStatus.INDIVIDUAL, PrayerStatus.QAZA_PENDING)
    }

@Composable
private fun CompactPrayerStatusArtwork(
    status: PrayerStatus,
    gender: Gender?,
    selected: Boolean,
    modifier: Modifier,
    vectorTint: Color = compactPrayerStatusColor(status),
) {
    val raster = compactStatusRaster(status, gender, selected)
    if (raster != null) {
        Image(painter = painterResource(raster), contentDescription = null, modifier = modifier)
    } else {
        Icon(
            painter = painterResource(compactStatusIcon(status, gender)),
            contentDescription = null,
            tint = vectorTint,
            modifier = modifier,
        )
    }
}

private fun compactStatusRaster(status: PrayerStatus, gender: Gender?, selected: Boolean): Int? = when (status) {
    PrayerStatus.CONGREGATION -> if (selected) R.drawable.home_status_cg_selected else R.drawable.home_status_cg_outline
    PrayerStatus.INDIVIDUAL -> if (gender == Gender.FEMALE) {
        if (selected) R.drawable.home_status_prayed_selected else R.drawable.home_status_prayed_outline
    } else if (selected) {
        R.drawable.home_status_ci_selected
    } else {
        R.drawable.home_status_ci_outline
    }
    else -> null
}

private fun compactStatusIcon(status: PrayerStatus, gender: Gender?): Int = when (status) {
    PrayerStatus.CONGREGATION -> R.drawable.home_status_congregation
    PrayerStatus.INDIVIDUAL -> if (gender == Gender.FEMALE) R.drawable.home_status_prayed else R.drawable.home_status_individual
    PrayerStatus.MENSTRUATION -> R.drawable.home_status_excused
    PrayerStatus.QAZA_PENDING -> R.drawable.home_status_qaza
    PrayerStatus.QAZA_COMPLETED -> R.drawable.home_status_made_up
    PrayerStatus.NOT_RECORDED -> R.drawable.home_status_qaza
}
