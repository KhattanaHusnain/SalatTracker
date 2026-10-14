package com.salattracker.feature.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.*
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.StreakInfo
import com.salattracker.domain.model.VerseInfo
import com.salattracker.domain.usecase.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ── Home Screen ───────────────────────────────────────────────────────────────
// A today-focused dashboard: a next-prayer hero card, a compact timeline of today's five
// prayers, a single "perfect day" streak with per-prayer detail, a 7-day history strip, two
// stat tiles (Qaza / Goals), then the verse card. Stats owns the deeper analytics — this screen
// never duplicates a trend chart or a breakdown, only "where do I stand right now".

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onPrayerCardClick: (PrayerType) -> Unit,
    onSheetDismiss: () -> Unit,
    onStatusSave: (PrayerStatus) -> Unit,
    onGoalsClick: () -> Unit,
    onAddGoalClick: () -> Unit,
    onQazaClick: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)   // room for bottom nav
        ) {
            if (uiState.isLoading) {
                HomeSkeleton(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            } else {
                HeroCard(
                    todayDate        = uiState.todayDate,
                    prayers          = uiState.prayers,
                    nextPrayer       = uiState.nextPrayer,
                    minutesUntilNext = uiState.minutesUntilNext,
                    modifier         = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                )

                Text(
                    text = stringResource(R.string.home_daily_stack),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    color = SalatPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = MontserratFamily,
                )
                Spacer(modifier = Modifier.height(4.dp))

                PrayerTimeline(
                    prayers       = uiState.prayers,
                    currentPrayer = uiState.currentPrayer,
                    onClick       = onPrayerCardClick,
                    modifier      = Modifier.fillMaxWidth(),
                )

                StreakCard(
                    perfectStreak = remember(uiState.streaks) { perfectDayStreak(uiState.streaks) },
                    streaks       = uiState.streaks,
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                )

                WeekStrip(
                    todayDate      = uiState.todayDate,
                    lastSevenDays  = uiState.lastSevenDays,
                    onDayClick     = onDayClick,
                    modifier       = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QazaTile(
                        pendingTotal = uiState.qazaPendingTotal,
                        onClick      = onQazaClick,
                        modifier     = Modifier.weight(1f),
                    )
                    GoalsTile(
                        percent  = uiState.goalsCompletionPercent,
                        onClick  = if (uiState.goalsCompletionPercent != null) onGoalsClick else onAddGoalClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            uiState.verse?.let { verse ->
                InspirationBanner(
                    verse = verse,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 24.dp)
                )
            }
        }
    }

    uiState.sheetArgs?.let { args ->
        PrayerStatusBottomSheet(
            args      = args,
            isSaving  = uiState.isSaving,
            onDismiss = onSheetDismiss,
            onSave    = onStatusSave,
        )
    }
}

// ── Hero card ─────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(
    todayDate: LocalDate,
    prayers: List<PrayerEntryUi>,
    nextPrayer: NextPrayer?,
    minutesUntilNext: Long,
    modifier: Modifier = Modifier,
) {
    val hijri = remember(todayDate) { todayDate.toHijriOrNull() }
    val islamicEvent = hijri?.islamicEventOrNull()
    val hijriText = hijri?.fullLabel()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, d MMM") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }
    val completed = remember(prayers) { completedCount(prayers.map { it.status }) }

    val sweep = remember { Animatable(0f) }
    LaunchedEffect(todayDate) { sweep.animateTo(1f, animationSpec = tween(700)) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(SalatPrimaryContainer, SalatPrimary)))
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = if (hijriText != null) "${todayDate.format(dateFormatter)} · $hijriText" else todayDate.format(dateFormatter),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontFamily = InterFamily,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    PrayerStatusRing(
                        statuses       = prayers.associate { it.type to it.status },
                        modifier       = Modifier.fillMaxSize(),
                        emptyColor     = Color.White.copy(alpha = 0.22f),
                        strokeFraction = 0.14f,
                        sweepFraction  = sweep.value,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.home_ring_count, completed),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MontserratFamily,
                        )
                        Text(
                            text = stringResource(R.string.home_ring_caption),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 9.sp,
                            fontFamily = InterFamily,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_next_prayer).uppercase(),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFamily,
                        letterSpacing = 1.sp,
                    )
                    if (nextPrayer != null) {
                        Text(
                            text = nextPrayer.type.label(),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MontserratFamily,
                        )
                        Text(
                            text = if (nextPrayer.isTomorrow)
                                "${stringResource(R.string.home_tomorrow)} ${nextPrayer.time.format(timeFormatter)}"
                            else
                                nextPrayer.time.format(timeFormatter),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontFamily = InterFamily,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (minutesUntilNext >= 60)
                                stringResource(R.string.home_next_in_hours_minutes, (minutesUntilNext / 60).toInt(), (minutesUntilNext % 60).toInt())
                            else
                                stringResource(R.string.home_next_in_minutes, minutesUntilNext.toInt()),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFamily,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.home_day_complete),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = MontserratFamily,
                        )
                    }
                }
            }
            if (islamicEvent != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_mosque), null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(islamicEvent.label(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily)
                }
            }
        }
    }
}

// ── Prayer timeline ────────────────────────────────────────────────────────────

@Composable
private fun PrayerTimeline(
    prayers: List<PrayerEntryUi>,
    currentPrayer: PrayerType?,
    onClick: (PrayerType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        prayers.forEachIndexed { index, entry ->
            TimelineRow(
                entry     = entry,
                isLast    = index == prayers.lastIndex,
                isCurrent = entry.type == currentPrayer,
                onClick   = { onClick(entry.type) },
            )
        }
    }
}

@Composable
private fun TimelineRow(
    entry: PrayerEntryUi,
    isLast: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }

    Surface(
        onClick  = onClick,
        enabled  = entry.isMarkable,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = if (isCurrent) SalatPrimaryFixed.copy(alpha = 0.18f) else Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .alpha(if (entry.isMarkable) 1f else 0.6f),
            verticalAlignment = Alignment.Top,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(18.dp)) {
                Box(
                    modifier = Modifier
                        .size(if (isCurrent) 14.dp else 10.dp)
                        .clip(CircleShape)
                        .background(statusRingColor(entry.status)),
                )
                if (!isLast) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.width(2.dp).height(28.dp).background(SalatOutlineVariant))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = entry.type.label(),
                        color = SalatPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = MontserratFamily,
                    )
                    if (isCurrent) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(SalatPrimary).padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(stringResource(R.string.home_now), color = SalatOnPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFamily)
                        }
                    }
                }
                if (entry.time != null) {
                    Text(entry.time.format(timeFormatter), color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = InterFamily)
                }
                if (entry.reminderTime != null) {
                    Text(
                        text = stringResource(R.string.home_reminder_at, entry.reminderTime.format(timeFormatter)),
                        color = SalatPrimary,
                        fontSize = 11.sp,
                        fontFamily = InterFamily,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            PrayerStatusChip(status = entry.status, isUpcoming = !entry.isMarkable)
        }
    }
}

// ── Streak card ────────────────────────────────────────────────────────────────

@Composable
private fun StreakCard(perfectStreak: Int, streaks: Map<PrayerType, StreakInfo>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        color    = SalatSurface,
        border   = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(painterResource(R.drawable.ic_local_fire_department), null, tint = Color(0xFFFF9939), modifier = Modifier.size(28.dp))
                Column {
                    Text("$perfectStreak", color = SalatPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = MontserratFamily)
                    Text(stringResource(R.string.home_streak_caption), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = InterFamily)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrayerType.values().forEach { type ->
                    StreakChip(type = type, info = streaks[type] ?: StreakInfo(0, 0), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StreakChip(type: PrayerType, info: StreakInfo, modifier: Modifier = Modifier) {
    val fraction = if (info.highestStreak > 0) (info.currentStreak.toFloat() / info.highestStreak).coerceIn(0f, 1f) else 0f
    Column(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(SalatSurfaceContainer).padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(type.shortLabel(), color = SalatOnSurfaceVariant, fontSize = 10.sp, fontFamily = InterFamily, maxLines = 1)
        Spacer(modifier = Modifier.height(2.dp))
        Text("${info.currentStreak}", color = SalatPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = MontserratFamily)
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth(0.8f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(SalatSurfaceVariant)) {
            Box(modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(SalatPrimary))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(stringResource(R.string.home_best_streak, info.highestStreak), color = SalatOutline, fontSize = 8.sp, fontFamily = InterFamily)
    }
}

// ── Last 7 days strip ─────────────────────────────────────────────────────────

@Composable
private fun WeekStrip(
    todayDate: LocalDate,
    lastSevenDays: Map<LocalDate, Map<PrayerType, PrayerStatus>>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.home_last_7_days),
            color = SalatPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MontserratFamily,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (offset in 6 downTo 0) {
                val date = todayDate.minusDays(offset.toLong())
                val isToday = date == todayDate
                Column(
                    modifier = Modifier.weight(1f).clickable { onDayClick(date) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .let { if (isToday) it.border(1.5.dp, SalatPrimary, CircleShape) else it }
                            .padding(3.dp),
                    ) {
                        PrayerStatusRing(statuses = lastSevenDays[date].orEmpty(), modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(date.dayOfWeek.shortLabelRes),
                        color = if (isToday) SalatPrimary else SalatOutline,
                        fontSize = 9.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = InterFamily,
                    )
                }
            }
        }
    }
}

// ── Stat tiles (Qaza / Goals) ──────────────────────────────────────────────────

@Composable
private fun QazaTile(pendingTotal: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(14.dp), color = SalatSurfaceContainer, border = BorderStroke(1.dp, SalatOutlineVariant)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(if (pendingTotal > 0) SalatTertiaryContainer else SalatPrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (pendingTotal > 0) R.drawable.ic_history_edu else R.drawable.ic_check_circle_filled),
                        contentDescription = null,
                        tint = if (pendingTotal > 0) SalatOnTertiaryContainer else SalatOnPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Icon(painterResource(R.drawable.ic_chevron_right), null, tint = SalatOnSurfaceVariant, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (pendingTotal > 0) {
                Text("$pendingTotal", color = SalatPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = MontserratFamily)
            } else {
                Text(stringResource(R.string.home_qaza_none), color = SalatPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily, maxLines = 2)
            }
            Text(stringResource(R.string.home_qaza_pending), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
        }
    }
}

@Composable
private fun GoalsTile(percent: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(14.dp), color = SalatSurfaceContainer, border = BorderStroke(1.dp, SalatOutlineVariant)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(SalatPrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (percent != null) R.drawable.ic_check_circle_filled else R.drawable.ic_add),
                        contentDescription = null,
                        tint = SalatOnPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
                if (percent != null) {
                    Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = SalatSurfaceVariant, strokeWidth = 2.5.dp)
                        CircularProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxSize(), color = SalatPrimary, strokeWidth = 2.5.dp)
                    }
                } else {
                    Icon(painterResource(R.drawable.ic_chevron_right), null, tint = SalatOnSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (percent != null) {
                Text(stringResource(R.string.home_percent_completed, percent), color = SalatPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily, maxLines = 1)
            } else {
                Text(stringResource(R.string.home_set_first_goal), color = SalatPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily, maxLines = 2)
            }
            Text(stringResource(R.string.home_daily_goals), color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily)
        }
    }
}

// ── Inspiration banner ────────────────────────────────────────────────────────

@Composable
private fun InspirationBanner(verse: VerseInfo, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(SalatPrimaryContainer, SalatPrimary)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column {
            Text(
                text = "“${verse.text}”",
                color = Color.White,
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = InterFamily,
                lineHeight = 22.sp,
            )
            if (verse.citation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = verse.citation,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontFamily = InterFamily,
                )
            }
        }
    }
}

// ── Skeleton placeholder ──────────────────────────────────────────────────────

@Composable
private fun ShimmerBlock(modifier: Modifier = Modifier, height: Dp = 76.dp) {
    val shimmerAlpha by rememberInfiniteTransition(label = "shimmer")
        .animateFloat(
            initialValue = 0.3f, targetValue = 0.7f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "alpha",
        )
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(SalatSurfaceContainerHigh.copy(alpha = shimmerAlpha))
    )
}

@Composable
private fun HomeSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ShimmerBlock(Modifier.fillMaxWidth(), height = 160.dp)
        repeat(5) { ShimmerBlock(Modifier.fillMaxWidth(), height = 64.dp) }
        ShimmerBlock(Modifier.fillMaxWidth(), height = 120.dp)
        ShimmerBlock(Modifier.fillMaxWidth(), height = 90.dp)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Reuses Calendar's weekday strings rather than java.time's Locale-based day names, which
 * read the system locale in a way Compose can't observe for recomposition on locale change. */
private val DayOfWeek.shortLabelRes: Int get() = when (this) {
    DayOfWeek.SUNDAY    -> R.string.weekday_sun
    DayOfWeek.MONDAY    -> R.string.weekday_mon
    DayOfWeek.TUESDAY   -> R.string.weekday_tue
    DayOfWeek.WEDNESDAY -> R.string.weekday_wed
    DayOfWeek.THURSDAY  -> R.string.weekday_thu
    DayOfWeek.FRIDAY    -> R.string.weekday_fri
    DayOfWeek.SATURDAY  -> R.string.weekday_sat
}

// ── Preview ───────────────────────────────────────────────────────────────────

private fun samplePrayers(statuses: List<PrayerStatus>, markable: List<Boolean>): List<PrayerEntryUi> =
    PrayerType.values().mapIndexed { i, type ->
        PrayerEntryUi(
            type         = type,
            time         = LocalTime.of((5 + i * 3) % 24, 30),
            reminderTime = if (i == 1) LocalTime.of((5 + i * 3) % 24, 25) else null,
            status       = statuses[i],
            isMarkable   = markable[i],
        )
    }

private fun sampleStreaks(values: List<Int>, best: List<Int>): Map<PrayerType, StreakInfo> =
    PrayerType.values().mapIndexed { i, type -> type to StreakInfo(currentStreak = values[i], highestStreak = best[i]) }.toMap()

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenMiddayPreview() {
    val today = LocalDate.now()
    HomeScreen(
        uiState = HomeUiState(
            isLoading        = false,
            todayDate        = today,
            prayers          = samplePrayers(
                statuses = listOf(PrayerStatus.CONGREGATION, PrayerStatus.INDIVIDUAL, PrayerStatus.NOT_RECORDED, PrayerStatus.NOT_RECORDED, PrayerStatus.NOT_RECORDED),
                markable = listOf(true, true, true, false, false),
            ),
            nextPrayer       = NextPrayer(PrayerType.ASR, LocalTime.of(15, 45), isTomorrow = false),
            minutesUntilNext = 84,
            currentPrayer    = PrayerType.DHUHR,
            streaks          = sampleStreaks(listOf(7, 12, 3, 0, 5), listOf(42, 20, 15, 8, 30)),
            lastSevenDays    = (0..6).associate { offset ->
                today.minusDays(offset.toLong()) to PrayerType.values().associateWith { PrayerStatus.CONGREGATION }
            },
            qazaPendingTotal = 3,
            verse = VerseInfo(
                text = "Verily, in the remembrance of Allah do hearts find rest.",
                citation = "Ar-Ra'd 13:28",
            ),
            goalsCompletionPercent = 68,
        ),
        onPrayerCardClick = {},
        onSheetDismiss    = {},
        onStatusSave      = {},
        onGoalsClick      = {},
        onAddGoalClick    = {},
        onQazaClick       = {},
        onDayClick        = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenAfterIshaPreview() {
    val today = LocalDate.now()
    HomeScreen(
        uiState = HomeUiState(
            isLoading        = false,
            todayDate        = today,
            prayers          = samplePrayers(
                statuses = listOf(PrayerStatus.CONGREGATION, PrayerStatus.CONGREGATION, PrayerStatus.INDIVIDUAL, PrayerStatus.QAZA_PENDING, PrayerStatus.CONGREGATION),
                markable = listOf(true, true, true, true, true),
            ),
            nextPrayer       = NextPrayer(PrayerType.FAJR, LocalTime.of(5, 31), isTomorrow = true),
            minutesUntilNext = 345,
            currentPrayer    = PrayerType.ISHA,
            streaks          = sampleStreaks(listOf(0, 30, 15, 0, 22), listOf(42, 30, 15, 8, 30)),
            lastSevenDays    = (0..6).associate { offset ->
                today.minusDays(offset.toLong()) to PrayerType.values().associateWith { PrayerStatus.CONGREGATION }
            },
            qazaPendingTotal = 0,
            verse = null,
            goalsCompletionPercent = null,
        ),
        onPrayerCardClick = {},
        onSheetDismiss    = {},
        onStatusSave      = {},
        onGoalsClick      = {},
        onAddGoalClick    = {},
        onQazaClick       = {},
        onDayClick        = {},
    )
}
