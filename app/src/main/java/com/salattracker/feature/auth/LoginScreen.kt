package com.salattracker.feature.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*

// ---------------------------------------------------------------------------
// Login Screen
// ---------------------------------------------------------------------------

@Composable
fun LoginScreen(
    uiState: LoginUiState = LoginUiState(),
    onGoogleSignInClick: () -> Unit = {},
    onEmailSignInClick: () -> Unit = {},
    onErrorDismiss: () -> Unit = {},
) {
    // ── Entrance animation ────────────────────────────────────────────────────
    val enterAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enterAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SalatBackground),
        contentAlignment = Alignment.Center,
    ) {
        // ── Background dot grid ───────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.05f)) {
            drawDotGrid(this)
        }

        // ── Decorative blurred blobs ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(256.dp)
                .offset(x = 80.dp, y = (-80).dp)
                .align(Alignment.TopEnd)
                .blur(64.dp)
                .background(SalatPrimaryContainer.copy(alpha = 0.08f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-80).dp, y = 80.dp)
                .align(Alignment.BottomStart)
                .blur(64.dp)
                .background(SalatSurfaceTint.copy(alpha = 0.06f), CircleShape)
        )

        // ── Main content ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .alpha(enterAnim.value)
                .offset(y = (20.dp * (1f - enterAnim.value))),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── Logo ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SalatPrimaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mosque),
                    contentDescription = "Salat Tracker",
                    tint = SalatPrimaryFixed,
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Sakeenah",
                color = SalatPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MontserratFamily,
                letterSpacing = (-0.5).sp,
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Hero copy ──────────────────────────────────────────────────────
            Text(
                text = "Sign in to track your prayers",
                color = SalatOnSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = MontserratFamily,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Maintain your spiritual discipline with organised daily tracking and quiet reflections.",
                color = SalatOnSurfaceVariant,
                fontSize = 14.sp,
                fontFamily = InterFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Action card ───────────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = SalatSecondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                    ),
                shape = RoundedCornerShape(16.dp),
                color = SalatSurface,
                tonalElevation = 0.dp,
                shadowElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {

                    // Google Sign-In button
                    GoogleSignInButton(
                        isLoading = uiState.isLoading,
                        onClick = onGoogleSignInClick,
                    )

                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = SalatOutlineVariant,
                        )
                        Text(
                            text = "OR",
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = SalatOnSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFamily,
                            letterSpacing = 1.sp,
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = SalatOutlineVariant,
                        )
                    }

                    // Email button
                    OutlinedButton(
                        onClick = onEmailSignInClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SalatOnSurface,
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = SalatOutline,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_mail),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign in with Email",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFamily,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── Legal footer ──────────────────────────────────────────────────
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = SalatOnSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontFamily = InterFamily,
                        )
                    ) {
                        append("By continuing, you agree to our\n")
                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append("Terms of Service")
                        }
                        append(" and ")
                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append("Privacy Policy")
                        }
                    }
                },
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )
        }

        // ── Error snackbar ────────────────────────────────────────────────────
        uiState.error?.let { message ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = onErrorDismiss) {
                        Text("Dismiss", color = SalatPrimaryFixed)
                    }
                },
                containerColor = SalatInverseSurface,
                contentColor = SalatInverseOnSurface,
            ) {
                Text(text = message, fontFamily = InterFamily, fontSize = 14.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Google Sign-In button
// ---------------------------------------------------------------------------

@Composable
private fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val interactionScale = remember { Animatable(1f) }

    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(interactionScale.value),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SalatPrimary,
            contentColor = SalatOnPrimary,
            disabledContainerColor = SalatPrimaryContainer,
            disabledContentColor = SalatOnPrimary.copy(alpha = 0.7f),
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = SalatOnPrimary,
            )
        } else {
            // Official Google "G" logo
            GoogleLogo(modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Continue with Google",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFamily,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Pixel-faithful Google "G" logo via Canvas
// ---------------------------------------------------------------------------

@Composable
private fun GoogleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // White rounded background tile
        drawRoundRect(
            color = Color.White,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
        )

        // Simplified coloured G paths (4-segment arc approximation via arcs)
        val cx = w / 2f
        val cy = h / 2f
        val r  = w * 0.38f

        // Blue top-right arc
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.55f),
        )
        // Green bottom-right
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.55f),
        )
        // Yellow bottom-left
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.55f),
        )
        // Red top-left
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.55f),
        )
        // Blue horizontal arm of the G
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(cx, cy - r * 0.25f),
            size = androidx.compose.ui.geometry.Size(r, r * 0.5f),
        )
    }
}

// ---------------------------------------------------------------------------
// Dot-grid background
// ---------------------------------------------------------------------------

private fun drawDotGrid(scope: DrawScope) {
    val spacing = 24.dp.value * scope.density
    val dotColor = Color(0xFF064E3B)
    var x = 0f
    while (x <= scope.size.width) {
        var y = 0f
        while (y <= scope.size.height) {
            scope.drawCircle(color = dotColor, radius = 1f, center = Offset(x, y))
            y += spacing
        }
        x += spacing
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenLoadingPreview() {
    LoginScreen(uiState = LoginUiState(isLoading = true))
}