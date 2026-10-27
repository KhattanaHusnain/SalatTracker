package com.salattracker.feature.main

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.salattracker.R
import com.salattracker.core.theme.AppTitleFontFamily
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.CormorantFamily
import com.salattracker.core.theme.DarkSalatColors
import com.salattracker.core.theme.LightSalatColors
import com.salattracker.core.theme.LocalSalatColors
import com.salattracker.core.theme.NastaliqUrduFamily
import com.salattracker.core.theme.OutfitFamily
import com.salattracker.core.theme.SalatColors
import com.salattracker.core.theme.SalatTrackerTheme
import com.salattracker.core.theme.ScheherazadeFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class AppThemeLayoutRegressionTest(
    private val width: Int,
    private val fontScale: Float,
    private val dark: Boolean,
) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private var renderedColors: SalatColors? = null
    private var renderedBodyFont: FontFamily? = null
    private var renderedTitleFont: FontFamily? = null
    private var renderedLanguage: String = ""
    private val output get() = File(
        InstrumentationRegistry.getInstrumentation().targetContext.externalCacheDir,
        "app-theme-home-v27",
    ).apply { mkdirs() }

    @Test
    fun productionDrawer_usesHomeIdentity_withoutClippedLabels() {
        render()

        compose.runOnIdle {
            val expected = if (dark) DarkSalatColors else LightSalatColors
            assertEquals(expected, renderedColors)
            assertEquals(if (dark) Color(0xFF73CBC8) else Color(0xFF087E83), renderedColors?.primary)
            assertEquals(if (dark) Color(0xFF101C1D) else Color.White, renderedColors?.surface)
            assertEquals(
                when (renderedLanguage) {
                    "ur" -> NastaliqUrduFamily
                    "ar" -> ScheherazadeFamily
                    else -> OutfitFamily
                },
                renderedBodyFont,
            )
            assertEquals(
                if (renderedLanguage in listOf("ur", "ar")) renderedBodyFont else CormorantFamily,
                renderedTitleFont,
            )
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val labels = listOf(
            R.string.action_bulk_update,
            R.string.prayer_reminders,
            R.string.mosque_mode,
            R.string.backup_title,
            R.string.nav_stats,
            R.string.tasbih_title,
            R.string.action_donate,
            R.string.action_developers,
            R.string.topbar_quran_credits,
            R.string.nav_settings,
        ).map(context::getString)

        screenshot("top")
        labels.forEach { label ->
            val node = compose.onNodeWithText(label, useUnmergedTree = true)
            node.performScrollTo().assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            val paintedExcess = (0 until layout.lineCount).maxOfOrNull {
                layout.getLineRight(it) - layout.getLineLeft(it) - layout.size.width
            } ?: 0f
            assertEquals("$label must remain one line at ${width}dp/$fontScale", 1, layout.lineCount)
            assertFalse(
                "$label overflow at ${width}dp/$fontScale: " +
                    "width=${layout.didOverflowWidth}, height=${layout.didOverflowHeight}, excess=$paintedExcess",
                layout.didOverflowHeight || (layout.didOverflowWidth && paintedExcess > 1f),
            )
            assertEquals(label.length, layout.getLineEnd(0))
        }
        screenshot("bottom")
    }

    private fun render() {
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val device = LocalDensity.current
                val height = if (width >= 600) 960 else 800
                val viewportDensity = minOf(
                    device.density,
                    constraints.maxWidth.toFloat() / width,
                    constraints.maxHeight.toFloat() / height,
                )
                CompositionLocalProvider(LocalDensity provides Density(viewportDensity, fontScale)) {
                    SalatTrackerTheme(darkTheme = dark) {
                        renderedColors = LocalSalatColors.current
                        renderedBodyFont = AppUiFontFamily
                        renderedTitleFont = AppTitleFontFamily
                        renderedLanguage = LocalConfiguration.current.locales[0].language
                        Box(
                            Modifier.requiredSize(width.dp, height.dp).testTag("app-theme-test-root"),
                        ) {
                            AppDrawer(
                                backupNeedsAttention = true,
                                onBulkUpdateClick = {},
                                onPrayerRemindersClick = {},
                                onMosqueModeClick = {},
                                onBackupClick = {},
                                onDonateClick = {},
                                onDevelopersClick = {},
                                onStatsClick = {},
                                onTasbihClick = {},
                                onQuranCreditsClick = {},
                                onSettingsClick = {},
                            )
                        }
                    }
                }
            }
        }
    }

    private fun screenshot(position: String) {
        if ((width == 360 && fontScale == 1f) || (width == 320 && fontScale == 1.3f) || dark) {
            val bitmap = compose.onNodeWithTag("app-theme-test-root")
                .captureToImage().asAndroidBitmap()
            File(output, "$position-${width}dp-$fontScale-${if (dark) "dark" else "light"}.png")
                .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0}dp fontScale={1} dark={2}")
        fun cases(): List<Array<Any>> = buildList {
            listOf(320, 360, 600).forEach { width ->
                listOf(1f, 1.3f).forEach { scale -> add(arrayOf<Any>(width, scale, false)) }
            }
            add(arrayOf<Any>(360, 1f, true))
        }
    }
}
