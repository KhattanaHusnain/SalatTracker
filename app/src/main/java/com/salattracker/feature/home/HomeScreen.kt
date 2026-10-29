package com.salattracker.feature.home

import android.animation.ValueAnimator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.*
import com.salattracker.domain.model.*
import com.salattracker.domain.usecase.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.*

private val V27HeroMuted = Color.White.copy(alpha = .78f)
private val V27Divider: Color @Composable get() = SalatOutlineVariant
private val V27Muted: Color @Composable get() = SalatOnSurfaceVariant

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onPrayerCardClick: (PrayerType) -> Unit,
    onSheetDismiss: () -> Unit,
    onStatusSave: (PrayerStatus) -> Unit,
    onQazaClick: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onQuickStatusSave: (PrayerType, PrayerStatus) -> Unit = { type, _ -> onPrayerCardClick(type) },
    onPastStatusSave: (LocalDate, PrayerType, PrayerStatus) -> Unit = { date, _, _ -> onDayClick(date) },
    onReminderToggle: (PrayerType) -> Unit = {},
    onMenuClick: () -> Unit = {},
    hostedBelowSharedTopBar: Boolean = false,
    onLocationClick: () -> Unit = {},
    onDonateClick: () -> Unit = {},
    onCatchUpClick: () -> Unit = {},
    onAppearanceChanged: (HomeAppearance) -> Unit = {},
    onErrorDismiss: () -> Unit = {},
) {
    var showStreaks by rememberSaveable { mutableStateOf(false) }
    var selectedDay by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var translated by rememberSaveable { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<HomeSaveFeedback?>(null) }
    var handledFeedback by rememberSaveable { mutableStateOf<Long?>(null) }
    var flashPrayer by remember { mutableStateOf<PrayerType?>(null) }
    val rowRequesters = remember { PrayerType.values().associateWith { BringIntoViewRequester() } }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val feedbackEffects = rememberHomeFeedbackEffects(uiState.appearance, configureVoice = false)
    val feedbackText = uiState.feedback?.let { v27AppreciationText(it, uiState) }

    LaunchedEffect(uiState.errorRes) {
        uiState.errorRes?.let { snackbar.showSnackbar(context.getString(it)); onErrorDismiss() }
    }
    LaunchedEffect(uiState.feedback?.id) {
        val saved = uiState.feedback ?: return@LaunchedEffect
        if (handledFeedback == saved.id) return@LaunchedEffect
        handledFeedback = saved.id
        feedback = saved
        val celebrate = saved.onTime && saved.status in listOf(PrayerStatus.CONGREGATION, PrayerStatus.INDIVIDUAL)
        feedbackEffects.play(feedbackText.orEmpty(), celebrate)
        delay(3600)
        if (feedback?.id == saved.id) feedback = null
    }

    HomeDesign {
        Box(Modifier.fillMaxSize().background(SalatBackground).testTag("home-screen")) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("home-scroll")) {
                V27Hero(
                    uiState,
                    onOpenMenu = onMenuClick,
                    hostedBelowSharedTopBar = hostedBelowSharedTopBar,
                    onLocation = onLocationClick,
                    onSave = onQuickStatusSave,
                    onOpenStreaks = { showStreaks = true },
                    onAppearanceChanged = onAppearanceChanged,
                    feedbackId = uiState.feedback?.id,
                )
                if (!uiState.appearance.streakBadge) {
                    V27StreakDisplay(uiState, badge = false, onChange = onAppearanceChanged) { showStreaks = true }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    if (uiState.isLoading) V27Loading()
                    else {
                        V27WeekHistory(uiState) { date ->
                            if (date == uiState.todayDate) {
                                val target = uiState.currentPrayer ?: uiState.nextPrayer?.type
                                if (target != null) scope.launch {
                                    rowRequesters.getValue(target).bringIntoView()
                                    flashPrayer = target
                                    delay(1500)
                                    if (flashPrayer == target) flashPrayer = null
                                }
                            } else selectedDay = date
                        }
                        V27TodayHeading(uiState)
                        if (v27RecentUnrecordedDays(uiState) >= 3) {
                            Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onCatchUpClick)
                                .padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                HomeText(stringResource(R.string.home_v8_catch_up), size = 13, color = SalatPrimary)
                            }
                        }
                        uiState.prayers.forEach { entry ->
                            V27PrayerRow(
                                entry = entry,
                                state = uiState,
                                flash = flashPrayer == entry.type,
                                modifier = Modifier.bringIntoViewRequester(rowRequesters.getValue(entry.type)),
                                onSave = onQuickStatusSave,
                                onReminder = onReminderToggle,
                                onDetails = { onPrayerCardClick(entry.type) },
                                onOpenStreaks = { showStreaks = true },
                            )
                        }
                        uiState.verse?.let { V27Verse(it, translated) { translated = !translated } }
                        V27DonateRow(onDonateClick)
                    }
                }
            }
            feedback?.let { saved ->
                if (uiState.appearance.showAppreciation && saved.status != PrayerStatus.NOT_RECORDED) {
                    V27AppreciationToast(v27AppreciationText(saved, uiState), saved.status == PrayerStatus.MENSTRUATION)
                }
                if (uiState.appearance.confetti && saved.onTime &&
                    saved.status in listOf(PrayerStatus.CONGREGATION, PrayerStatus.INDIVIDUAL) &&
                    ValueAnimator.areAnimatorsEnabled()) V27Confetti(saved.id)
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
        if (showStreaks) V27StreakSheet(uiState) { showStreaks = false }
        selectedDay?.let { day -> V27DaySheet(day, uiState, onPastStatusSave) { selectedDay = null } }
        uiState.sheetArgs?.let { args ->
            PrayerStatusBottomSheet(
                args = args, isSaving = uiState.isSaving, onDismiss = onSheetDismiss,
                onSave = onStatusSave, gender = uiState.gender,
            )
        }
    }
}

@Composable
private fun V27Hero(
    state: HomeUiState,
    onOpenMenu: () -> Unit,
    hostedBelowSharedTopBar: Boolean,
    onLocation: () -> Unit,
    onSave: (PrayerType, PrayerStatus) -> Unit,
    onOpenStreaks: () -> Unit,
    onAppearanceChanged: (HomeAppearance) -> Unit,
    feedbackId: Long?,
) {
    val locale = LocalConfiguration.current.locales[0]
    val date = state.now.toLocalDate()
    val hijriAdjustment = LocalHijriAdjustmentDays.current
    val hijri = remember(date, hijriAdjustment) { date.toHijriOrNull(hijriAdjustment) }
    val current = state.prayers.firstOrNull { it.type == state.currentPrayer }
    val next = state.nextPrayer
    val type = current?.type ?: next?.type
    val displayDate = state.todayDate.plusDays(if (current == null && next?.isTomorrow == true) 1 else 0)
    val name = type?.label(displayDate, state.jumuahEnabled, state.madhab)
    val remaining = current?.windowEnd?.let { Duration.between(state.now, it).toMinutes().coerceAtLeast(0) }
        ?: state.minutesUntilNext
    val progress = current?.let { entry ->
        val start = entry.time?.let(state.todayDate::atTime)
        val end = entry.windowEnd
        if (start != null && end != null) {
            val full = Duration.between(start, end).seconds.coerceAtLeast(1)
            (Duration.between(start, state.now).seconds.toFloat() / full).coerceIn(0f, 1f)
        } else 0f
    } ?: 0f
    val status = current?.status ?: PrayerStatus.NOT_RECORDED
    val filled = status != PrayerStatus.NOT_RECORDED && status != PrayerStatus.QAZA_PENDING

    Box(Modifier.fillMaxWidth().testTag("home-hero")) {
        Image(painterResource(R.drawable.home_mosque), null, Modifier.matchParentSize(),
            contentScale = ContentScale.Crop, alignment = BiasAlignment(0f, -.72f))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to Color(0xB8064E52), .55f to Color(0x6B064E52), 1f to Color(0xDB053C40),
        )))
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (hostedBelowSharedTopBar) Modifier else Modifier.statusBarsPadding())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 26.dp),
        ) {
            if (!hostedBelowSharedTopBar || state.appearance.streakBadge) {
                Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!hostedBelowSharedTopBar) {
                        IconButton(onClick = onOpenMenu, modifier = Modifier.size(40.dp).offset(x = (-10).dp)
                            .testTag("home-menu-open")) {
                            Icon(painterResource(R.drawable.ic_menu), stringResource(R.string.home_v8_menu),
                                tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (state.appearance.streakBadge) V27StreakDisplay(state, true, onAppearanceChanged, onOpenStreaks)
                }
            }
            val enlarged = LocalDensity.current.fontScale > 1.15f
            BoxWithConstraints(Modifier.fillMaxWidth().heightIn(min = 196.dp)) {
                val tallHero = enlarged || maxWidth < 340.dp
                val heroLanguage = locale.language
                val heroHeight = when {
                    heroLanguage == "ur" && enlarged -> 294.dp
                    heroLanguage in setOf("ar", "fr") && enlarged -> 244.dp
                    maxWidth < 340.dp -> 260.dp
                    tallHero -> 226.dp
                    else -> 204.dp
                }
                val captionWidth = (maxWidth - 162.dp).coerceIn(104.dp, 168.dp)
                Spacer(Modifier.height(heroHeight))
                Column(Modifier.align(Alignment.TopStart).padding(top = 24.dp).width(captionWidth)
                    .testTag("home-date-block")) {
                    V27FixedText(hijri?.fullLabel() ?: date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale)),
                        size = 27, line = 32, color = Color.White, fontFamily = HomeTitleFont,
                        weight = FontWeight.SemiBold)
                    V27FixedText(date.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale)),
                        Modifier.padding(top = 4.dp), size = 13, line = 18, color = V27HeroMuted)
                    Row(Modifier.heightIn(min = 40.dp).clickable(role = Role.Button, onClick = onLocation),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.onboarding_pin), null, tint = HomeGold, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        V27FixedText(state.locationLabel?.substringBefore(',') ?: stringResource(R.string.home_v8_choose_city),
                            Modifier.testTag("home-location-city"), size = 12, line = 17,
                            color = V27HeroMuted, maxLines = 2)
                    }
                }
                Box(Modifier.align(Alignment.TopEnd)
                    .size(150.dp, if (tallHero) 226.dp else 196.dp)
                    .testTag("home-countdown")) {
                    HomeArch(if (filled) 1f else progress, Modifier.matchParentSize(), filled,
                        status == PrayerStatus.MENSTRUATION, status == PrayerStatus.CONGREGATION)
                    V27HeroCenter(state, current, name, remaining, filled)
                    when {
                        current == null && name != null -> HomeText(stringResource(R.string.home_v8_opens, name),
                            Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 18.dp),
                            size = 11, color = V27HeroMuted, align = TextAlign.Center)
                        current != null && status == PrayerStatus.NOT_RECORDED -> V27HeroPickActions(current, state, onSave)
                        current != null && v27HeroFigure(status, state.gender) != null -> V27HeroFigure(
                            current, status, state.gender, feedbackId, onSave)
                        current != null -> V27HeroUndoStatement(current, status, state.gender, onSave)
                    }
                }
                V27PhotoInfo(state.appearance.showPhotoInfo, Modifier.align(Alignment.BottomStart).width(captionWidth),
                    { onAppearanceChanged(state.appearance.copy(showPhotoInfo = false)) },
                    { onAppearanceChanged(state.appearance.copy(showPhotoInfo = true)) })
            }
        }
    }
}

@Composable
private fun BoxScope.V27HeroCenter(state: HomeUiState, current: PrayerEntryUi?, name: String?, remaining: Long, filled: Boolean) {
    val isUrdu = LocalConfiguration.current.locales[0].language == "ur"
    Column(Modifier.align(Alignment.TopCenter).fillMaxWidth()
        .padding(top = if (filled) 42.dp else 52.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.isLoading) CircularProgressIndicator(Modifier.size(26.dp), color = HomeGold, strokeWidth = 2.dp)
        else if (name == null) HomeText(stringResource(R.string.home_v8_times_unavailable), size = 12,
            color = Color.White, align = TextAlign.Center)
        else if (current == null) {
            HomeText(stringResource(R.string.home_v8_begins, name), size = 11, color = V27HeroMuted, align = TextAlign.Center)
            V27FixedText(v27Remaining(remaining), Modifier.testTag("home-countdown-value"),
                size = when (LocalConfiguration.current.locales[0].language) { "ar" -> 26; "ur" -> 20; else -> 34 },
                line = when (LocalConfiguration.current.locales[0].language) { "ur" -> 34; else -> 40 },
                color = Color.White, align = TextAlign.Center, maxLines = 1, softWrap = false)
        } else if (!filled) {
            V27FixedText(name, size = 26, line = 29, color = Color.White,
                fontFamily = HomeTitleFont, weight = FontWeight.SemiBold, align = TextAlign.Center)
            V27FixedText(v27Remaining(remaining), Modifier.testTag("home-countdown-value"),
                size = when (LocalConfiguration.current.locales[0].language) { "ar" -> 26; "ur" -> 20; else -> 34 },
                line = when (LocalConfiguration.current.locales[0].language) { "ur" -> 34; else -> 40 },
                color = Color.White, align = TextAlign.Center, maxLines = 1, softWrap = false)
            V27FixedText(stringResource(R.string.home_v8_left),
                Modifier.heightIn(min = if (isUrdu) 32.dp else 0.dp), size = 12,
                line = if (isUrdu) 28 else 18,
                color = V27HeroMuted, softWrap = false,
                includeFontPadding = isUrdu)
        } else {
            V27FixedText(name, size = 25, line = 28, color = Color.White,
                fontFamily = HomeTitleFont, weight = FontWeight.SemiBold, align = TextAlign.Center)
            HomeText(v27StatusLabel(current.status, state.gender),
                size = if (current.status == PrayerStatus.CONGREGATION) 15 else 19,
                color = if (current.status == PrayerStatus.MENSTRUATION) Color(0xFFF7A6D6) else Color(0xFFF2C86B),
                weight = FontWeight.SemiBold, align = TextAlign.Center)
            if (current.type.ordinal > 0 && state.nextPrayer != null) {
                val nextName = state.nextPrayer.type.label(state.todayDate, state.jumuahEnabled, state.madhab)
                V27FixedText(stringResource(R.string.home_v27_next_in, nextName, v27Remaining(state.minutesUntilNext)),
                    Modifier.padding(top = 2.dp, start = 8.dp, end = 8.dp), size = 10,
                    line = if (LocalConfiguration.current.locales[0].language == "ur") 18 else 14,
                    color = V27HeroMuted, align = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

@Composable
private fun BoxScope.V27HeroPickActions(entry: PrayerEntryUi, state: HomeUiState,
    onSave: (PrayerType, PrayerStatus) -> Unit) {
    val scope = rememberCoroutineScope()
    var pending by remember(entry.type, entry.status) { mutableStateOf<PrayerStatus?>(null) }
    val choices = if (state.gender == Gender.FEMALE) listOf(PrayerStatus.INDIVIDUAL, PrayerStatus.MENSTRUATION)
        else listOf(PrayerStatus.CONGREGATION, PrayerStatus.INDIVIDUAL)
    Row(Modifier.align(Alignment.BottomCenter).height(56.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom) {
        choices.forEach { choice ->
            val chosen = pending == choice
            val hidden = pending != null && !chosen
            val description = v27StatusLabel(choice, state.gender)
            val animAlpha by animateFloatAsState(
                targetValue = if (hidden) 0f else 1f,
                animationSpec = tween(260, easing = FastOutSlowInEasing),
                label = "hero-pick-alpha",
            )
            val animScale by animateFloatAsState(
                targetValue = if (chosen) 1.35f else if (hidden) .55f else 1f,
                animationSpec = tween(260, easing = FastOutSlowInEasing),
                label = "hero-pick-scale",
            )
            Box(Modifier.size(48.dp).graphicsLayer {
                alpha = animAlpha
                scaleX = animScale
                scaleY = animScale
            }.testTag("home-pick-hero-${entry.type}-$choice")
                .semantics { role = Role.Button; contentDescription = description }
                .clickable(enabled = entry.isMarkable && !state.isSaving && pending == null) {
                    pending = choice
                    scope.launch { delay(340); onSave(entry.type, choice) }
                }, contentAlignment = Alignment.Center) {
                V27StatusArtwork(choice, state.gender, false, true, Modifier.size(36.dp))
            }
        }
    }
}

@Composable
private fun BoxScope.V27HeroFigure(entry: PrayerEntryUi, status: PrayerStatus, gender: Gender?, feedbackId: Long?,
    onSave: (PrayerType, PrayerStatus) -> Unit) {
    val resource = v27HeroFigure(status, gender) ?: return
    val width = when (resource) {
        R.drawable.home_status_cg_full -> 39.dp
        R.drawable.home_status_ci_full -> 28.dp
        else -> 26.dp
    }
    val animEnabled = ValueAnimator.areAnimatorsEnabled()
    val scale = remember(resource, feedbackId) { Animatable(if (feedbackId == null || !animEnabled) 1f else .02f) }
    var hint by remember(resource, feedbackId) { mutableStateOf(feedbackId != null) }
    LaunchedEffect(resource, feedbackId) {
        if (feedbackId != null) {
            if (animEnabled) {
                scale.animateTo(1f, tween(800, easing = CubicBezierEasing(.22f, 1f, .36f, 1f)))
            } else {
                scale.snapTo(1f)
            }
            delay(2400)
            hint = false
        }
    }
    Box(Modifier.align(Alignment.BottomCenter).height(56.dp).wrapContentWidth()
        .testTag("home-status-hero-${entry.type}"), contentAlignment = Alignment.BottomCenter) {
        Image(painterResource(resource), v27StatusLabel(status, gender), Modifier.width(width).height(56.dp)
            .testTag("home-figure-${entry.type}-$status")
            .semantics { stateDescription = if (scale.value < .99f) "figure-grow" else "figure-settled" }
            .graphicsLayer { scaleY = scale.value; transformOrigin = TransformOrigin(.5f, 1f) }
            .clickable(role = Role.Button) { onSave(entry.type, PrayerStatus.NOT_RECORDED) })
        AnimatedVisibility(hint, Modifier.align(Alignment.BottomEnd).offset(x = 22.dp, y = (-3).dp),
            enter = fadeIn(tween(500, 900)) + scaleIn(tween(500, 900)), exit = fadeOut(tween(450))) {
            Surface(shape = CircleShape, color = Color(0x9904282B), border = BorderStroke(1.dp, Color(0xBFF2C86B)),
                modifier = Modifier.size(20.dp).testTag("home-undo-hint")) {
                Icon(painterResource(R.drawable.home_undo), null, tint = Color.White, modifier = Modifier.padding(4.dp))
            }
        }
    }
}

@Composable
private fun BoxScope.V27HeroUndoStatement(entry: PrayerEntryUi, status: PrayerStatus, gender: Gender?,
    onSave: (PrayerType, PrayerStatus) -> Unit) {
    Row(Modifier.align(Alignment.BottomCenter).height(42.dp).clickable(role = Role.Button) {
        onSave(entry.type, PrayerStatus.NOT_RECORDED)
    }.testTag("home-status-hero-${entry.type}"), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        V27StatusArtwork(status, gender, true, false, Modifier.size(16.dp))
        if (status == PrayerStatus.QAZA_PENDING) HomeText(v27StatusLabel(status, gender) + " · ",
            size = 12, color = Color.White, weight = FontWeight.SemiBold)
        Icon(painterResource(R.drawable.home_undo), null, tint = Color.White, modifier = Modifier.size(14.dp))
        HomeText(stringResource(R.string.home_v27_undo), size = 12, color = Color.White)
    }
}

@Composable
private fun V27PhotoInfo(shown: Boolean, modifier: Modifier, onHide: () -> Unit, onShow: () -> Unit) {
    if (!shown) Box(modifier.height(48.dp), contentAlignment = Alignment.BottomStart) {
        IconButton(onClick = onShow, modifier = Modifier.size(30.dp).testTag("home-photo-show")
            .background(Color(0x9904282B), CircleShape)) {
            Icon(painterResource(R.drawable.onboarding_pin), stringResource(R.string.home_v8_about_photo),
                tint = HomeGold, modifier = Modifier.size(15.dp))
        }
    } else {
        var drag by remember { mutableStateOf(Offset.Zero) }
        Row(modifier.testTag("home-photo-info").offset { IntOffset(drag.x.roundToInt(), drag.y.roundToInt()) }
            .pointerInput(Unit) { detectDragGestures(
                onDrag = { change, delta -> change.consume(); drag += delta },
                onDragEnd = { if (drag.getDistance() > 60.dp.toPx()) onHide(); drag = Offset.Zero },
                onDragCancel = { drag = Offset.Zero },
            ) }.background(Color(0x9904282B), RoundedCornerShape(10.dp))
            .padding(start = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.Top) {
            Icon(painterResource(R.drawable.onboarding_pin), null, tint = HomeGold,
                modifier = Modifier.size(13.dp).padding(top = 1.dp))
            Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f)) {
                V27FixedText(stringResource(R.string.home_v8_photo_title), size = 11, line = 16,
                    color = Color.White, weight = FontWeight.SemiBold)
                V27FixedText(stringResource(R.string.home_v8_photo_detail), size = 10, line = 15,
                    color = Color.White.copy(alpha = .85f))
            }
            IconButton(onClick = onHide, modifier = Modifier.size(28.dp).testTag("home-photo-hide")) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.home_v8_hide_photo),
                    tint = Color.White, modifier = Modifier.size(11.dp))
            }
        }
    }
}

@Composable
private fun V27StreakDisplay(state: HomeUiState, badge: Boolean, onChange: (HomeAppearance) -> Unit, onOpen: () -> Unit) {
    val top = state.streaks.maxByOrNull { it.value.currentStreak }
    val count = top?.value?.currentStreak ?: 0
    val prayer = top?.key?.label(state.todayDate, state.jumuahEnabled, state.madhab)
    var drag by remember { mutableStateOf(Offset.Zero) }
    val action = stringResource(if (badge) R.string.home_v8_move_band else R.string.home_v8_move_badge)
    val interaction = remember { MutableInteractionSource() }
    val base = Modifier.offset { IntOffset(drag.x.roundToInt(), drag.y.roundToInt()) }
        .semantics { customActions = listOf(CustomAccessibilityAction(action) {
            onChange(state.appearance.copy(streakBadge = !badge)); true
        }) }.pointerInput(badge) { detectDragGestures(
            onDrag = { change, delta -> change.consume(); drag += delta },
            onDragEnd = {
                if ((!badge && drag.y < -36.dp.toPx()) || (badge && drag.y > 36.dp.toPx()))
                    onChange(state.appearance.copy(streakBadge = !badge))
                drag = Offset.Zero
            }, onDragCancel = { drag = Offset.Zero },
        ) }.clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onOpen)
        .testTag("home-streak")

    if (badge) {
        HomePlaque(base.size(42.dp, 52.dp)) {
            val density = LocalDensity.current
            val countSize = with(density) { 18.dp.toSp() }
            val countLine = with(density) { 28.dp.toSp() }
            val daySize = with(density) { 7.dp.toSp() }
            val dayLine = with(density) { 12.dp.toSp() }
            Icon(painterResource(R.drawable.home_flame), null, tint = HomeCream,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = 8.dp).size(10.dp))
            Text("$count", Modifier.align(Alignment.TopCenter).offset(y = 12.dp),
                fontFamily = HomeSerif, fontSize = countSize, lineHeight = countLine,
                fontWeight = FontWeight.Bold, color = Color(0xFF0B4F53), maxLines = 1,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
            Text(stringResource(R.string.home_v27_day), Modifier.align(Alignment.TopCenter).offset(y = 37.dp),
                color = Color(0xFF0B4F53), fontFamily = HomeBodyFont, fontSize = daySize,
                lineHeight = dayLine, fontWeight = FontWeight.SemiBold, maxLines = 1,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
        }
    } else {
        Row(base.fillMaxWidth().background(SalatSecondaryContainer).padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            V27GripDots()
            HomePlaque(Modifier.size(24.dp, 30.dp)) {
                Icon(painterResource(R.drawable.home_flame), null, tint = HomeCream,
                    modifier = Modifier.padding(top = 7.dp).size(11.dp))
            }
            HomeText(if (count > 0 && prayer != null) stringResource(R.string.home_v27_streak_band, prayer, count)
                else stringResource(R.string.home_v8_streak_start), Modifier.weight(1f), size = 13,
                color = SalatOnSurface, weight = FontWeight.Medium)
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = V27Muted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable private fun V27GripDots() = Canvas(Modifier.size(8.dp, 18.dp)) {
    for (y in listOf(2f, 8f, 14f)) drawCircle(Color(0xFFB9A06A), 1.3.dp.toPx(), Offset(size.width / 2, y.dp.toPx()))
}

@Composable
private fun V27WeekHistory(state: HomeUiState, onDay: (LocalDate) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val dates = (6 downTo 0).map { state.todayDate.minusDays(it.toLong()) }
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        HomeText(stringResource(R.string.home_v8_week).uppercase(locale), size = 11, muted = true, weight = FontWeight.SemiBold)
        HomeText(" · ${dates.first().format(DateTimeFormatter.ofPattern("d", locale))}–${state.todayDate.format(DateTimeFormatter.ofPattern("d MMM", locale))}",
            size = 11, muted = true, weight = FontWeight.Medium)
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        dates.forEach { date ->
            val today = date == state.todayDate
            val marks = if (today) state.prayers.associate { it.type to it.status } else state.lastSevenDays[date].orEmpty()
            Column(Modifier.weight(1f).testTag("home-day-$date").clickable(role = Role.Button) { onDay(date) }
                .padding(vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(38.dp, 42.dp), contentAlignment = Alignment.TopCenter) {
                    V27WeekRing(marks, today, Modifier.size(34.dp))
                    if (today) Box(Modifier.align(Alignment.BottomCenter).offset(y = (-1).dp).size(9.dp)
                        .rotate(45f).background(SalatSurface).border(1.dp, V27Divider))
                }
                HomeText(date.format(DateTimeFormatter.ofPattern("EEEEE", locale)), size = 11,
                    color = if (today) SalatPrimary else V27Muted,
                    weight = if (today) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

@Composable private fun V27WeekRing(marks: Map<PrayerType, PrayerStatus>, today: Boolean, modifier: Modifier) {
    val arcColors = PrayerType.values().map { homeStatusColor(marks[it] ?: PrayerStatus.NOT_RECORDED) }
    val todayColor = SalatPrimary
    Canvas(modifier) {
        val stroke = 4.5.dp.toPx()
        arcColors.forEachIndexed { index, color ->
            drawArc(color, -88f + index * 72f, 58.8f, false,
                Offset(stroke / 2, stroke / 2), androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke))
        }
        if (today) drawCircle(todayColor, size.minDimension / 2 - .5.dp.toPx(), style = Stroke(1.dp.toPx()))
    }
}

@Composable private fun V27TodayHeading(state: HomeUiState) {
    val completed = state.prayers.count { it.status != PrayerStatus.NOT_RECORDED && it.status != PrayerStatus.QAZA_PENDING }
    val dividerColor = V27Divider
    Row(Modifier.fillMaxWidth().drawBehind { drawLine(dividerColor, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx()) }
        .padding(top = 11.dp, bottom = 3.dp), verticalAlignment = Alignment.Bottom) {
        Text(stringResource(R.string.home_v27_today), fontFamily = HomeTitleFont, fontSize = 22.sp,
            lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, color = SalatOnSurface, modifier = Modifier.weight(1f))
        HomeText(stringResource(R.string.home_v27_prayed_count, completed), size = 12, muted = true)
    }
}

@Composable
private fun V27PrayerRow(entry: PrayerEntryUi, state: HomeUiState, flash: Boolean, modifier: Modifier,
    onSave: (PrayerType, PrayerStatus) -> Unit, onReminder: (PrayerType) -> Unit,
    onDetails: () -> Unit, onOpenStreaks: () -> Unit) {
    val current = entry.type == state.currentPrayer
    val stacked = LocalDensity.current.fontScale > 1.18f || LocalConfiguration.current.screenWidthDp < 340
    val flashAlpha by animateFloatAsState(if (flash) .28f else 0f, tween(if (flash) 120 else 900), label = "row-flash")
    CompactPrayerRow(
        prayerType = entry.type,
        enabled = entry.isMarkable,
        isActive = current,
        highlightAlpha = flashAlpha,
        highlightColor = HomeGold,
        accentColor = SalatPrimary,
        dividerColor = V27Divider,
        iconTint = V27Muted,
        modifier = modifier.testTag("home-row-${entry.type}"),
        contentModifier = Modifier.clickable(enabled = entry.isMarkable, onClick = onDetails),
        content = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HomeText(entry.type.label(state.todayDate, state.jumuahEnabled, state.madhab),
                        Modifier.weight(1f, fill = false), size = 15, weight = FontWeight.Medium)
                    V27PrayerStreak(entry, state, onOpenStreaks)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HomeText(entry.time?.formatAppTime(LocalUse24HourFormat.current) ?: "—", size = 12, muted = true)
                    entry.loggedOnTime?.let { onTime ->
                        HomeText(" · " + stringResource(if (onTime) R.string.home_v8_on_time else R.string.home_v8_late),
                            size = 11, color = if (onTime) SalatPrimary else Color(0xFFB07F1E))
                    }
                }
                if (entry.status == PrayerStatus.QAZA_PENDING) TextButton(
                    onClick = { onSave(entry.type, PrayerStatus.QAZA_COMPLETED) },
                    enabled = !state.isSaving && entry.isMarkable,
                    modifier = Modifier.height(30.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    HomeText(stringResource(R.string.home_v8_make_up), size = 11, color = SalatPrimary)
                }
        },
        actions = {
            if (!stacked) V27RowStatusActions(entry, state, onSave)
            V27ReminderButton(entry, state, onReminder)
        },
        footer = {
            if (stacked) Row(Modifier.fillMaxWidth().padding(start = 26.dp), horizontalArrangement = Arrangement.End) {
                V27RowStatusActions(entry, state, onSave)
            }
        },
    )
}

@Composable private fun V27PrayerStreak(entry: PrayerEntryUi, state: HomeUiState, onOpen: () -> Unit) {
    val count = state.streaks[entry.type]?.currentStreak ?: 0
    if (count > 0) Row(Modifier.clickable(role = Role.Button, onClick = onOpen).padding(start = 5.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.home_flame), null, tint = HomeGold,
            modifier = Modifier.size(13.dp).alpha(if (entry.loggedOnTime == true) 1f else .65f))
        HomeText("$count", size = 12, muted = true)
    }
}

@Composable private fun V27RowStatusActions(entry: PrayerEntryUi, state: HomeUiState,
    onSave: (PrayerType, PrayerStatus) -> Unit) {
    CompactPrayerStatusActions(
        prayerType = entry.type,
        status = entry.status,
        gender = state.gender,
        enabled = entry.isMarkable && !state.isSaving,
        tagPrefix = "home",
        onStatusSelected = { status -> onSave(entry.type, status) },
    )
}

@Composable private fun V27ReminderButton(entry: PrayerEntryUi, state: HomeUiState, onReminder: (PrayerType) -> Unit) {
    val name = entry.type.label(state.todayDate, state.jumuahEnabled, state.madhab)
    Box(Modifier.size(34.dp).clip(CircleShape)
        .background(if (entry.reminderEnabled) SalatPrimary.copy(alpha = .1f) else Color.Transparent)
        .then(if (!entry.reminderEnabled) Modifier.border(1.5.dp, V27Divider, CircleShape) else Modifier)
        .clickable(role = Role.Button) { onReminder(entry.type) }, contentAlignment = Alignment.Center) {
        Icon(painterResource(if (entry.reminderEnabled) R.drawable.onboarding_sound else R.drawable.home_speaker_off),
            stringResource(if (entry.reminderEnabled) R.string.home_v8_reminder_on else R.string.home_v8_reminder_off, name),
            tint = if (entry.reminderEnabled) SalatPrimary else V27Muted, modifier = Modifier.size(19.dp))
    }
}

@Composable private fun V27StatusArtwork(status: PrayerStatus, gender: Gender?, selected: Boolean, hero: Boolean,
    modifier: Modifier, vectorTint: Color = homeStatusColor(status)) {
    val raster = v27StatusRaster(status, gender, selected)
    if (raster != null) Image(painterResource(raster), null, modifier,
        colorFilter = if (hero && !selected) ColorFilter.tint(Color.White) else null)
    else Icon(painterResource(v27StatusIcon(status, gender)), null,
        tint = if (hero) Color.White else vectorTint, modifier = modifier)
}

private fun v27StatusRaster(status: PrayerStatus, gender: Gender?, selected: Boolean): Int? = when (status) {
    PrayerStatus.CONGREGATION -> if (selected) R.drawable.home_status_cg_selected else R.drawable.home_status_cg_outline
    PrayerStatus.INDIVIDUAL -> if (gender == Gender.FEMALE) {
        if (selected) R.drawable.home_status_prayed_selected else R.drawable.home_status_prayed_outline
    } else if (selected) R.drawable.home_status_ci_selected else R.drawable.home_status_ci_outline
    else -> null
}

private fun v27HeroFigure(status: PrayerStatus, gender: Gender?): Int? = when (status) {
    PrayerStatus.CONGREGATION -> R.drawable.home_status_cg_full
    PrayerStatus.INDIVIDUAL -> if (gender == Gender.FEMALE) R.drawable.home_status_prayed_full else R.drawable.home_status_ci_full
    else -> null
}

@Composable private fun V27Verse(verse: VerseInfo, translated: Boolean, onToggle: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        HomeFrieze(Modifier.align(Alignment.TopEnd).size(160.dp, 16.dp).alpha(.18f))
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (verse.arabic.isNotBlank()) Text(verse.arabic, color = SalatOnSurface, fontFamily = AmiriQuranFamily,
                fontSize = 22.sp, lineHeight = 37.sp, textAlign = TextAlign.Center)
            HomeText(verse.citation, size = 12, muted = true, align = TextAlign.Center)
            if (verse.text.isNotBlank()) {
                HomeText(stringResource(R.string.home_v8_translation) + if (translated) " ▴" else " ▾",
                    Modifier.padding(start = 8.dp).clickable(role = Role.Button, onClick = onToggle)
                        .testTag("home-translation-toggle"), size = 12, color = SalatPrimary)
                AnimatedVisibility(translated) {
                    HomeText(verse.text, Modifier.fillMaxWidth().padding(top = 4.dp).testTag("home-translation-text"),
                        size = 13, align = TextAlign.Center)
                }
            }
        }
    }
}

@Composable private fun V27DonateRow(onDonate: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onDonate).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(painterResource(R.drawable.ic_heart), null, tint = SalatPrimary, modifier = Modifier.size(18.dp))
        HomeText(stringResource(R.string.home_v8_donate), Modifier.weight(1f), size = 13, muted = true)
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = V27Muted, modifier = Modifier.size(16.dp))
    }
    HorizontalDivider(color = V27Divider)
    Spacer(Modifier.height(8.dp))
}

@Composable private fun V27Loading() {
    LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 20.dp), color = SalatPrimary)
    repeat(5) { Box(Modifier.fillMaxWidth().height(58.dp).padding(vertical = 7.dp)
        .background(V27Divider.copy(alpha = .45f), RoundedCornerShape(8.dp))) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun V27DaySheet(date: LocalDate, state: HomeUiState,
    onSave: (LocalDate, PrayerType, PrayerStatus) -> Unit, onDismiss: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val marks = state.lastSevenDays[date].orEmpty()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SalatSurface) {
        Column(Modifier.fillMaxWidth().testTag("home-day-sheet").padding(horizontal = 20.dp).padding(bottom = 18.dp)) {
            HomeText(date.format(DateTimeFormatter.ofPattern("EEE · d MMM", locale)), size = 16, weight = FontWeight.SemiBold)
            PrayerType.values().forEach { type ->
                val status = marks[type] ?: PrayerStatus.NOT_RECORDED
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(painterResource(type.iconRes), null, tint = V27Muted, modifier = Modifier.size(18.dp))
                    Column(Modifier.weight(1f)) {
                        HomeText(type.label(date, state.jumuahEnabled, state.madhab), size = 15, weight = FontWeight.Medium)
                        if (status == PrayerStatus.NOT_RECORDED) HomeText(stringResource(R.string.home_v27_not_recorded), size = 12, muted = true)
                    }
                    if (status == PrayerStatus.NOT_RECORDED) Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        v27OfferedStatuses(state.gender).forEach { choice ->
                            Box(Modifier.size(34.dp).border(1.5.dp, V27Divider, CircleShape)
                                .clickable { onSave(date, type, choice) }, contentAlignment = Alignment.Center) {
                                V27StatusArtwork(choice, state.gender, false, false, Modifier.size(26.dp))
                            }
                        }
                    } else Column(horizontalAlignment = Alignment.End) {
                        val statusInk = compactPrayerStatusContentColor(status)
                        Row(Modifier.height(34.dp).clip(CircleShape).background(homeStatusColor(status))
                            .clickable { onSave(date, type, if (status == PrayerStatus.QAZA_COMPLETED)
                                PrayerStatus.QAZA_PENDING else PrayerStatus.NOT_RECORDED) }.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            V27StatusArtwork(status, state.gender, true, false, Modifier.size(22.dp), statusInk)
                            Spacer(Modifier.width(4.dp))
                            V27FixedText(v27StatusLabel(status, state.gender), size = 12,
                                line = if (LocalConfiguration.current.locales[0].language == "ur") 23 else 18,
                                color = statusInk, maxLines = 1)
                        }
                        if (status == PrayerStatus.QAZA_PENDING) TextButton(
                            onClick = { onSave(date, type, PrayerStatus.QAZA_COMPLETED) },
                            contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(30.dp)) {
                            HomeText(stringResource(R.string.home_v8_make_up), size = 11, color = SalatPrimary)
                        }
                    }
                }
                HorizontalDivider(color = V27Divider)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun V27StreakSheet(state: HomeUiState, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SalatSurface) {
        Column(Modifier.fillMaxWidth().testTag("home-streak-sheet").verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            HomeText(stringResource(R.string.home_v8_streaks), size = 16, weight = FontWeight.SemiBold)
            PrayerType.values().forEach { type ->
                val streak = state.streaks[type] ?: StreakInfo(0, 0)
                Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(type.iconRes), null, tint = V27Muted, modifier = Modifier.size(18.dp))
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        HomeText(type.label(state.todayDate, state.jumuahEnabled, state.madhab), size = 15)
                        if (state.gender != Gender.FEMALE) HomeText(
                            stringResource(R.string.home_v27_in_congregation, v27CongregationRun(state, type), v27CongregationRun(state, type)),
                            size = 12, muted = true)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.home_flame), null, tint = HomeGold, modifier = Modifier.size(16.dp))
                            HomeText("${streak.currentStreak}", size = 18, weight = FontWeight.SemiBold)
                        }
                        HomeText(stringResource(R.string.home_v8_best, streak.highestStreak), size = 12, muted = true)
                    }
                }
                HorizontalDivider(color = V27Divider)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.home_flame), null, tint = HomeGold, modifier = Modifier.size(18.dp))
                HomeText(stringResource(R.string.home_v8_all_five), Modifier.weight(1f).padding(start = 8.dp), size = 13)
                HomeText("${perfectDayStreak(state.streaks)} · ${stringResource(R.string.home_v8_best, perfectDayStreak(state.streaks))}", size = 12)
            }
            HomeText(stringResource(R.string.home_v27_streak_rule), size = 11, muted = true)
        }
    }
}

@Composable private fun BoxScope.V27AppreciationToast(message: String, excused: Boolean) {
    Surface(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp).widthIn(max = 250.dp)
        .testTag("home-appreciation"), shape = RoundedCornerShape(14.dp),
        color = Color(0x9904282B), shadowElevation = 6.dp) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.home_flame), null,
                tint = if (excused) Color(0xFFE873BC) else HomeGold, modifier = Modifier.size(16.dp))
            HomeText(message, Modifier.padding(start = 7.dp), size = 12, color = Color.White, weight = FontWeight.SemiBold)
        }
    }
}

@Composable private fun BoxScope.V27Confetti(id: Long) {
    val progress = remember(id) { Animatable(0f) }
    LaunchedEffect(id) { progress.animateTo(1f, tween(1600, easing = CubicBezierEasing(.2f, .7f, .4f, 1f))) }
    val colors = listOf(HomeGold, HomeCream, Color(0xFF3E9A95), Color(0xFFF2C86B), Color.White, HomeTeal)
    Canvas(Modifier.fillMaxWidth().height(330.dp).align(Alignment.TopCenter)) {
        val t = progress.value
        repeat(30) { i ->
            val angle = (-165f + ((i * 47) % 150)) * PI.toFloat() / 180f
            val distance = (70 + (i * 37) % 120).dp.toPx()
            val x = size.width * .72f + cos(angle) * distance * t
            val y = size.height * .27f + sin(angle) * distance * t + 150.dp.toPx() * t * t
            drawRect(colors[i % colors.size].copy(alpha = 1f - t), Offset(x, y),
                androidx.compose.ui.geometry.Size(7.dp.toPx(), 11.dp.toPx()))
        }
    }
}

@Composable private fun v27StatusLabel(status: PrayerStatus, gender: Gender?): String = when {
    status == PrayerStatus.INDIVIDUAL && gender == Gender.FEMALE -> stringResource(R.string.home_v8_prayed)
    status == PrayerStatus.MENSTRUATION -> stringResource(R.string.home_v8_excused)
    status == PrayerStatus.QAZA_PENDING -> stringResource(R.string.home_v8_qaza)
    status == PrayerStatus.QAZA_COMPLETED -> stringResource(R.string.home_v8_made_up)
    else -> status.label()
}

private fun v27OfferedStatuses(gender: Gender?) = if (gender == Gender.FEMALE)
    listOf(PrayerStatus.INDIVIDUAL, PrayerStatus.MENSTRUATION, PrayerStatus.QAZA_PENDING)
else listOf(PrayerStatus.CONGREGATION, PrayerStatus.INDIVIDUAL, PrayerStatus.QAZA_PENDING)

private fun v27StatusIcon(status: PrayerStatus, gender: Gender?) = when (status) {
    PrayerStatus.CONGREGATION -> R.drawable.home_status_congregation
    PrayerStatus.INDIVIDUAL -> if (gender == Gender.FEMALE) R.drawable.home_status_prayed else R.drawable.home_status_individual
    PrayerStatus.MENSTRUATION -> R.drawable.home_status_excused
    PrayerStatus.QAZA_PENDING -> R.drawable.home_status_qaza
    PrayerStatus.QAZA_COMPLETED -> R.drawable.home_status_made_up
    PrayerStatus.NOT_RECORDED -> R.drawable.home_status_qaza
}

@Composable private fun v27AppreciationText(saved: HomeSaveFeedback, state: HomeUiState): String {
    val prayer = saved.type.label(state.todayDate, state.jumuahEnabled, state.madhab)
    return when {
        saved.status == PrayerStatus.MENSTRUATION -> stringResource(R.string.home_v8_rest)
        !saved.onTime -> stringResource(R.string.home_v27_late_message)
        saved.status == PrayerStatus.CONGREGATION -> stringResource(R.string.home_v27_congregation_message, prayer)
        saved.status == PrayerStatus.INDIVIDUAL && state.gender == Gender.FEMALE -> stringResource(R.string.home_v27_prayed_message, prayer)
        saved.status == PrayerStatus.INDIVIDUAL -> stringResource(R.string.home_v27_individual_message, prayer)
        else -> stringResource(R.string.home_v8_saved)
    }
}

private fun v27RecentUnrecordedDays(state: HomeUiState): Int = (0L..6L).count { offset ->
    val day = state.todayDate.minusDays(offset)
    val marks = if (offset == 0L) state.prayers.associate { it.type to it.status } else state.lastSevenDays[day].orEmpty()
    marks.isEmpty() || PrayerType.values().all { marks[it] == null || marks[it] == PrayerStatus.NOT_RECORDED }
}

private fun v27CongregationRun(state: HomeUiState, type: PrayerType): Int {
    var count = 0
    for (offset in 0L..6L) {
        val status = if (offset == 0L) state.prayers.firstOrNull { it.type == type }?.status
        else state.lastSevenDays[state.todayDate.minusDays(offset)]?.get(type)
        if (status == PrayerStatus.MENSTRUATION) continue
        if (status != PrayerStatus.CONGREGATION) break
        count++
    }
    return count
}

@Composable private fun v27Remaining(minutes: Long): String = stringResource(
    R.string.home_v8_remaining, (minutes / 60).toInt(), (minutes % 60).toInt(),
)

@Composable private fun V27FixedText(
    text: String,
    modifier: Modifier = Modifier,
    size: Int,
    line: Int,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    includeFontPadding: Boolean = false,
    fontFamily: FontFamily = HomeBodyFont,
) {
    val density = LocalDensity.current
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = fontFamily,
        fontWeight = weight,
        fontSize = with(density) { size.dp.toSp() },
        lineHeight = with(density) { line.dp.toSp() },
        textAlign = align,
        maxLines = maxLines,
        softWrap = softWrap,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = includeFontPadding)),
    )
}

@Composable
internal fun HomeText(
    text: String,
    modifier: Modifier = Modifier,
    size: Int = 14,
    muted: Boolean = false,
    color: Color = Color.Unspecified,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        modifier = modifier,
        color = if (color != Color.Unspecified) color else if (muted) SalatOnSurfaceVariant else SalatOnSurface,
        fontFamily = HomeBodyFont,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = (size * 1.5f).sp,
        textAlign = align,
    )
}
