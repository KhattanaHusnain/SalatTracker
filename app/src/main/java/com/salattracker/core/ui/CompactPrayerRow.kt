package com.salattracker.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.salattracker.domain.model.PrayerType

/**
 * Compact prayer-row scaffold shared by Home and Calendar. Callers provide the prayer details
 * and actions while the leading icon, active-row accent, spacing, disabled state, and divider
 * remain identical across both screens.
 */
@Composable
internal fun CompactPrayerRow(
    prayerType: PrayerType,
    enabled: Boolean,
    accentColor: Color,
    dividerColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    highlightAlpha: Float = 0f,
    highlightColor: Color = accentColor,
    contentModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (isActive) drawRect(accentColor, size = Size(3.dp.toPx(), size.height))
                    if (highlightAlpha > 0f) drawRect(highlightColor.copy(alpha = highlightAlpha))
                }
                .alpha(if (enabled) 1f else 0.55f)
                .padding(
                    start = if (isActive) 8.dp else 0.dp,
                    top = 7.dp,
                    bottom = 7.dp,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(prayerType.iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
                Column(modifier = Modifier.weight(1f).then(contentModifier), content = content)
                actions()
            }
            footer()
        }
        HorizontalDivider(color = dividerColor)
    }
}
