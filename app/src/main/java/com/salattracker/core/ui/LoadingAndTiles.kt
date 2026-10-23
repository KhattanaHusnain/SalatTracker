package com.salattracker.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.AppTitleFontFamily
import com.salattracker.core.theme.SalatOnSurfaceVariant
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatSurfaceContainer
import com.salattracker.core.theme.SalatSurfaceContainerHigh

/** A pulsing placeholder block shown while a section loads — shared by Stats and the Qaza tab
 * so every skeleton in the app pulses at the same rate. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val alpha by rememberInfiniteTransition(label = "sk").animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(SalatSurfaceContainerHigh.copy(alpha = alpha)))
}

/** A small metric tile — a bold value over a muted label, e.g. Stats' "Perfect days" and the
 * Qaza tab's "Today" / "This week" / "Per day" stats. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(SalatSurfaceContainer).padding(vertical = 12.dp, horizontal = 10.dp),
    ) {
        Text(value, color = SalatPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = AppTitleFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(label, color = SalatOnSurfaceVariant,  fontSize = 12.sp, fontFamily = AppUiFontFamily, maxLines = 2)
    }
}
