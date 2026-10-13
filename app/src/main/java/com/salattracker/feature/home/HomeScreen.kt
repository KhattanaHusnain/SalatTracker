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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.PrayerStatusBottomSheet
import com.salattracker.core.ui.label
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.StreakInfo
import com.salattracker.domain.model.VerseInfo
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ── Home Screen ───────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onPrayerCardClick: (PrayerType) -> Unit,
    onSheetDismiss: () -> Unit,
    onStatusSave: (PrayerStatus) -> Unit,
    onGoalsClick: () -> Unit,
    onAddGoalClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)   // room for bottom nav
        ) {
            // ── Daily goals summary ───────────────────────────────────────────
            val goalsCardModifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp)
            val percent = uiState.goalsCompletionPercent
            if (percent != null) {
                DailyGoalsCard(percent = percent, onClick = onGoalsClick, modifier = goalsCardModifier)
            } else if (!uiState.isLoading) {
                NoGoalsYetCard(onClick = onAddGoalClick, modifier = goalsCardModifier)
            }

            // ── Per-prayer streaks ─────────────────────────────────────────────
            StreakRow(
                streaks  = uiState.streaks,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            )

            // ── Section title ─────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.home_daily_stack),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                color = SalatPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = MontserratFamily,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Prayer cards ──────────────────────────────────────────────────
            if (uiState.isLoading) {
                repeat(5) {
                    PrayerCardSkeleton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            } else {
                uiState.prayers.forEach { prayer ->
                    PrayerCard(
                        entry = prayer,
                        onClick = { onPrayerCardClick(prayer.type) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
            }

            // ── Inspirational banner ──────────────────────────────────────────
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

    // ── Prayer status bottom sheet ────────────────────────────────────────────
    uiState.sheetArgs?.let { args ->
        PrayerStatusBottomSheet(
            args      = args,
            isSaving  = uiState.isSaving,
            onDismiss = onSheetDismiss,
            onSave    = onStatusSave,
        )
    }
}

// ── Daily goals summary ───────────────────────────────────────────────────────

@Composable
private fun DailyGoalsCard(percent: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick  = onClick,
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        color    = SalatSurfaceContainer,
        border   = BorderStroke(1.dp, SalatOutlineVariant),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(SalatPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_check_circle_filled), null, tint = SalatOnPrimary, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(stringResource(R.string.home_daily_goals), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = InterFamily)
                    Text(stringResource(R.string.home_percent_completed, percent), color = SalatPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
                }
            }
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress    = { 1f },
                    modifier    = Modifier.fillMaxSize(),
                    color       = SalatSurfaceVariant,
                    strokeWidth = 3.dp,
                )
                CircularProgressIndicator(
                    progress    = { percent / 100f },
                    modifier    = Modifier.fillMaxSize(),
                    color       = SalatPrimary,
                    strokeWidth = 3.dp,
                )
                Text("$percent%", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFamily, color = SalatOnSurface)
            }
        }
    }
}

@Composable
private fun NoGoalsYetCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick  = onClick,
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        color    = SalatSurfaceContainer,
        border   = BorderStroke(1.dp, SalatOutlineVariant),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(SalatPrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_add), null, tint = SalatOnPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(stringResource(R.string.home_daily_goals), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = InterFamily)
                    Text(stringResource(R.string.home_set_first_goal), color = SalatPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
                }
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = SalatOnSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Per-prayer streaks ────────────────────────────────────────────────────────

@Composable
private fun StreakRow(
    streaks: Map<PrayerType, StreakInfo>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PrayerType.values().forEach { type ->
            val info = streaks[type] ?: StreakInfo(0, 0)
            StreakTile(
                type     = type,
                info     = info,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StreakTile(
    type: PrayerType,
    info: StreakInfo,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SalatPrimaryContainer)
            .padding(vertical = 12.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(type.iconRes),
            contentDescription = null,
            tint = SalatPrimary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${info.currentStreak}",
            color = SalatOnPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MontserratFamily,
        )
        Text(
            text = type.label(),
            color = SalatOnPrimaryContainer,
            fontSize = 10.sp,
            fontFamily = InterFamily,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.home_best_streak, info.highestStreak),
            color = SalatOnSurfaceVariant,
            fontSize = 9.sp,
            fontFamily = InterFamily,
        )
    }
}

// ── Prayer card ───────────────────────────────────────────────────────────────

@Composable
private fun PrayerCard(
    entry: PrayerEntryUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionScale = remember { Animatable(1f) }
    val timeFormatter    = remember { DateTimeFormatter.ofPattern("hh:mm a") }

    Surface(
        onClick = onClick,
        enabled = entry.isMarkable,
        modifier = modifier.clip(RoundedCornerShape(12.dp)).alpha(if (entry.isMarkable) 1f else 0.6f),
        shape = RoundedCornerShape(12.dp),
        color = SalatSurface,
        border = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SalatSecondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(entry.type.iconRes),
                    contentDescription = null,
                    tint = SalatPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.type.label(),
                    color = SalatPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = MontserratFamily,
                )
                if (entry.time != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_schedule),
                            contentDescription = null,
                            tint = SalatOnSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = entry.time.format(timeFormatter),
                            color = SalatOnSurfaceVariant,
                            fontSize = 14.sp,
                            fontFamily = InterFamily,
                        )
                    }
                }
                if (entry.reminderTime != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_schedule),
                            contentDescription = null,
                            tint = SalatPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.home_reminder_at, entry.reminderTime.format(timeFormatter)),
                            color = SalatPrimary,
                            fontSize = 12.sp,
                            fontFamily = InterFamily,
                        )
                    }
                }
            }

            // Status chip
            PrayerStatusChip(status = entry.status, isUpcoming = !entry.isMarkable)
        }
    }
}

// ── Status chip ───────────────────────────────────────────────────────────────

@Composable
private fun PrayerStatusChip(status: PrayerStatus, isUpcoming: Boolean = false) {
    val (bg, fg, label) = if (status == PrayerStatus.NOT_RECORDED && isUpcoming) {
        Triple(SalatSurfaceContainerHigh, SalatOnSurfaceVariant, stringResource(R.string.status_upcoming))
    } else when (status) {
        PrayerStatus.CONGREGATION    -> Triple(SalatPrimary,            SalatOnPrimary,          stringResource(R.string.status_congregation))
        PrayerStatus.INDIVIDUAL      -> Triple(SalatPrimaryContainer,   SalatPrimaryFixed,        stringResource(R.string.status_individual))
        PrayerStatus.QAZA_PENDING    -> Triple(SalatTertiaryContainer,  Color(0xFFFF9939),        stringResource(R.string.status_qaza_pending))
        PrayerStatus.QAZA_COMPLETED  -> Triple(SalatSecondaryContainer, SalatOnSecondaryContainer,stringResource(R.string.status_qaza_done))
        PrayerStatus.MENSTRUATION    -> Triple(SalatErrorContainer,     SalatOnErrorContainer,    stringResource(R.string.status_menstruation))
        PrayerStatus.NOT_RECORDED    -> Triple(SalatSurfaceContainerHigh, SalatOnSurfaceVariant,  stringResource(R.string.status_not_recorded))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label.uppercase(),
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFamily,
            letterSpacing = 0.5.sp,
        )
    }
}

// ── Skeleton placeholder ──────────────────────────────────────────────────────

@Composable
private fun PrayerCardSkeleton(modifier: Modifier = Modifier) {
    val shimmerAlpha by rememberInfiniteTransition(label = "shimmer")
        .animateFloat(
            initialValue = 0.3f, targetValue = 0.7f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "alpha",
        )
    Box(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SalatSurfaceContainerHigh.copy(alpha = shimmerAlpha))
    )
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
                text = "\u201c${verse.text}\u201d",
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

// ── Helpers ───────────────────────────────────────────────────────────────────

private val PrayerType.iconRes get() = when (this) {
    PrayerType.FAJR    -> R.drawable.ic_wb_twilight
    PrayerType.DHUHR   -> R.drawable.ic_light_mode
    PrayerType.ASR     -> R.drawable.ic_wb_sunny
    PrayerType.MAGHRIB -> R.drawable.ic_wb_twilight
    PrayerType.ISHA    -> R.drawable.ic_dark_mode
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        uiState = HomeUiState(
            isLoading = false,
            hijriDate = "24 Rajab 1445",
            prayers = PrayerType.values().mapIndexed { i, type ->
                PrayerEntryUi(
                    type         = type,
                    time         = LocalTime.of(5 + i * 2, 30),
                    reminderTime = if (i == 1) LocalTime.of(5 + i * 2, 25) else null,
                    status       = if (i < 2) PrayerStatus.CONGREGATION else PrayerStatus.NOT_RECORDED,
                    isMarkable   = i < 3,
                )
            },
            streaks = PrayerType.values().mapIndexed { i, type ->
                type to StreakInfo(currentStreak = listOf(7, 12, 3, 0, 5)[i], highestStreak = listOf(42, 20, 15, 8, 30)[i])
            }.toMap(),
            verse = VerseInfo(
                text = "Verily, in the remembrance of Allah do hearts find rest.",
                citation = "Ar-Ra'd 13:28",
            ),
            goalsCompletionPercent = 68,
        ),
        onPrayerCardClick  = {},
        onSheetDismiss     = {},
        onStatusSave       = {},
        onGoalsClick       = {},
        onAddGoalClick     = {},
    )
}