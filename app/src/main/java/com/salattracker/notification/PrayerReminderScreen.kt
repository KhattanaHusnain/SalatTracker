package com.salattracker.notification

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.InterFamily
import com.salattracker.core.theme.MontserratFamily
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatPrimaryContainer
import com.salattracker.core.theme.SalatPrimaryFixed
import com.salattracker.core.theme.SalatPrimaryFixedDim
import com.salattracker.core.ui.label
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.VerseInfo
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun PrayerReminderScreen(
    prayerType: PrayerType,
    date: LocalDate,
    jumuahEnabled: Boolean,
    madhab: Madhab,
    scheduledTime: LocalTime?,
    verse: VerseInfo?,
    snoozeMinutes: Int,
    onMark: (PrayerStatus) -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(SalatPrimaryContainer, SalatPrimary, Color.Black))
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // ── Clock + prayer info ──────────────────────────────────────────
            Text(
                text       = now.format(timeFormatter),
                color      = SalatPrimaryFixed,
                fontSize   = 64.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MontserratFamily,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text          = stringResource(R.string.reminder_prayer_title, prayerType.label(date, jumuahEnabled, madhab)).uppercase(),
                color         = Color.White,
                fontSize      = 20.sp,
                fontWeight    = FontWeight.SemiBold,
                fontFamily    = MontserratFamily,
                letterSpacing = 3.sp,
            )
            if (scheduledTime != null) {
                Text(
                    text     = stringResource(R.string.reminder_scheduled_for, scheduledTime.format(timeFormatter)),
                    color    = SalatPrimaryFixedDim.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontFamily = InterFamily,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Pulsing logo + verse ──────────────────────────────────────────
            val infiniteTransition = rememberInfiniteTransition(label = "breathe")
            val scale by infiniteTransition.animateFloat(
                initialValue  = 1f,
                targetValue   = 1.1f,
                animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
                label         = "scale",
            )
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter  = painterResource(R.drawable.ic_mosque),
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(88.dp),
                )
            }

            if (verse != null) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text       = "“${verse.text}”",
                    color      = Color.White.copy(alpha = 0.9f),
                    fontSize   = 16.sp,
                    fontFamily = InterFamily,
                    textAlign  = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier   = Modifier.padding(horizontal = 8.dp),
                )
                if (verse.citation.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text     = verse.citation,
                        color    = SalatPrimaryFixedDim.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = InterFamily,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Action grid ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ReminderActionTile(
                    iconRes  = R.drawable.ic_groups,
                    label    = stringResource(R.string.status_congregation),
                    modifier = Modifier.weight(1f),
                    onClick  = { onMark(PrayerStatus.CONGREGATION) },
                )
                ReminderActionTile(
                    iconRes  = R.drawable.ic_person,
                    label    = stringResource(R.string.status_individual),
                    modifier = Modifier.weight(1f),
                    onClick  = { onMark(PrayerStatus.INDIVIDUAL) },
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ReminderActionTile(
                    iconRes  = R.drawable.ic_pending_actions,
                    label    = stringResource(R.string.status_qaza_pending),
                    modifier = Modifier.weight(1f),
                    onClick  = { onMark(PrayerStatus.QAZA_PENDING) },
                )
                ReminderActionTile(
                    iconRes  = R.drawable.ic_schedule,
                    label    = stringResource(R.string.reminder_snooze, snoozeMinutes),
                    modifier = Modifier.weight(1f),
                    onClick  = onSnooze,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onDismiss) {
                Icon(
                    painter  = painterResource(R.drawable.ic_close),
                    contentDescription = null,
                    tint     = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text          = stringResource(R.string.reminder_dismiss),
                    color         = Color.White.copy(alpha = 0.6f),
                    fontSize      = 12.sp,
                    fontFamily    = InterFamily,
                    letterSpacing = 2.sp,
                )
            }
        }
    }
}

@Composable
private fun ReminderActionTile(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick  = onClick,
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        color    = Color.White.copy(alpha = 0.1f),
        border   = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter  = painterResource(iconRes),
                contentDescription = null,
                tint     = SalatPrimaryFixed,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text       = label,
                color      = Color.White,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFamily,
            )
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PrayerReminderScreenPreview() {
    PrayerReminderScreen(
        prayerType    = PrayerType.ASR,
        date          = LocalDate.now(),
        jumuahEnabled = true,
        madhab        = Madhab.STANDARD,
        scheduledTime = LocalTime.of(15, 20),
        verse = VerseInfo(
            text = "Maintain with care the [obligatory] prayers and [in particular] the middle [i.e., ʿaṣr] prayer and stand before Allāh, devoutly obedient.",
            citation = "Al-Baqarah 2:238",
        ),
        snoozeMinutes = 10,
        onMark   = {},
        onSnooze = {},
        onDismiss = {},
    )
}
