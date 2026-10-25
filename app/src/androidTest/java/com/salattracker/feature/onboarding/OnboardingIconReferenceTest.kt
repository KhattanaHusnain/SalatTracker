package com.salattracker.feature.onboarding

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.PathParser
import androidx.test.platform.app.InstrumentationRegistry
import com.salattracker.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/** Small raster reference comparisons, not full-screen goldens.
 * Expected paths transcribed from IG/BK/ARW in the user-supplied Architectural Teal HTML.
 * Resource bindings were checked against production OnboardingScreen, including SummaryRow and
 * ReminderRow. Clock and forward arrow are controls with equivalent geometry.
 */
@RunWith(Parameterized::class)
class OnboardingIconReferenceTest(private val name: String, private val resource: Int, private val paths: List<Path>) {
    @Test fun productionIcon_matchesExportSilhouette() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = File(context.externalCacheDir, "onboarding-icons").apply { mkdirs() }
        val actual = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val expected = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val actualCanvas = Canvas(actual)
        actualCanvas.apply {
            drawColor(Color.WHITE)
            context.getDrawable(resource)!!.mutate().apply {
                setTint(Color.BLACK); setBounds(0, 0, 96, 96); draw(actualCanvas)
            }
        }
        Canvas(expected).apply {
            drawColor(Color.WHITE); scale(4f, 4f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = if (name == "back" || name == "forward") 1.9f else 1.6f
                strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            }
            paths.forEach { drawPath(it, paint) }
        }
        // Two physical pixels at 4x raster scale tolerate stroke thickness and antialiasing.
        // Both directions matter: extra strokes must fail just like missing ones.
        fun coverage(a: Bitmap, b: Bitmap): Float {
            var ink = 0; var matched = 0
            for (y in 0 until 96) for (x in 0 until 96) if (Color.red(a.getPixel(x, y)) < 128) {
                ink++
                if ((-2..2).any { dy -> (-2..2).any { dx ->
                    val bx = x + dx; val by = y + dy
                    bx in 0..95 && by in 0..95 && Color.red(b.getPixel(bx, by)) < 128
                } }) matched++
            }
            return matched.toFloat() / ink.coerceAtLeast(1)
        }
        val expectedCoverage = coverage(expected, actual)
        val actualCoverage = coverage(actual, expected)
        val passed = expectedCoverage >= .90f && actualCoverage >= .90f
        val sheet = Bitmap.createBitmap(300, 132, Bitmap.Config.ARGB_8888)
        Canvas(sheet).apply {
            drawColor(Color.WHITE)
            val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 13f }
            drawText("$name · export", 8f, 17f, label); drawText("current app", 158f, 17f, label)
            drawBitmap(expected, 8f, 28f, null); drawBitmap(actual, 158f, 28f, null)
        }
        File(output, "$name.png").outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val details = "expectedCoverage=$expectedCoverage; actualCoverage=$actualCoverage; passed=$passed"
        File(output, "$name.txt").writeText(details)
        assertTrue("$name differs from the export: $details", passed)
    }

    companion object {
        private fun path(data: String) = PathParser.createPathFromPathData(data)
        private fun circle(x: Float, y: Float, r: Float) = Path().apply { addCircle(x, y, r, Path.Direction.CW) }
        private fun rect(left: Float, top: Float, right: Float, bottom: Float, radius: Float) = Path().apply {
            addRoundRect(left, top, right, bottom, radius, radius, Path.Direction.CW)
        }
        @JvmStatic @Parameterized.Parameters(name = "icon={0}")
        fun icons(): List<Array<Any>> = listOf(
            arrayOf("male", R.drawable.onboarding_male, listOf(circle(10f, 14f, 5f), path("M14 10l6-6M15 4h5v5"))),
            arrayOf("female", R.drawable.onboarding_female, listOf(circle(12f, 9f, 5f), path("M12 14v7M9 18h6"))),
            // Expand adjacent SVG arc flags for Android PathParser (same geometry).
            arrayOf("pin", R.drawable.onboarding_pin, listOf(path("M12 21s7-6 7-11a7 7 0 1 0 -14 0c0 5 7 11 7 11z"), circle(12f, 10f, 2.5f))),
            arrayOf("method", R.drawable.onboarding_method, listOf(path("M4 7h10M18 7h2M4 17h2M10 17h10"), circle(16f, 7f, 2f), circle(8f, 17f, 2f))),
            arrayOf("screen", R.drawable.onboarding_screen, listOf(rect(7f, 3f, 17f, 21f, 2f), path("M11 18h2"))),
            arrayOf("sound", R.drawable.onboarding_sound, listOf(path("M5 10v4h3l5 4V6L8 10zM16 9a4 4 0 0 1 0 6"))),
            arrayOf("back", R.drawable.onboarding_back, listOf(path("M15 5l-7 7 7 7"))),
            arrayOf("clock", R.drawable.ic_schedule, listOf(circle(12f, 12f, 9f), path("M12 7v5l3 2"))),
            arrayOf("forward", R.drawable.ic_arrow_forward, listOf(path("M5 12h14M13 6l6 6-6 6"))),
        )
    }
}
