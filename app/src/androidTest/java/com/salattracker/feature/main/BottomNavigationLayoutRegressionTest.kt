package com.salattracker.feature.main

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.salattracker.core.theme.SalatTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class BottomNavigationLayoutRegressionTest(
    private val width: Int,
    private val fontScale: Float,
) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val selectedRoute = mutableStateOf(BottomNavRoute.Home.route)
    private var clickedRoute: BottomNavRoute? = null
    private val output get() = File(
        InstrumentationRegistry.getInstrumentation().targetContext.externalCacheDir,
        "bottom-navigation-v27",
    ).apply { mkdirs() }

    @Test
    fun v27Bar_isSharedByEveryTopLevelScreen_withoutClippedLabels() {
        render(selectedRoute)

        BottomNavRoute.all.forEach { active ->
            compose.runOnIdle { selectedRoute.value = active.route }
            compose.waitForIdle()

            BottomNavRoute.all.forEach { route ->
                val node = compose.onNodeWithTag("bottom-nav-${route.route}")
                node.assertIsDisplayed()
                if (route == active) node.assertIsSelected() else node.assertIsNotSelected()
            }

            BottomNavRoute.all.forEach { route ->
                val labelNode = compose.onNodeWithTag(
                    "bottom-nav-label-${route.route}",
                    useUnmergedTree = true,
                )
                val layouts = mutableListOf<TextLayoutResult>()
                labelNode.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                val layout = layouts.single()
                val paintedExcess = (0 until layout.lineCount).maxOfOrNull {
                    layout.getLineRight(it) - layout.getLineLeft(it) - layout.size.width
                } ?: 0f
                assertEquals("${route.route} must remain one line at ${width}dp/$fontScale", 1, layout.lineCount)
                assertFalse(
                    "${route.route} overflow at ${width}dp/$fontScale: " +
                        "width=${layout.didOverflowWidth}, height=${layout.didOverflowHeight}, " +
                        "paintedExcess=$paintedExcess, size=${layout.size}",
                    layout.didOverflowHeight || (layout.didOverflowWidth && paintedExcess > 1f),
                )
                assertEquals(layout.layoutInput.text.length, layout.getLineEnd(0))
            }

            if ((width == 360 && fontScale == 1f) ||
                (active == BottomNavRoute.Calendar && width in listOf(320, 600))) {
                screenshot(active.route)
            }
        }

        compose.onNodeWithText("18").assertIsDisplayed()
        compose.onNodeWithTag("bottom-nav-quran").performClick()
        compose.runOnIdle { assertEquals(BottomNavRoute.Quran, clickedRoute) }
    }

    private fun render(route: MutableState<String>) {
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val deviceDensity = LocalDensity.current
                val viewportDensity = minOf(
                    deviceDensity.density,
                    constraints.maxWidth.toFloat() / width,
                )
                CompositionLocalProvider(
                    LocalDensity provides Density(viewportDensity, fontScale),
                ) {
                    SalatTrackerTheme {
                        Box(
                            Modifier.requiredWidth(width.dp).align(Alignment.BottomStart)
                                .testTag("bottom-navigation-test-root"),
                        ) {
                            SalatBottomNavBar(
                                routes = BottomNavRoute.all,
                                currentRoute = route.value,
                                qazaPendingTotal = 18,
                                onNavigate = { clickedRoute = it },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun screenshot(route: String) {
        val bitmap = compose.onNodeWithTag("bottom-navigation-test-root")
            .captureToImage().asAndroidBitmap()
        File(output, "$route-${width}dp-$fontScale.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0}dp fontScale={1}")
        fun cases(): List<Array<Any>> = listOf(320, 360, 600).flatMap { width ->
            listOf(1f, 1.3f).map { scale -> arrayOf<Any>(width, scale) }
        }
    }
}
