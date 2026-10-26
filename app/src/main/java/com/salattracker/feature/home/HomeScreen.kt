package com.salattracker.feature.home

import android.animation.ValueAnimator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.*
import com.salattracker.domain.model.*
import com.salattracker.domain.usecase.*
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.*

@Composable
private fun LegacyHomeScreen(
    uiState: HomeUiState,
    onPrayerCardClick: (PrayerType) -> Unit,
    onSheetDismiss: () -> Unit,
    onStatusSave: (PrayerStatus) -> Unit,
    onQazaClick: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onQuickStatusSave: (PrayerType, PrayerStatus) -> Unit = { type, _ -> onPrayerCardClick(type) },
    onReminderToggle: (PrayerType) -> Unit = {},
    onMenuClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
    onDonateClick: () -> Unit = {},
    onCatchUpClick: () -> Unit = {},
    onAppearanceChanged: (HomeAppearance) -> Unit = {},
    onErrorDismiss: () -> Unit = {},
) {
    var showStreaks by rememberSaveable { mutableStateOf(false) }
    var showAppearance by rememberSaveable { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<HomeSaveFeedback?>(null) }
    var handledFeedback by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val feedbackEffects = rememberHomeFeedbackEffects(uiState.appearance, showAppearance)
    LaunchedEffect(uiState.errorRes) {
        uiState.errorRes?.let { snackbar.showSnackbar(context.getString(it)); onErrorDismiss() }
    }
    LaunchedEffect(uiState.feedback?.id) {
        val saved = uiState.feedback ?: return@LaunchedEffect
        if (handledFeedback == saved.id) return@LaunchedEffect
        handledFeedback = saved.id
        feedback = saved
        val celebrate = saved.onTime && saved.status in listOf(PrayerStatus.CONGREGATION, PrayerStatus.INDIVIDUAL)
        feedbackEffects.play(context.getString(R.string.home_v8_well_done), celebrate)
        delay(3600); feedback = null
    }
    HomeDesign {
        Box(Modifier.fillMaxSize().background(SalatBackground).testTag("home-screen")) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("home-scroll")) {
                HomeHero(uiState, onMenuClick, onLocationClick, onQuickStatusSave,
                    { showStreaks = true }, onAppearanceChanged)
                if (!uiState.appearance.streakBadge)
                    StreakDisplay(uiState, false, onAppearanceChanged) { showStreaks = true }
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    if (uiState.isLoading) {
                        LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 20.dp), color = SalatPrimary)
                        repeat(5) { Box(Modifier.fillMaxWidth().height(64.dp).padding(vertical = 8.dp)
                            .background(SalatOutlineVariant.copy(alpha = .3f), RoundedCornerShape(8.dp))) }
                    } else {
                        WeekHistory(uiState, onDayClick)
                        if (uiState.lastSevenDays.count { (_, marks) ->
                            marks.isEmpty() || marks.values.all { it == PrayerStatus.NOT_RECORDED } } >= 3) {
                            TextButton(onClick = onCatchUpClick, contentPadding = PaddingValues(0.dp)) {
                                HomeText(stringResource(R.string.home_v8_catch_up), size = 12, color = SalatPrimary)
                            }
                        }
                        uiState.prayers.forEach { entry ->
                            HomePrayerRow(entry, uiState, onQuickStatusSave, onReminderToggle,
                                { onPrayerCardClick(entry.type) }, { showStreaks = true })
                        }
                        uiState.verse?.let { HomeVerse(it) }
                        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button, onClick = onDonateClick)
                            .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(painterResource(R.drawable.home_heart), null, tint = SalatPrimary, modifier = Modifier.size(18.dp))
                            HomeText(stringResource(R.string.home_v8_donate), Modifier.weight(1f), size = 13, muted = true)
                            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = SalatOnSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                        HorizontalDivider(color = SalatOutlineVariant)
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
            feedback?.let { saved ->
                if (uiState.appearance.showAppreciation && saved.status != PrayerStatus.NOT_RECORDED) {
                    Surface(Modifier.align(Alignment.TopCenter).statusBarsPadding()
                        .padding(top = 12.dp, start = 64.dp, end = 28.dp).testTag("home-appreciation"),
                        shape = RoundedCornerShape(14.dp), color = HomeCream, shadowElevation = 6.dp) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.home_flame), null, tint = HomeGold, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            val message = when {
                                saved.status == PrayerStatus.MENSTRUATION -> R.string.home_v8_rest
                                saved.status in listOf(PrayerStatus.CONGREGATION, PrayerStatus.INDIVIDUAL) && saved.onTime -> R.string.home_v8_well_done
                                else -> R.string.home_v8_saved
                            }
                            HomeText(stringResource(message), size = 13, color = Color(0xFF0B4F53), weight = FontWeight.SemiBold)
                        }
                    }
                }
                if (uiState.appearance.confetti && saved.onTime &&
                    saved.status in listOf(PrayerStatus.CONGREGATION, PrayerStatus.INDIVIDUAL) && ValueAnimator.areAnimatorsEnabled())
                    HomeConfetti(saved.id)
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
        if (showStreaks) StreakSheet(uiState, { showStreaks = false }) {
            showStreaks = false; showAppearance = true
        }
        if (showAppearance) AppearanceSheet(uiState.appearance, onAppearanceChanged, feedbackEffects) { showAppearance = false }
        uiState.sheetArgs?.let { args ->
            PrayerStatusBottomSheet(args = args, isSaving = uiState.isSaving,
                onDismiss = onSheetDismiss, onSave = onStatusSave, gender = uiState.gender)
        }
    }
}

@Composable private fun HomeHero(s: HomeUiState, onMenu: () -> Unit, onLocation: () -> Unit,
    onSave: (PrayerType, PrayerStatus) -> Unit, onOpenStreaks: () -> Unit, onAppearanceChanged: (HomeAppearance) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val hijriAdjustment = LocalHijriAdjustmentDays.current
    val date = s.now.toLocalDate()
    val hijri = remember(date, hijriAdjustment) { date.toHijriOrNull(hijriAdjustment) }
    val current = s.prayers.firstOrNull { it.type == s.currentPrayer }
    val next = s.nextPrayer
    val type = current?.type ?: next?.type
    val name = type?.label(if (current != null) s.todayDate else date.plusDays(if (next?.isTomorrow == true) 1 else 0),
        s.jumuahEnabled, s.madhab)
    val remaining = current?.windowEnd?.let { Duration.between(s.now, it).toMinutes().coerceAtLeast(0) } ?: s.minutesUntilNext
    val progress = current?.let { entry ->
        val start = entry.time?.let(s.todayDate::atTime)
        val end = entry.windowEnd
        if (start != null && end != null) (Duration.between(s.now, end).seconds.toFloat() /
            Duration.between(start, end).seconds.coerceAtLeast(1)).coerceIn(0f, 1f) else 0f
    } ?: 0f
    Box(Modifier.fillMaxWidth().testTag("home-hero")) {
        Image(painterResource(R.drawable.home_mosque), null, Modifier.matchParentSize(),
            contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to Color(0xB8064E52), .55f to Color(0x6B064E52), 1f to Color(0xDB053C40))))
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenu) {
                    Icon(painterResource(R.drawable.ic_menu), stringResource(R.string.home_v8_menu),
                        tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.weight(1f))
                if (s.appearance.streakBadge) StreakDisplay(s, true, onAppearanceChanged, onOpenStreaks)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(hijri?.fullLabel() ?: date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale)),
                        color = Color.White, fontFamily = HomeTitleFont, fontWeight = FontWeight.SemiBold,
                        fontSize = 27.sp, lineHeight = 30.sp)
                    Spacer(Modifier.height(6.dp))
                    HomeText(date.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale)),
                        size = 13, color = Color.White.copy(alpha = .8f))
                    Row(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onLocation),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.onboarding_pin), null, tint = HomeGold, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        HomeText(s.locationLabel?.substringBefore(',') ?: stringResource(R.string.home_v8_choose_city),
                            size = 12, color = Color.White.copy(alpha = .85f))
                    }
                }
                Box(Modifier.width(150.dp).heightIn(min = 180.dp).testTag("home-countdown"), contentAlignment = Alignment.Center) {
                    HomeArch(progress, Modifier.matchParentSize())
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        if (s.isLoading) CircularProgressIndicator(Modifier.size(26.dp), color = HomeGold, strokeWidth = 2.dp)
                        else if (name != null) {
                            if (current != null) Text(name, color = Color.White, fontFamily = HomeTitleFont,
                                fontSize = 27.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                            else HomeText(stringResource(R.string.home_v8_begins, name), size = 12, color = Color.White, align = TextAlign.Center)
                            HomeText(stringResource(R.string.home_v8_remaining, (remaining / 60).toInt(), (remaining % 60).toInt()),
                                size = 30, color = Color.White, align = TextAlign.Center)
                            if (current != null) HomeText(stringResource(R.string.home_v8_left), size = 12, color = Color.White.copy(alpha = .8f))
                        } else HomeText(stringResource(R.string.home_v8_times_unavailable), size = 13, color = Color.White, align = TextAlign.Center)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PhotoInfo(s.appearance.showPhotoInfo, Modifier.weight(1f),
                    { onAppearanceChanged(s.appearance.copy(showPhotoInfo = false)) },
                    { onAppearanceChanged(s.appearance.copy(showPhotoInfo = true)) })
                Box(Modifier.widthIn(min = 132.dp, max = 180.dp), contentAlignment = Alignment.Center) {
                    if (current != null) StatusActions(current, s, hero = true, onSave = onSave)
                    else if (name != null) HomeText(stringResource(R.string.home_v8_opens, name),
                        size = 12, color = Color.White.copy(alpha = .85f), align = TextAlign.Center)
                    else TextButton(onClick = onLocation) {
                        HomeText(stringResource(R.string.home_v8_choose_city), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable private fun PhotoInfo(shown: Boolean, modifier: Modifier, onHide: () -> Unit, onShow: () -> Unit) {
    if (!shown) Box(modifier) {
        IconButton(onClick = onShow, modifier = Modifier.size(48.dp).testTag("home-photo-show").background(Color(0x8804282B), CircleShape)) {
            Icon(painterResource(R.drawable.onboarding_pin), stringResource(R.string.home_v8_about_photo),
                tint = HomeGold, modifier = Modifier.size(17.dp))
        }
    } else {
        var drag by remember { mutableStateOf(Offset.Zero) }
        Row(modifier.offset { IntOffset(drag.x.roundToInt(), drag.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(onDrag = { change, delta -> change.consume(); drag += delta },
                    onDragEnd = { if (drag.getDistance() > 64.dp.toPx()) onHide(); drag = Offset.Zero },
                    onDragCancel = { drag = Offset.Zero })
            }.background(Color(0x9904282B), RoundedCornerShape(10.dp))
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                HomeText(stringResource(R.string.home_v8_photo_title), size = 11, color = Color.White, weight = FontWeight.SemiBold)
                HomeText(stringResource(R.string.home_v8_photo_detail), size = 10, color = Color.White.copy(alpha = .85f))
            }
            IconButton(onClick = onHide, modifier = Modifier.size(32.dp).testTag("home-photo-hide")) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.home_v8_hide_photo),
                    tint = Color.White, modifier = Modifier.size(13.dp))
            }
        }
    }
}

@Composable private fun StreakDisplay(s: HomeUiState, badge: Boolean, onChange: (HomeAppearance) -> Unit, onOpen: () -> Unit) {
    val top = s.streaks.maxByOrNull { it.value.currentStreak }
    val count = top?.value?.currentStreak ?: 0
    val text = if (count > 0 && top != null) stringResource(R.string.home_v8_streak_value,
        top.key.label(s.todayDate, s.jumuahEnabled, s.madhab), count) else stringResource(R.string.home_v8_streak_start)
    var drag by remember { mutableStateOf(Offset.Zero) }
    val action = stringResource(if (badge) R.string.home_v8_move_band else R.string.home_v8_move_badge)
    Row((if (badge) Modifier.widthIn(max = 220.dp) else Modifier.fillMaxWidth())
        .offset { IntOffset(drag.x.roundToInt(), drag.y.roundToInt()) }
        .semantics { customActions = listOf(CustomAccessibilityAction(action) {
            onChange(s.appearance.copy(streakBadge = !badge)); true
        }) }.pointerInput(badge) {
            detectDragGesturesAfterLongPress(onDrag = { change, delta -> change.consume(); drag += delta },
                onDragEnd = {
                    if ((!badge && drag.y < -48.dp.toPx()) || (badge && drag.y > 48.dp.toPx()))
                        onChange(s.appearance.copy(streakBadge = !badge))
                    drag = Offset.Zero
                }, onDragCancel = { drag = Offset.Zero })
        }.clip(if (badge) CircleShape else RoundedCornerShape(0.dp)).background(HomeCream)
        .clickable(role = Role.Button, onClick = onOpen).heightIn(min = 48.dp)
        .padding(horizontal = if (badge) 12.dp else 20.dp, vertical = 8.dp).testTag("home-streak"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(painterResource(R.drawable.home_flame), null, tint = HomeGold, modifier = Modifier.size(18.dp))
        HomeText(if (badge && count == 0) stringResource(R.string.home_v8_streaks) else text,
            if (badge) Modifier else Modifier.weight(1f), size = if (badge) 12 else 13,
            color = Color(0xFF111111), weight = FontWeight.Medium)
        if (!badge) Icon(painterResource(R.drawable.ic_chevron_right), null,
            tint = Color(0xFF6B7270), modifier = Modifier.size(16.dp))
    }
}

@Composable private fun WeekHistory(s: HomeUiState, onDay: (LocalDate) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val dates = (6 downTo 0).map { s.todayDate.minusDays(it.toLong()) }
    HomeText(stringResource(R.string.home_v8_week).uppercase(locale) + " · " +
        dates.first().format(DateTimeFormatter.ofPattern("d", locale)) + "–" +
        s.todayDate.format(DateTimeFormatter.ofPattern("d MMM", locale)),
        Modifier.padding(top = 12.dp, bottom = 4.dp), size = 11, muted = true, weight = FontWeight.SemiBold)
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        dates.forEach { date ->
            val dayDescription = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
            Column(Modifier.weight(1f).heightIn(min = 58.dp).clickable(role = Role.Button) { onDay(date) }
                .semantics { contentDescription = dayDescription }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(34.dp).let {
                    if (date == s.todayDate) it.border(1.dp, SalatPrimary, CircleShape) else it
                }.padding(2.dp)) {
                    PrayerStatusRing(if (date == s.todayDate) s.prayers.associate { it.type to it.status }
                        else s.lastSevenDays[date].orEmpty(), Modifier.fillMaxSize())
                }
                HomeText(date.format(DateTimeFormatter.ofPattern("EEEEE", locale)), size = 11,
                    color = if (date == s.todayDate) SalatPrimary else SalatOnSurfaceVariant)
            }
        }
    }
}

@Composable private fun HomePrayerRow(entry: PrayerEntryUi, s: HomeUiState,
    onSave: (PrayerType, PrayerStatus) -> Unit, onReminder: (PrayerType) -> Unit,
    onDetails: () -> Unit, onOpenStreaks: () -> Unit) {
    val name = entry.type.label(s.todayDate, s.jumuahEnabled, s.madhab)
    val current = entry.type == s.currentPrayer
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("home-row-${entry.type}")) {
        val stacked = maxWidth < 335.dp || fontScale > 1.15f
        Column(Modifier.fillMaxWidth().let {
            if (current) it.border(1.dp, SalatPrimary.copy(alpha = .3f), RoundedCornerShape(8.dp)) else it
        }.padding(vertical = 7.dp).alpha(if (entry.isMarkable) 1f else .55f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(prayerIcon(entry.type)), null,
                    tint = SalatOnSurfaceVariant, modifier = Modifier.size(18.dp))
                Column(Modifier.weight(1f).heightIn(min = 48.dp)
                    .clickable(enabled = entry.isMarkable, onClick = onDetails), verticalArrangement = Arrangement.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HomeText(name, Modifier.weight(1f, fill = false), size = 15, weight = FontWeight.Medium)
                        val streak = s.streaks[entry.type]?.currentStreak ?: 0
                        if (streak > 0) Row(Modifier.clickable(role = Role.Button, onClick = onOpenStreaks)
                            .padding(start = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.home_flame), null,
                                tint = HomeGold.copy(alpha = if (entry.loggedOnTime == true) 1f else .65f),
                                modifier = Modifier.size(13.dp))
                            HomeText("$streak", size = 12, muted = true)
                        }
                    }
                    HomeText(entry.time?.formatAppTime(LocalUse24HourFormat.current) ?: "—", size = 12, muted = true)
                    entry.loggedOnTime?.let { onTime ->
                        HomeText(stringResource(if (onTime) R.string.home_v8_on_time else R.string.home_v8_late),
                            size = 10, color = if (onTime) SalatPrimary else Color(0xFFB07F1E))
                    }
                }
                if (!stacked) StatusActions(entry, s, onSave = onSave)
                ReminderButton(entry, name, onReminder)
            }
            if (stacked) Row(Modifier.fillMaxWidth().padding(start = 26.dp), horizontalArrangement = Arrangement.End) {
                StatusActions(entry, s, onSave = onSave)
            }
            if (entry.status == PrayerStatus.QAZA_PENDING) TextButton(
                onClick = { onSave(entry.type, PrayerStatus.QAZA_COMPLETED) },
                enabled = !s.isSaving && entry.isMarkable, contentPadding = PaddingValues(horizontal = 26.dp)) {
                HomeText(stringResource(R.string.home_v8_make_up), size = 11, color = SalatPrimary)
            }
        }
    }
    HorizontalDivider(color = SalatOutlineVariant)
}

@Composable private fun ReminderButton(entry: PrayerEntryUi, name: String, onReminder: (PrayerType) -> Unit) {
    IconButton(onClick = { onReminder(entry.type) }, modifier = Modifier.size(48.dp)) {
        Box(Modifier.size(34.dp)
            .background(if (entry.reminderEnabled) SalatPrimary.copy(alpha = .1f) else Color.Transparent, CircleShape)
            .let { if (!entry.reminderEnabled) it.border(1.dp, SalatOutlineVariant, CircleShape) else it },
            contentAlignment = Alignment.Center) {
            Icon(painterResource(if (entry.reminderEnabled) R.drawable.onboarding_sound else R.drawable.home_speaker_off),
                stringResource(if (entry.reminderEnabled) R.string.home_v8_reminder_on else R.string.home_v8_reminder_off, name),
                tint = if (entry.reminderEnabled) SalatPrimary else SalatOnSurfaceVariant, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable private fun StatusActions(entry: PrayerEntryUi, s: HomeUiState,
    hero: Boolean = false, onSave: (PrayerType, PrayerStatus) -> Unit) {
    val name = entry.type.label(s.todayDate, s.jumuahEnabled, s.madhab)
    val enabled = entry.isMarkable && !s.isSaving
    AnimatedContent(entry.status, label = "home-status-${entry.type}") { status ->
        if (status != PrayerStatus.NOT_RECORDED) {
            val label = contextStatusLabel(status, s.gender)
            val clearLabel = stringResource(R.string.home_v8_clear_status, name)
            Surface(onClick = { onSave(entry.type, PrayerStatus.NOT_RECORDED) }, enabled = enabled,
                shape = CircleShape, color = homeStatusColor(status),
                modifier = Modifier.heightIn(min = 48.dp).widthIn(max = 180.dp)
                    .semantics { contentDescription = "$label. $clearLabel" }
                    .testTag("home-status-${if (hero) "hero" else "row"}-${entry.type}")) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val ink = if (status == PrayerStatus.QAZA_COMPLETED) Color(0xFF302408) else Color.White
                    Icon(painterResource(statusIcon(status, s.gender)), null, tint = ink, modifier = Modifier.size(17.dp))
                    HomeText(label, size = 12, color = ink)
                }
            }
        } else Row {
            val offered = if (s.gender == Gender.FEMALE)
                listOf(PrayerStatus.INDIVIDUAL, PrayerStatus.MENSTRUATION, PrayerStatus.QAZA_PENDING)
            else listOf(PrayerStatus.CONGREGATION, PrayerStatus.INDIVIDUAL, PrayerStatus.QAZA_PENDING)
            offered.filter { !hero || it != PrayerStatus.QAZA_PENDING }.forEach { choice ->
                val label = contextStatusLabel(choice, s.gender)
                IconButton(onClick = { onSave(entry.type, choice) }, enabled = enabled,
                    modifier = Modifier.size(48.dp)
                        .testTag("home-pick-${if (hero) "hero" else "row"}-${entry.type}-$choice")) {
                    Box(Modifier.size(if (hero) 40.dp else 34.dp)
                        .background(if (hero) HomeCream else SalatSurface, CircleShape)
                        .let { if (!hero) it.border(1.5.dp, SalatOutlineVariant, CircleShape) else it },
                        contentAlignment = Alignment.Center) {
                        Icon(painterResource(statusIcon(choice, s.gender)), "$name, $label",
                            modifier = Modifier.size(19.dp), tint = if (enabled) homeStatusColor(choice) else SalatOnSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable private fun contextStatusLabel(status: PrayerStatus, gender: Gender?): String = when {
    status == PrayerStatus.INDIVIDUAL && gender == Gender.FEMALE -> stringResource(R.string.home_v8_prayed)
    status == PrayerStatus.MENSTRUATION -> stringResource(R.string.home_v8_excused)
    status == PrayerStatus.QAZA_PENDING -> stringResource(R.string.home_v8_qaza)
    status == PrayerStatus.QAZA_COMPLETED -> stringResource(R.string.home_v8_made_up)
    else -> status.label()
}

private fun statusIcon(status: PrayerStatus, gender: Gender?) = when (status) {
    PrayerStatus.CONGREGATION -> R.drawable.home_status_congregation
    PrayerStatus.INDIVIDUAL -> if (gender == Gender.FEMALE) R.drawable.home_status_prayed else R.drawable.home_status_individual
    PrayerStatus.MENSTRUATION -> R.drawable.home_status_excused
    PrayerStatus.QAZA_PENDING -> R.drawable.home_status_qaza
    PrayerStatus.QAZA_COMPLETED -> R.drawable.home_status_made_up
    PrayerStatus.NOT_RECORDED -> R.drawable.home_status_qaza
}
private fun prayerIcon(type: PrayerType) = when (type) {
    PrayerType.FAJR -> R.drawable.home_prayer_fajr
    PrayerType.DHUHR -> R.drawable.home_prayer_dhuhr
    PrayerType.ASR -> R.drawable.home_prayer_asr
    PrayerType.MAGHRIB -> R.drawable.home_prayer_maghrib
    PrayerType.ISHA -> R.drawable.home_prayer_isha
}

@Composable private fun HomeVerse(verse: VerseInfo) {
    var translated by rememberSaveable(verse.citation) { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        HomeFrieze(Modifier.align(Alignment.TopEnd).size(160.dp, 16.dp).alpha(.18f))
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (verse.arabic.isNotBlank()) Text(verse.arabic, color = SalatOnSurface,
                fontFamily = AmiriQuranFamily, fontSize = 23.sp, lineHeight = 39.sp, textAlign = TextAlign.Center)
            HomeText(verse.citation, size = 12, muted = true, align = TextAlign.Center)
            if (verse.text.isNotBlank()) {
                TextButton(onClick = { translated = !translated }, modifier = Modifier.testTag("home-translation-toggle")) {
                    HomeText(stringResource(R.string.home_v8_translation) + if (translated) " ▴" else " ▾", size = 12, color = SalatPrimary)
                }
                AnimatedVisibility(translated) {
                    HomeText(verse.text, Modifier.fillMaxWidth().testTag("home-translation-text"), size = 13, align = TextAlign.Center)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun StreakSheet(s: HomeUiState, onDismiss: () -> Unit, onAppearance: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SalatSurface) {
        Column(Modifier.fillMaxWidth().testTag("home-streak-sheet").verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            HomeText(stringResource(R.string.home_v8_streaks), size = 20, weight = FontWeight.SemiBold)
            PrayerType.values().forEach { type ->
                val streak = s.streaks[type] ?: StreakInfo(0, 0)
                Row(Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(prayerIcon(type)), null, tint = SalatOnSurfaceVariant, modifier = Modifier.size(20.dp))
                    HomeText(type.label(s.todayDate, s.jumuahEnabled, s.madhab),
                        Modifier.weight(1f).padding(start = 12.dp), size = 15)
                    Column(horizontalAlignment = Alignment.End) {
                        HomeText("${streak.currentStreak}", size = 20, weight = FontWeight.SemiBold)
                        HomeText(stringResource(R.string.home_v8_best, streak.highestStreak), size = 12, muted = true)
                    }
                }
                HorizontalDivider(color = SalatOutlineVariant)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                HomeText(stringResource(R.string.home_v8_all_five), Modifier.weight(1f), size = 13)
                HomeText("${perfectDayStreak(s.streaks)}", size = 13)
            }
            HomeText(stringResource(R.string.home_v8_streak_note), size = 12, muted = true)
            TextButton(onClick = onAppearance, modifier = Modifier.testTag("home-appearance-open")) {
                HomeText(stringResource(R.string.home_v8_appearance), color = SalatPrimary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun AppearanceSheet(value: HomeAppearance, onChange: (HomeAppearance) -> Unit,
    effects: HomeFeedbackEffects, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SalatSurface) {
        Column(Modifier.fillMaxWidth().testTag("home-appearance-sheet").verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            HomeText(stringResource(R.string.home_v8_appearance), size = 20, weight = FontWeight.SemiBold)
            listOf(Triple(R.string.home_v8_badge, value.streakBadge, { checked: Boolean -> onChange(value.copy(streakBadge = checked)) }),
                Triple(R.string.home_v8_photo_info, value.showPhotoInfo, { checked: Boolean -> onChange(value.copy(showPhotoInfo = checked)) }),
                Triple(R.string.home_v8_appreciation, value.showAppreciation, { checked: Boolean -> onChange(value.copy(showAppreciation = checked)) }),
                Triple(R.string.home_v8_confetti, value.confetti, { checked: Boolean -> onChange(value.copy(confetti = checked)) }),
                Triple(R.string.home_v8_voice, value.voice, { checked: Boolean -> onChange(value.copy(voice = checked)) }),
                Triple(R.string.home_v8_chime, value.chime, { checked: Boolean -> onChange(value.copy(chime = checked)) }),
                Triple(R.string.home_v8_vibration, value.vibration, { checked: Boolean -> onChange(value.copy(vibration = checked)) }))
                .forEach { (label, checked, change) ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                        HomeText(stringResource(label), Modifier.weight(1f).padding(end = 12.dp), size = 14)
                        Switch(checked, change)
                    }
                }
            if (value.voice) {
                if (!effects.speechReady) HomeText(stringResource(R.string.home_v8_voice_unavailable), size = 12, muted = true)
                else {
                    var voiceMenu by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { voiceMenu = true }) {
                            HomeText(value.voiceName ?: stringResource(R.string.home_v8_voice_auto), size = 12, color = SalatPrimary)
                        }
                        DropdownMenu(voiceMenu, onDismissRequest = { voiceMenu = false }) {
                            DropdownMenuItem(text = { HomeText(stringResource(R.string.home_v8_voice_auto)) },
                                onClick = { onChange(value.copy(voiceName = null)); voiceMenu = false })
                            effects.voices.forEach { (name, label) -> DropdownMenuItem(text = { HomeText(label) },
                                onClick = { onChange(value.copy(voiceName = name)); voiceMenu = false }) }
                        }
                    }
                }
                HomeText(stringResource(R.string.home_v8_speed), size = 12, muted = true)
                Slider(value.speechRate, { onChange(value.copy(speechRate = it)) }, valueRange = .7f..1.2f, steps = 9)
            }
            val preview = stringResource(R.string.home_v8_well_done)
            TextButton(onClick = { effects.play(preview, true) }) { HomeText(stringResource(R.string.home_v8_preview), color = SalatPrimary) }
            HomeText(stringResource(R.string.home_v8_drag_hint), size = 12, muted = true)
        }
    }
}

@Composable private fun HomeConfetti(id: Long) {
    val progress = remember(id) { Animatable(0f) }
    LaunchedEffect(id) { progress.animateTo(1f, tween(1600)) }
    Canvas(Modifier.fillMaxWidth().height(320.dp)) {
        val t = progress.value
        repeat(30) { i ->
            val angle = (-165 + i * 5) * PI.toFloat() / 180
            val distance = (60 + (i * 37) % 130).dp.toPx()
            val x = size.width * .72f + cos(angle) * distance * t
            val y = size.height * .25f + sin(angle) * distance * t + 150.dp.toPx() * t * t
            drawRect(listOf(HomeGold, HomeCream, Color(0xFF3E9A95), Color.White)[i % 4].copy(alpha = 1f - t),
                topLeft = Offset(x, y), size = androidx.compose.ui.geometry.Size(4.dp.toPx(), 7.dp.toPx()))
        }
    }
}

@Composable internal fun HomeText(text: String, modifier: Modifier = Modifier, size: Int = 14,
    muted: Boolean = false, color: Color = Color.Unspecified, weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Start) {
    Text(text, modifier, color = if (color != Color.Unspecified) color else if (muted) SalatOnSurfaceVariant else SalatOnSurface,
        fontFamily = HomeBodyFont, fontWeight = weight, fontSize = size.sp, lineHeight = (size * 1.5f).sp, textAlign = align)
}
