package com.salattracker.feature.qaza

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.PrayerType
import java.time.LocalDate

// ── Qaza Screen ───────────────────────────────────────────────────────────────

@Composable
fun QazaScreen(
    uiState: QazaUiState,
    onMarkCompleted: (QazaPendingEntryUi) -> Unit,
    onViewAllClick: () -> Unit = {},
    onErrorDismiss: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 100.dp),
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ── Hero backlog banner ───────────────────────────────────────────
            BacklogHeroBanner(
                total     = uiState.totalBacklog,
                isLoading = uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Per-prayer bento grid ─────────────────────────────────────────
            PrayerBalanceGrid(
                balances  = uiState.balances,
                isLoading = uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Recent missed list header ─────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text       = "Recent Missed Prayers",
                    color      = SalatOnSurface,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = MontserratFamily,
                )
                TextButton(onClick = onViewAllClick) {
                    Text(
                        text       = "View All",
                        color      = SalatPrimary,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFamily,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Pending Qaza entries ──────────────────────────────────────────
            if (uiState.isLoading) {
                repeat(3) {
                    QazaEntrySkeleton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            } else if (uiState.pendingEntries.isEmpty()) {
                QazaEmptyState()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.pendingEntries.forEach { entry ->
                        val isCompleting = entry.id in uiState.completingIds
                        AnimatedVisibility(
                            visible = !isCompleting,
                            exit    = slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(400, easing = FastOutSlowInEasing),
                            ) + fadeOut(tween(400)),
                        ) {
                            QazaEntryCard(
                                entry           = entry,
                                onMarkCompleted = { onMarkCompleted(entry) },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Insight card ──────────────────────────────────────────────────
            if (!uiState.isLoading && uiState.insightText.isNotEmpty()) {
                InsightCard(text = uiState.insightText)
            }
        }

        // ── Error snackbar ────────────────────────────────────────────────────
        uiState.error?.let { message ->
            Snackbar(
                modifier      = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action        = {
                    TextButton(onClick = onErrorDismiss) {
                        Text("OK", color = SalatPrimaryFixed)
                    }
                },
                containerColor = SalatInverseSurface,
                contentColor   = SalatInverseOnSurface,
            ) { Text(message, fontFamily = InterFamily, fontSize = 14.sp) }
        }
    }
}

// ── Hero backlog banner ───────────────────────────────────────────────────────

@Composable
private fun BacklogHeroBanner(total: Int, isLoading: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(listOf(SalatPrimaryContainer, SalatPrimary))
            )
            .padding(vertical = 28.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Subtle geometric dot pattern overlay
        Canvas(modifier = Modifier.matchParentSize()) {
            val spacing = 20.dp.toPx()
            var x = 0f
            while (x <= size.width) {
                var y = 0f
                while (y <= size.height) {
                    drawCircle(
                        color  = Color.White.copy(alpha = 0.04f),
                        radius = 1.5f,
                        center = androidx.compose.ui.geometry.Offset(x, y),
                    )
                    y += spacing
                }
                x += spacing
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text          = "TOTAL OUTSTANDING",
                color         = SalatOnPrimaryContainer.copy(alpha = 0.8f),
                fontSize      = 11.sp,
                fontWeight    = FontWeight.SemiBold,
                fontFamily    = InterFamily,
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                )
            } else {
                Text(
                    text       = "Overall Backlog: $total",
                    color      = Color.White,
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MontserratFamily,
                    textAlign  = TextAlign.Center,
                    lineHeight = 38.sp,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    }
}

// ── Per-prayer bento grid ─────────────────────────────────────────────────────

@Composable
private fun PrayerBalanceGrid(
    balances: List<QazaBalanceUi>,
    isLoading: Boolean,
) {
    // 2 rows, auto-fill: first row 2 cols, second row 3 cols on small screens
    // We use a simple LazyVerticalGrid equivalent with two manual rows
    val shimmerAlpha by rememberInfiniteTransition(label = "sk")
        .animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val displayList = if (isLoading) List(5) { null } else balances.map { it as QazaBalanceUi? }

        // Split into two rows: first 2, then 3
        val row1 = displayList.take(2)
        val row2 = displayList.drop(2)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            row1.forEach { balance ->
                if (balance == null || isLoading) {
                    PrayerBalanceCardSkeleton(shimmerAlpha, Modifier.fillMaxWidth())
                } else {
                    PrayerBalanceCard(balance, Modifier.fillMaxWidth())
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            row2.forEach { balance ->
                if (balance == null || isLoading) {
                    PrayerBalanceCardSkeleton(shimmerAlpha, Modifier.fillMaxWidth())
                } else {
                    PrayerBalanceCard(balance, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun PrayerBalanceCard(balance: QazaBalanceUi, modifier: Modifier = Modifier) {
    Surface(
        modifier        = modifier,
        shape           = RoundedCornerShape(12.dp),
        color           = SalatSurface,
        border          = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier              = Modifier.padding(16.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.Center,
        ) {
            Icon(
                painter         = painterResource(balance.prayerType.iconRes),
                contentDescription = null,
                tint            = SalatPrimary,
                modifier        = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text       = balance.prayerType.displayName,
                color      = SalatOnSurfaceVariant,
                fontSize   = 11.sp,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text       = balance.pendingCount.toString(),
                color      = SalatPrimary,
                fontSize   = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = MontserratFamily,
            )
        }
    }
}

@Composable
private fun PrayerBalanceCardSkeleton(alpha: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(88.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SalatSurfaceContainerHigh.copy(alpha = alpha))
    )
}

// ── Pending Qaza entry card ───────────────────────────────────────────────────

@Composable
private fun QazaEntryCard(
    entry: QazaPendingEntryUi,
    onMarkCompleted: () -> Unit,
) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(12.dp),
        color           = SalatSurface,
        border          = BorderStroke(1.dp, SalatSecondaryContainer),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon circle
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SalatSecondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter         = painterResource(entry.prayerType.iconRes),
                    contentDescription = null,
                    tint            = SalatOnSecondaryContainer,
                    modifier        = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Prayer name + date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = entry.prayerType.displayName,
                    color      = SalatOnSurface,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFamily,
                )
                Text(
                    text     = entry.dateLabel,
                    color    = SalatOnSurfaceVariant,
                    fontSize = 11.sp,
                    fontFamily = InterFamily,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Mark Completed button
            Button(
                onClick  = onMarkCompleted,
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = SalatPrimary,
                    contentColor   = SalatOnPrimary,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_circle_filled),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text       = "Mark Done",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFamily,
                    letterSpacing = 0.3.sp,
                )
            }
        }
    }
}

@Composable
private fun QazaEntrySkeleton(modifier: Modifier = Modifier) {
    val alpha by rememberInfiniteTransition(label = "sk")
        .animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SalatSurfaceContainerHigh.copy(alpha = alpha))
    )
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun QazaEmptyState() {
    Column(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter         = painterResource(R.drawable.ic_check_circle_filled),
            contentDescription = null,
            tint            = SalatPrimary.copy(alpha = 0.4f),
            modifier        = Modifier.size(48.dp),
        )
        Text(
            text       = "No pending Qaza prayers!",
            color      = SalatPrimary,
            fontSize   = 20.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MontserratFamily,
            textAlign  = TextAlign.Center,
        )
        Text(
            text       = "MashAllah — keep up the consistency.",
            color      = SalatOnSurfaceVariant,
            fontSize   = 14.sp,
            fontFamily = InterFamily,
            textAlign  = TextAlign.Center,
        )
    }
}

// ── Insight card ──────────────────────────────────────────────────────────────

@Composable
private fun InsightCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = SalatSurfaceContainerLow,
        border   = BorderStroke(1.dp, SalatOutlineVariant.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter         = painterResource(R.drawable.ic_auto_awesome),
                contentDescription = null,
                tint            = SalatSurfaceTint,
                modifier        = Modifier.size(32.dp),
            )
            Column {
                Text(
                    text       = "Consistency Insight",
                    color      = SalatPrimary,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFamily,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text       = text,
                    color      = SalatOnSurfaceVariant,
                    fontSize   = 14.sp,
                    fontFamily = InterFamily,
                    lineHeight = 20.sp,
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

private val PrayerType.displayName get() = when (this) {
    PrayerType.FAJR    -> "Fajr"
    PrayerType.DHUHR   -> "Dhuhr"
    PrayerType.ASR     -> "Asr"
    PrayerType.MAGHRIB -> "Maghrib"
    PrayerType.ISHA    -> "Isha"
}

// ── Canvas import ─────────────────────────────────────────────────────────────
//// (needed for the dot-pattern overlay in BacklogHeroBanner)
//@Suppress("unused")
//private val canvasImport = androidx.compose.foundation.Canvas

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun QazaScreenPreview() {
    QazaScreen(
        uiState = QazaUiState(
            isLoading    = false,
            totalBacklog = 25,
            balances     = PrayerType.values().mapIndexed { i, t ->
                QazaBalanceUi(t, listOf(3, 7, 4, 5, 6)[i])
            },
            pendingEntries = listOf(
                QazaPendingEntryUi("1", PrayerType.DHUHR,   LocalDate.now().minusDays(1), "Tuesday, 23 Rajab"),
                QazaPendingEntryUi("2", PrayerType.ISHA,    LocalDate.now().minusDays(2), "Monday, 22 Rajab"),
                QazaPendingEntryUi("3", PrayerType.MAGHRIB, LocalDate.now().minusDays(3), "Sunday, 21 Rajab"),
                QazaPendingEntryUi("4", PrayerType.ASR,     LocalDate.now().minusDays(3), "Sunday, 21 Rajab"),
            ),
            insightText = "You've cleared 5 prayers this week! Maintaining this pace will clear your Dhuhr backlog in approximately 10 days.",
        ),
        onMarkCompleted = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun QazaScreenEmptyPreview() {
    QazaScreen(
        uiState = QazaUiState(
            isLoading    = false,
            totalBacklog = 0,
            balances     = PrayerType.values().map { QazaBalanceUi(it, 0) },
            pendingEntries = emptyList(),
            insightText  = "MashAllah! You have no outstanding Qaza prayers.",
        ),
        onMarkCompleted = {},
    )
}