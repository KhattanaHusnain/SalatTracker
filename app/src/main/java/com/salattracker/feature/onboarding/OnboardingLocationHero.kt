package com.salattracker.feature.onboarding

import android.animation.ValueAnimator
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.salattracker.R
import com.salattracker.core.theme.SalatPrimary

/** The export's two 2.8s ripples and 3dp bob, local to the location introduction. */
@Composable
internal fun OnboardingLocationHero(modifier: Modifier = Modifier, motionEnabled: Boolean? = null) {
    val enabled = motionEnabled ?: rememberOnboardingMotionEnabled()
    var first = .4667f // Reduced motion: scale 1.2, as in the export's static fallback.
    var second: Float? = null
    var bob = 0f
    if (enabled) {
        val transition = rememberInfiniteTransition(label = "onboarding-location")
        first = transition.animateFloat(0f, 1f,
            infiniteRepeatable(tween(2800, easing = LinearOutSlowInEasing)), label = "first-ripple").value
        second = transition.animateFloat(0f, 1f,
            infiniteRepeatable(tween(2800, easing = LinearOutSlowInEasing),
                initialStartOffset = StartOffset(1400)), label = "second-ripple").value
        bob = transition.animateFloat(0f, -3f,
            infiniteRepeatable(tween(1400, easing = CubicBezierEasing(.42f, 0f, .58f, 1f)), RepeatMode.Reverse),
            label = "pin-bob").value
    }
    val color = SalatPrimary
    Box(modifier.fillMaxWidth().height(132.dp).testTag("onboarding-location-hero"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            fun ring(progress: Float) {
                val opacity = if (enabled) .45f * (1f - progress) else .25f
                drawCircle(color.copy(alpha = opacity), radius = 32.dp.toPx() * (.5f + 1.5f * progress),
                    center = Offset(size.width / 2f, size.height / 2f), style = Stroke(1.5.dp.toPx()))
            }
            ring(first)
            // StartOffset keeps the delayed ring invisible before its first cycle.
            second?.takeIf { it > 0f }?.let { ring(it) }
        }
        Icon(painterResource(R.drawable.onboarding_pin), null, Modifier.size(30.dp).offset(y = bob.dp), tint = color)
    }
}

/** Observe the setting so changing "Remove animations" also updates an already open screen. */
@Composable
private fun rememberOnboardingMotionEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    var enabled by remember(resolver) { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { enabled = ValueAnimator.areAnimatorsEnabled() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return enabled
}
