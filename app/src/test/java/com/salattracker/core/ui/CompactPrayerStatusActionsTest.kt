package com.salattracker.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.salattracker.core.theme.DarkSalatColors
import com.salattracker.core.theme.LightSalatColors
import com.salattracker.core.theme.SalatColors
import org.junit.Assert.assertTrue
import org.junit.Test

class CompactPrayerStatusActionsTest {

    @Test
    fun readableStatusContentColor_meetsNormalTextContrastAcrossThemes() {
        listOf(LightSalatColors, DarkSalatColors).forEach { palette ->
            statusColors(palette).forEach { background ->
                val foreground = readableStatusContentColor(background)
                assertTrue(
                    "contrast=${contrastRatio(foreground, background)} foreground=$foreground background=$background",
                    contrastRatio(foreground, background) >= 4.5f,
                )
            }
        }
    }

    private fun statusColors(colors: SalatColors) = listOf(
        colors.statusCongregation,
        colors.statusIndividual,
        colors.statusQazaPending,
        colors.statusQazaCompleted,
        colors.statusExcused,
        colors.statusNotRecorded,
    )

    private fun contrastRatio(first: Color, second: Color): Float {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
