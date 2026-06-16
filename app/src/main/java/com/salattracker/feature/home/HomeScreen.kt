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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.PrayerStatusBottomSheet
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ── Home Screen ───────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onPrayerCardClick: (PrayerType) -> Unit,
    onSheetDismiss: () -> Unit,
    onStatusSave: (PrayerStatus) -> Unit,
    onBulkUpdateClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp)   // room for FAB + bottom nav
        ) {
            // ── Streak bento card ─────────────────────────────────────────────
            StreakCard(
                currentStreak = uiState.streak.currentStreak,
                highestStreak = uiState.streak.highestStreak,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            )

            // ── Section title ─────────────────────────────────────────────────
            Text(
                text = "The Daily Stack",
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
            InspirationBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp)
            )
        }

        // ── Floating Action Button ────────────────────────────────────────────
        BulkUpdateFab(
            onClick = onBulkUpdateClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 88.dp), // above nav bar
        )
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

// ── Streak card ───────────────────────────────────────────────────────────────

@Composable
private fun StreakCard(
    currentStreak: Int,
    highestStreak: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SalatPrimaryContainer),
    ) {
        // Decorative watermark icon
        Icon(
            painter = painterResource(R.drawable.ic_auto_awesome),
            contentDescription = null,
            tint = SalatOnPrimary.copy(alpha = 0.08f),
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 32.dp, y = 32.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "CURRENT PROGRESS",
                    color = SalatOnPrimaryContainer,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFamily,
                    letterSpacing = 2.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$currentStreak Day${if (currentStreak != 1) "s" else ""} Streak",
                    color = SalatOnPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MontserratFamily,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_workspace_premium),
                    contentDescription = null,
                    tint = Color(0xFFFFB77D),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Highest: $highestStreak Days",
                    color = SalatOnPrimaryContainer,
                    fontSize = 14.sp,
                    fontFamily = InterFamily,
                )
            }
        }
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
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
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
                    text = entry.type.displayName,
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
            }

            // Status chip
            PrayerStatusChip(status = entry.status)
        }
    }
}

// ── Status chip ───────────────────────────────────────────────────────────────

@Composable
private fun PrayerStatusChip(status: PrayerStatus) {
    val (bg, fg, label) = when (status) {
        PrayerStatus.CONGREGATION    -> Triple(SalatPrimary,            SalatOnPrimary,          "Congregation")
        PrayerStatus.INDIVIDUAL      -> Triple(SalatPrimaryContainer,   SalatPrimaryFixed,        "Individual")
        PrayerStatus.QAZA_PENDING    -> Triple(SalatTertiaryContainer,  Color(0xFFFF9939),        "Qaza Pending")
        PrayerStatus.QAZA_COMPLETED  -> Triple(SalatSecondaryContainer, SalatOnSecondaryContainer,"Qaza Done")
        PrayerStatus.MENSTRUATION    -> Triple(SalatErrorContainer,     SalatOnErrorContainer,    "Menstruation")
        PrayerStatus.NOT_RECORDED    -> Triple(SalatSurfaceContainerHigh, SalatOnSurfaceVariant,  "Not Recorded")
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
private fun InspirationBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(SalatPrimaryContainer, SalatPrimary)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(
            text = "\u201cVerily, in the remembrance of Allah do hearts find rest.\u201d",
            color = Color.White,
            fontSize = 16.sp,
            fontStyle = FontStyle.Italic,
            fontFamily = InterFamily,
            lineHeight = 22.sp,
        )
    }
}

// ── Bulk Update FAB ───────────────────────────────────────────────────────────

@Composable
private fun BulkUpdateFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Subtle periodic glow pulse
    val glowAlpha by rememberInfiniteTransition(label = "glow")
        .animateFloat(
            initialValue = 0f, targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                tween(1_000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(2_000),
            ),
            label = "glowAlpha",
        )

    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = SalatPrimary,
        contentColor = SalatOnPrimary,
        shape = RoundedCornerShape(50),
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_edit_square),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        },
        text = {
            Text(
                text = "BULK UPDATE",
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
            )
        },
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private val PrayerType.iconRes get() = when (this) {
    PrayerType.FAJR    -> R.drawable.ic_wb_twilight
    PrayerType.DHUHR   -> R.drawable.ic_light_mode
    PrayerType.ASR     -> R.drawable.ic_wb_sunny
    PrayerType.MAGHRIB -> R.drawable.ic_wb_twilight
    PrayerType.ISHA    -> R.drawable.ic_dark_mode
}

private val PrayerType.displayName get() = when (this) {
    PrayerType.FAJR    -> "Fajr"
    PrayerType.DHUHR   -> "Dhuhr"
    PrayerType.ASR     -> "Asr"
    PrayerType.MAGHRIB -> "Maghrib"
    PrayerType.ISHA    -> "Isha"
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
                    type   = type,
                    time   = LocalTime.of(5 + i * 2, 30),
                    status = if (i < 2) PrayerStatus.CONGREGATION else PrayerStatus.NOT_RECORDED,
                )
            },
            streak = com.salattracker.domain.model.StreakInfo(7, 42),
        ),
        onPrayerCardClick  = {},
        onSheetDismiss     = {},
        onStatusSave       = {},
        onBulkUpdateClick  = {},
    )
}