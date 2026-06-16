package com.salattracker.feature.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Splash Screen
// ---------------------------------------------------------------------------

@Composable
fun SplashScreen() {
    // ── Entrance: fade-in + slight scale-up ──────────────────────────────────
    val entranceAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entranceAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1_000, easing = FastOutSlowInEasing)
        )
    }

    // ── Floating icon: gentle bob up and down ────────────────────────────────
    val floatAnim = rememberInfiniteTransition(label = "float")
    val iconOffsetY by floatAnim.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconFloat"
    )

    // ── Progress bar ─────────────────────────────────────────────────────────
    val progressAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2_500, easing = LinearOutSlowInEasing)
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SalatBackground
    ) {
        // Subtle Islamic star-pattern overlay via Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIslamicPattern(this)
        }

        // Soft radial glow in the centre
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = SalatSurfaceContainerLow.copy(alpha = 0.25f),
                radius = size.minDimension * 0.7f,
                center = center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Logo + wordmark ───────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .alpha(entranceAnim.value)
                    .scale(0.95f + 0.05f * entranceAnim.value)
                    .offset(y = iconOffsetY.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon tile
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SalatPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_mosque),
                        contentDescription = "Salat Tracker logo",
                        tint = SalatPrimaryFixed,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // App name
                Text(
                    text = "Salat Tracker",
                    color = SalatPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MontserratFamily,
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tagline
                Text(
                    text = "DIGITAL SERENITY",
                    color = SalatOnSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFamily,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── Bottom: progress bar + loading label ─────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp, start = 48.dp, end = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            // Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SalatSurfaceContainerHigh)
            ) {
                // Fill
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progressAnim.value)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SalatPrimaryContainer)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Seeking tranquility…",
                color = SalatOnSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontFamily = InterFamily,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Draw a tiled, lightweight Islamic star pattern (no bitmap required)
// ---------------------------------------------------------------------------
private fun drawIslamicPattern(scope: DrawScope) {
    val tileSize = 60f
    val cols = (scope.size.width / tileSize).toInt() + 2
    val rows = (scope.size.height / tileSize).toInt() + 2
    val patternColor = Color(0xFF003527).copy(alpha = 0.03f)
    val cx = tileSize / 2f
    val cy = tileSize / 2f
    val outerR = tileSize * 0.44f
    val innerR = tileSize * 0.18f
    val points = 8

    for (row in -1..rows) {
        for (col in -1..cols) {
            val ox = col * tileSize
            val oy = row * tileSize
            val path = Path()
            for (i in 0 until points * 2) {
                val angle = Math.PI / points * i - Math.PI / 2
                val r = if (i % 2 == 0) outerR else innerR
                val x = ox + cx + (r * cos(angle)).toFloat()
                val y = oy + cy + (r * sin(angle)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            scope.drawPath(path = path, color = patternColor)
        }
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen()
}