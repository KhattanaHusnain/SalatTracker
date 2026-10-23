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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.*
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.StreakInfo
import com.salattracker.domain.model.VerseInfo
import com.salattracker.domain.usecase.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ── Home Screen ───────────────────────────────────────────────────────────────
// A today-focused dashboard: a next-prayer hero card (which also surfaces pending Qaza), a
// compact timeline of today's five prayers, then a consistency card (streak + 7-day history),
// then the verse card. Stats owns the deeper analytics and Goals owns goal management — this
// screen never duplicates a trend chart, a breakdown, or goal setup, only "where do I stand
// right now".

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onPrayerCardClick: (PrayerType) -> Unit,
    onSheetDismiss: () -> Unit,
    onStatusSave: (PrayerStatus) -> Unit,
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
                    qazaPendingTotal = uiState.qazaPendingTotal,
                    jumuahEnabled    = uiState.jumuahEnabled,
                    madhab           = uiState.madhab,
                    onQazaClick      = onQazaClick,
                    modifier         = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                )

                // Order: motivation (streak + history) first, then today's action, then the
                // verse as a closing note. Pending Qaza lives in the hero now, next to the
                // next-prayer countdown, since it's the other "where do I stand right now" fact.
                ConsistencyCard(
                    perfectStreak = remember(uiState.streaks) { perfectDayStreak(uiState.streaks) },
                    streaks       = uiState.streaks,
                    todayDate     = uiState.todayDate,
                    lastSevenDays = uiState.lastSevenDays,
                    onDayClick    = onDayClick,
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                )

                SectionTitle(
                    text     = stringResource(R.string.home_daily_stack),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp).padding(top = 16.dp),
                )
                PrayerTimeline(
                    todayDate     = uiState.todayDate,
                    jumuahEnabled = uiState.jumuahEnabled,
                    madhab        = uiState.madhab,
                    prayers       = uiState.prayers,
                    currentPrayer = uiState.currentPrayer,
                    onClick       = onPrayerCardClick,
                    modifier      = Modifier.fillMaxWidth(),
                )

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
    }

    uiState.sheetArgs?.let { args ->
        PrayerStatusBottomSheet(
            args      = args,
            isSaving  = uiState.isSaving,
            onDismiss = onSheetDismiss,
            onSave    = onStatusSave,
            gender    = uiState.gender,
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
    qazaPendingTotal: Int,
    jumuahEnabled: Boolean,
    madhab: Madhab,
    onQazaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hijri = remember(todayDate) { todayDate.toHijriOrNull() }
    val islamicEvent = hijri?.islamicEventOrNull()
    val hijriText = hijri?.fullLabel()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, d MMM") }
    val use24HourFormat = LocalUse24HourFormat.current
    val completed = remember(prayers) { completedCount(prayers.map { it.status }) }

    val sweep = remember { Animatable(0f) }
    LaunchedEffect(todayDate) { sweep.animateTo(1f, animationSpec = tween(700)) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(SalatHeroGradientStart, SalatHeroGradientEnd)))
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = if (hijriText != null) "${todayDate.format(dateFormatter)} · $hijriText" else todayDate.format(dateFormatter),
                color = SalatOnHero.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontFamily = AppUiFontFamily,
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
                        emptyColor     = SalatOnHero.copy(alpha = 0.22f),
                        strokeFraction = 0.14f,
                        sweepFraction  = sweep.value,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.home_ring_count, completed),
                            color = SalatOnHero,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppTitleFontFamily,
                        )
                        Text(
                            text = stringResource(R.string.home_ring_caption),
                            color = SalatOnHero.copy(alpha = 0.75f),
                             fontSize = 12.sp,
                            fontFamily = AppUiFontFamily,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_next_prayer).uppercase(),
                            color = SalatOnHero.copy(alpha = 0.7f),
                         fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AppUiFontFamily,
                        letterSpacing = 1.sp,
                    )
                    if (nextPrayer != null) {
                        val nextPrayerDate = if (nextPrayer.isTomorrow) todayDate.plusDays(1) else todayDate
                        Text(
                            text = nextPrayer.type.label(nextPrayerDate, jumuahEnabled, madhab),
                            color = SalatOnHero,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppTitleFontFamily,
                        )
                        Text(
                            text = if (nextPrayer.isTomorrow)
                                "${stringResource(R.string.home_tomorrow)} ${nextPrayer.time.formatAppTime(use24HourFormat)}"
                            else
                                nextPrayer.time.formatAppTime(use24HourFormat),
                            color = SalatOnHero.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontFamily = AppUiFontFamily,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (minutesUntilNext >= 60)
                                stringResource(R.string.home_next_in_hours_minutes, (minutesUntilNext / 60).toInt(), (minutesUntilNext % 60).toInt())
                            else
                                stringResource(R.string.home_next_in_minutes, minutesUntilNext.toInt()),
                            color = SalatOnHero,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppUiFontFamily,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.home_day_complete),
                            color = SalatOnHero,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppTitleFontFamily,
                        )
                    }
                    if (qazaPendingTotal > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SalatOnHero.copy(alpha = 0.25f)))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onQazaClick)
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(painterResource(R.drawable.ic_history_edu), null, tint = SalatOnHero, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.home_qaza_count_pending, qazaPendingTotal),
                                color = SalatOnHero,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = AppUiFontFamily,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = SalatOnHero.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
            if (islamicEvent != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SalatOnHero.copy(alpha = 0.18f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_mosque), null, tint = SalatOnHero, modifier = Modifier.size(16.dp))
                    Text(islamicEvent.label(), color = SalatOnHero, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
                }
            }
        }
    }
}

// ── Prayer timeline ────────────────────────────────────────────────────────────

@Composable
private fun PrayerTimeline(
    todayDate: LocalDate,
    jumuahEnabled: Boolean,
    madhab: Madhab,
    prayers: List<PrayerEntryUi>,
    currentPrayer: PrayerType?,
    onClick: (PrayerType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        prayers.forEachIndexed { index, entry ->
            TimelineRow(
                entry     = entry,
                date      = todayDate,
                jumuahEnabled = jumuahEnabled,
                madhab    = madhab,
                isLast    = index == prayers.lastIndex,
                // Only call attention to the current prayer while it still needs marking — once
                // it's recorded, highlighting it again would just be noise next to the hero's
                // "next prayer" countdown.
                needsMarking = entry.type == currentPrayer && entry.isMarkable && entry.status == PrayerStatus.NOT_RECORDED,
                onClick   = { onClick(entry.type) },
            )
        }
    }
}

@Composable
private fun TimelineRow(
    entry: PrayerEntryUi,
    date: LocalDate,
    jumuahEnabled: Boolean,
    madhab: Madhab,
    isLast: Boolean,
    needsMarking: Boolean,
    onClick: () -> Unit,
) {
    val use24HourFormat = LocalUse24HourFormat.current

    Surface(
        onClick  = onClick,
        enabled  = entry.isMarkable,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = if (needsMarking) SalatPrimaryFixed.copy(alpha = 0.18f) else Color.Transparent,
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
                        .size(if (needsMarking) 14.dp else 10.dp)
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
                        text = entry.type.label(date, jumuahEnabled, madhab),
                        color = SalatPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AppTitleFontFamily,
                    )
                    if (needsMarking) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(SalatPrimary).padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(stringResource(R.string.home_now), color = SalatOnPrimary,  fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily, maxLines = 1)
                        }
                    }
                }
                if (entry.time != null) {
                    Text(entry.time.formatAppTime(use24HourFormat), color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily)
                }
                if (entry.reminderTime != null) {
                    Text(
                        text = stringResource(R.string.home_reminder_at, entry.reminderTime.formatAppTime(use24HourFormat)),
                        color = SalatPrimary,
                         fontSize = 12.sp,
                        fontFamily = AppUiFontFamily,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrayerStatusChip(status = entry.status, isUpcoming = !entry.isMarkable)
                if (entry.isMarkable) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = SalatOnSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp).size(16.dp),
                    )
                }
            }
        }
    }
}

// ── Section title ─────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = SalatPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = AppTitleFontFamily,
    )
}

// ── Consistency card (streak + last 7 days) ───────────────────────────────────

@Composable
private fun ConsistencyCard(
    perfectStreak: Int,
    streaks: Map<PrayerType, StreakInfo>,
    todayDate: LocalDate,
    lastSevenDays: Map<LocalDate, Map<PrayerType, PrayerStatus>>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                if (perfectStreak > 0) {
                    Column {
                        Text("$perfectStreak", color = SalatPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily)
                        Text(stringResource(R.string.home_streak_caption), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
                    }
                } else {
                    Text(stringResource(R.string.home_streak_start), color = SalatPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PrayerType.values().forEach { type ->
                    StreakChip(type = type, info = streaks[type] ?: StreakInfo(0, 0), modifier = Modifier.weight(1f))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = SalatOutlineVariant.copy(alpha = 0.6f))

            Text(
                text = stringResource(R.string.home_last_7_days),
                color = SalatOnSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppUiFontFamily,
            )
            Spacer(modifier = Modifier.height(10.dp))
            WeekRings(todayDate = todayDate, lastSevenDays = lastSevenDays, onDayClick = onDayClick)
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
        Text(type.shortLabel(), color = SalatOnSurfaceVariant,  fontSize = 12.sp, fontFamily = AppUiFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(2.dp))
        Text("${info.currentStreak}", color = SalatPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily)
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth(0.8f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(SalatSurfaceVariant)) {
            Box(modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(SalatPrimary))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.home_best_streak, info.highestStreak),
            color = SalatOnSurfaceVariant,
             fontSize = 12.sp,
            fontFamily = AppUiFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WeekRings(
    todayDate: LocalDate,
    lastSevenDays: Map<LocalDate, Map<PrayerType, PrayerStatus>>,
    onDayClick: (LocalDate) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (offset in 6 downTo 0) {
            val date = todayDate.minusDays(offset.toLong())
            val isToday = date == todayDate
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onDayClick(date) }
                    .padding(vertical = 4.dp),
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
                    color = if (isToday) SalatPrimary else SalatOnSurfaceVariant,
                     fontSize = 12.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = AppUiFontFamily,
                    maxLines = 1,
                )
            }
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
                    listOf(SalatHeroGradientStart, SalatHeroGradientEnd)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column {
            Text(
                text = "“${verse.text}”",
                color = SalatOnHero,
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = AppUiFontFamily,
                lineHeight = 22.sp,
            )
            if (verse.citation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = verse.citation,
                color = SalatOnHero.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontFamily = AppUiFontFamily,
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
        ),
        onPrayerCardClick = {},
        onSheetDismiss    = {},
        onStatusSave      = {},
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
        ),
        onPrayerCardClick = {},
        onSheetDismiss    = {},
        onStatusSave      = {},
        onQazaClick       = {},
        onDayClick        = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenQazaPendingPreview() {
    val today = LocalDate.now()
    HomeScreen(
        uiState = HomeUiState(
            isLoading        = false,
            todayDate        = today,
            prayers          = samplePrayers(
                statuses = listOf(PrayerStatus.NOT_RECORDED, PrayerStatus.NOT_RECORDED, PrayerStatus.NOT_RECORDED, PrayerStatus.NOT_RECORDED, PrayerStatus.NOT_RECORDED),
                markable = listOf(true, true, true, false, false),
            ),
            nextPrayer       = NextPrayer(PrayerType.MAGHRIB, LocalTime.of(17, 32), isTomorrow = false),
            minutesUntilNext = 5,
            currentPrayer    = PrayerType.ASR,
            streaks          = sampleStreaks(listOf(0, 0, 0, 0, 0), listOf(42, 20, 15, 8, 30)),
            lastSevenDays    = (0..6).associate { offset ->
                today.minusDays(offset.toLong()) to PrayerType.values().associateWith { PrayerStatus.QAZA_PENDING }
            },
            qazaPendingTotal = 456,
            verse            = null,
        ),
        onPrayerCardClick = {},
        onSheetDismiss    = {},
        onStatusSave      = {},
        onQazaClick       = {},
        onDayClick        = {},
    )
}
