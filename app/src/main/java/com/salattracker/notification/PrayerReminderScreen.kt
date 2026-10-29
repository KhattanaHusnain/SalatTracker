package com.salattracker.notification

import android.animation.ValueAnimator
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.AppTitleFontFamily
import com.salattracker.core.theme.AmiriQuranFamily
import com.salattracker.core.theme.SalatHeroGradientEnd
import com.salattracker.core.theme.SalatHeroGradientStart
import com.salattracker.core.theme.SalatOnHero
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatPrimaryContainer
import com.salattracker.core.theme.SalatPrimaryFixed
import com.salattracker.core.theme.SalatPrimaryFixedDim
import com.salattracker.core.ui.label
import com.salattracker.core.ui.formatAppTime
import com.salattracker.core.ui.iconRes
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.VerseInfo
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun PrayerReminderScreen(
    prayerType: PrayerType,
    date: LocalDate,
    jumuahEnabled: Boolean,
    madhab: Madhab,
    gender: Gender?,
    scheduledTime: LocalTime?,
    verse: VerseInfo?,
    snoozeMinutes: Int,
    use24HourFormat: Boolean,
    onMark: (PrayerStatus) -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1000)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("prayer-reminder-screen")
            .background(
                Brush.verticalGradient(listOf(SalatHeroGradientStart, SalatHeroGradientEnd, Color.Black))
            ),
    ) {
        val veryCompact = maxHeight < 620.dp
        val compact = maxHeight < 760.dp
        val screenPadding = when {
            veryCompact -> 12.dp
            compact -> 16.dp
            else -> 24.dp
        }
        val topSpace = when {
            veryCompact -> 0.dp
            compact -> 8.dp
            else -> 24.dp
        }
        val clockSize = when {
            veryCompact -> 42.sp
            compact -> 52.sp
            else -> 64.sp
        }
        val titleSize = when {
            veryCompact -> 15.sp
            compact -> 18.sp
            else -> 20.sp
        }
        val titleSpacing = when {
            veryCompact -> 1.5.sp
            compact -> 2.sp
            else -> 3.sp
        }
        val scheduledSize = when {
            veryCompact -> 11.sp
            compact -> 13.sp
            else -> 14.sp
        }
        val logoSize = when {
            veryCompact -> 84.dp
            compact -> 128.dp
            else -> 180.dp
        }
        val logoIconSize = when {
            veryCompact -> 40.dp
            compact -> 60.dp
            else -> 88.dp
        }
        val verseTopSpace = when {
            veryCompact -> 8.dp
            compact -> 16.dp
            else -> 32.dp
        }
        val arabicSize = when {
            veryCompact -> 18.sp
            compact -> 21.sp
            else -> 24.sp
        }
        val translationSize = when {
            veryCompact -> 12.sp
            compact -> 14.sp
            else -> 16.sp
        }
        val translationLineHeight = when {
            veryCompact -> 18.sp
            compact -> 21.sp
            else -> 24.sp
        }
        val citationSize = when {
            veryCompact -> 10.sp
            compact -> 11.sp
            else -> 12.sp
        }
        val actionGap = when {
            veryCompact -> 8.dp
            compact -> 12.dp
            else -> 16.dp
        }
        val dismissGap = when {
            veryCompact -> 4.dp
            compact -> 12.dp
            else -> 24.dp
        }
        val centerScrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(topSpace))

            // ── Clock + prayer info ──────────────────────────────────────────
            Text(
                text       = now.formatAppTime(use24HourFormat),
                color      = SalatPrimaryFixed,
                fontSize   = clockSize,
                fontWeight = FontWeight.Bold,
                fontFamily = AppTitleFontFamily,
                modifier   = Modifier.testTag("prayer-reminder-clock"),
            )
            Spacer(modifier = Modifier.height(if (veryCompact) 4.dp else 8.dp))
            Text(
                text          = stringResource(R.string.reminder_prayer_title, prayerType.label(date, jumuahEnabled, madhab)).uppercase(),
                color         = SalatOnHero,
                fontSize      = titleSize,
                fontWeight    = FontWeight.SemiBold,
                fontFamily    = AppTitleFontFamily,
                letterSpacing = titleSpacing,
            )
            if (scheduledTime != null) {
                Text(
                    text     = stringResource(R.string.reminder_scheduled_for, scheduledTime.formatAppTime(use24HourFormat)),
                    color    = SalatPrimaryFixedDim.copy(alpha = 0.8f),
                    fontSize = scheduledSize,
                    fontFamily = AppUiFontFamily,
                )
            }

            // ── Pulsing logo + verse ──────────────────────────────────────────
            val scale = if (ValueAnimator.areAnimatorsEnabled()) {
                val infiniteTransition = rememberInfiniteTransition(label = "breathe")
                val animated by infiniteTransition.animateFloat(
                    initialValue  = 1f,
                    targetValue   = 1.1f,
                    animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
                    label         = "scale",
                )
                animated
            } else {
                1f
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(centerScrollState)
                        .testTag("prayer-reminder-center-scroll"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(if (veryCompact) 4.dp else 8.dp))
                    Box(
                        modifier = Modifier
                            .size(logoSize)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter  = painterResource(prayerType.iconRes),
                            contentDescription = null,
                            tint     = SalatOnHero,
                            modifier = Modifier.size(logoIconSize),
                        )
                    }

                    if (verse != null) {
                        Spacer(modifier = Modifier.height(verseTopSpace))
                        if (verse.arabic.isNotBlank()) {
                            Text(
                                text = verse.arabic,
                                color = SalatOnHero.copy(alpha = 0.9f),
                                fontSize = arabicSize,
                                fontFamily = AmiriQuranFamily,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("prayer-reminder-verse-arabic"),
                            )
                            Spacer(modifier = Modifier.height(if (veryCompact) 6.dp else if (compact) 10.dp else 16.dp))
                        }
                        Text(
                            text       = "“${verse.text}”",
                            color      = SalatOnHero.copy(alpha = 0.9f),
                            fontSize   = translationSize,
                            fontFamily = AppUiFontFamily,
                            textAlign  = TextAlign.Center,
                            lineHeight = translationLineHeight,
                            modifier   = Modifier
                                .padding(horizontal = 8.dp)
                                .testTag("prayer-reminder-verse-translation"),
                        )
                        if (verse.citation.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(if (veryCompact) 4.dp else if (compact) 6.dp else 8.dp))
                            Text(
                                text       = verse.citation,
                                color      = SalatPrimaryFixedDim.copy(alpha = 0.6f),
                                fontSize   = citationSize,
                                fontFamily = AppUiFontFamily,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(if (veryCompact) 4.dp else 8.dp))
                }
            }

            // ── Action grid ───────────────────────────────────────────────────
            // Women aren't obliged to attend Jumu'ah/congregation, so that tile is dropped
            // entirely rather than just left tappable — Individual takes the full row instead.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(actionGap),
            ) {
                if (gender != Gender.FEMALE) {
                    ReminderActionTile(
                        iconRes  = R.drawable.ic_groups,
                        label    = stringResource(R.string.status_congregation),
                        modifier = Modifier.weight(1f),
                        testTag  = "prayer-reminder-action-congregation",
                        compact  = compact,
                        veryCompact = veryCompact,
                        onClick  = { onMark(PrayerStatus.CONGREGATION) },
                    )
                }
                ReminderActionTile(
                    iconRes  = R.drawable.ic_person,
                    label    = stringResource(R.string.status_individual),
                    modifier = Modifier.weight(1f),
                    testTag  = "prayer-reminder-action-individual",
                    compact  = compact,
                    veryCompact = veryCompact,
                    onClick  = { onMark(PrayerStatus.INDIVIDUAL) },
                )
            }
            Spacer(modifier = Modifier.height(actionGap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(actionGap),
            ) {
                ReminderActionTile(
                    iconRes  = R.drawable.ic_pending_actions,
                    label    = stringResource(R.string.status_qaza_pending),
                    modifier = Modifier.weight(1f),
                    testTag  = "prayer-reminder-action-qaza",
                    compact  = compact,
                    veryCompact = veryCompact,
                    onClick  = { onMark(PrayerStatus.QAZA_PENDING) },
                )
                ReminderActionTile(
                    iconRes  = R.drawable.ic_schedule,
                    label    = stringResource(R.string.reminder_snooze, snoozeMinutes),
                    modifier = Modifier.weight(1f),
                    testTag  = "prayer-reminder-action-snooze",
                    compact  = compact,
                    veryCompact = veryCompact,
                    onClick  = onSnooze,
                )
            }

            Spacer(modifier = Modifier.height(dismissGap))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("prayer-reminder-dismiss"),
            ) {
                Icon(
                    painter  = painterResource(R.drawable.ic_close),
                    contentDescription = null,
                    tint     = SalatOnHero.copy(alpha = 0.6f),
                    modifier = Modifier.size(if (compact) 14.dp else 16.dp),
                )
                Spacer(modifier = Modifier.width(if (compact) 6.dp else 8.dp))
                Text(
                    text          = stringResource(R.string.reminder_dismiss),
                    color         = SalatOnHero.copy(alpha = 0.6f),
                    fontSize      = if (veryCompact) 10.sp else if (compact) 11.sp else 12.sp,
                    fontFamily    = AppUiFontFamily,
                    letterSpacing = if (veryCompact) 1.sp else if (compact) 1.5.sp else 2.sp,
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
    testTag: String,
    compact: Boolean,
    veryCompact: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick  = onClick,
        modifier = modifier.testTag(testTag),
        shape    = RoundedCornerShape(if (veryCompact) 12.dp else if (compact) 14.dp else 16.dp),
        color    = Color.White.copy(alpha = 0.1f),
        border   = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (veryCompact) 8.dp else if (compact) 12.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter  = painterResource(iconRes),
                contentDescription = null,
                tint     = SalatPrimaryFixed,
                modifier = Modifier.size(if (veryCompact) 22.dp else if (compact) 24.dp else 28.dp),
            )
            Spacer(modifier = Modifier.height(if (veryCompact) 4.dp else if (compact) 6.dp else 8.dp))
            Text(
                text       = label,
                color      = SalatOnHero,
                fontSize   = if (veryCompact) 10.sp else if (compact) 11.sp else 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppUiFontFamily,
                maxLines   = 1,
                softWrap   = false,
                textAlign  = TextAlign.Center,
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
        gender        = null,
        scheduledTime = LocalTime.of(15, 20),
        verse = VerseInfo(
            text = "Maintain with care the [obligatory] prayers and [in particular] the middle [i.e., ʿaṣr] prayer and stand before Allāh, devoutly obedient.",
            citation = "Al-Baqarah 2:238",
        ),
        snoozeMinutes = 10,
        use24HourFormat = false,
        onMark   = {},
        onSnooze = {},
        onDismiss = {},
    )
}
