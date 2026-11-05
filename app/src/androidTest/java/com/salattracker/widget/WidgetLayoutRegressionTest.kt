package com.salattracker.widget

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Measures production RemoteViews in simulated widget bounds; not a launcher integration test. */
@RunWith(Parameterized::class)
class WidgetLayoutRegressionTest(private val variant: WidgetVariant, private val width: Int, private val height: Int, private val scale: Float) {
    @Test fun contentFits() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = instrumentation.targetContext
        val config = Configuration(base.resources.configuration).apply { fontScale = scale; setLocale(Locale.US) }
        val context = base.createConfigurationContext(config)
        val now = Instant.now()
        val prayers = PrayerType.values().mapIndexed { index, type ->
            WidgetPrayer(type, listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")[index], LocalTime.of(5 + index * 3, 30),
                if (index == 0) PrayerStatus.CONGREGATION else PrayerStatus.NOT_RECORDED, index == 1, index == 2)
        }
        val snapshot = WidgetSnapshot(true, LocalDate.now(), now, Locale.US, false, "Thu, 5 Nov", "25 Jumada al-Awwal 1448", "Islamabad", prayers,
            prayers[1], now.plusSeconds(3600), prayers[2], now.plusSeconds(7200), "Maghrib", 12, now.plusSeconds(3600))
        val issues = mutableListOf<String>()
        instrumentation.runOnMainSync {
            val root = WidgetRenderer(context).render(variant, 12345, snapshot).apply(context, null)
            val density = context.resources.displayMetrics.density
            val w = (width * density).toInt(); val h = (height * density).toInt()
            root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            root.layout(0, 0, w, h)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            // The phone selects light text for its dark wallpaper; use a dark backdrop for inspection.
            val canvas = Canvas(bitmap); canvas.drawColor(Color.rgb(25, 35, 33)); root.draw(canvas)
            val dir = File(base.getExternalFilesDir(null), "widget-layout-tests").apply { mkdirs() }
            File(dir, "${variant.name}_${width}x${height}_$scale.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            fun inspect(view: View, x: Int, y: Int) {
                if (view.visibility != View.VISIBLE) return
                val left = x + view.left; val top = y + view.top
                if (view is TextView && view.text.isNotEmpty()) {
                    val id = runCatching { context.resources.getResourceEntryName(view.id) }.getOrDefault("unknown")
                    val layout = view.layout
                    if (left < 0 || top < 0 || left + view.width > w || top + view.height > h) issues += "$id outside widget bounds"
                    if (view.width <= 0 || view.height <= 0 || (layout != null && layout.height > view.height - view.totalPaddingTop - view.totalPaddingBottom))
                        issues += "$id has insufficient visible space: ${view.text}"
                    var parent = view.parent
                    var child: View = view
                    while (parent is ViewGroup) {
                        if (child.left < 0 || child.top < 0 || child.right > parent.width || child.bottom > parent.height) {
                            issues += "$id clipped by parent"
                            break
                        }
                        child = parent
                        parent = parent.parent
                    }
                    if (layout != null) for (line in 0 until layout.lineCount) {
                        if (layout.getEllipsisCount(line) > 0 || layout.getLineWidth(line) > view.width - view.totalPaddingLeft - view.totalPaddingRight + 1)
                            issues += "$id text clipped or ellipsized: ${view.text}"
                    }
                }
                if (view is ViewGroup) for (i in 0 until view.childCount) inspect(view.getChildAt(i), left, top)
            }
            inspect(root, 0, 0)
        }
        assertTrue("${variant.name} ${width}x$height scale=$scale: ${issues.joinToString()}", issues.isEmpty())
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}-{1}x{2}-font{3}")
        fun cases(): List<Array<Any>> = WidgetVariant.entries.flatMap { variant ->
            val width = when (variant.size) { WidgetSize.SMALL -> 220; WidgetSize.MEDIUM -> 260; else -> 300 }
            val minHeight = if (variant.content == WidgetContent.ALL_FIVE) 290 else 270
            val height = if (variant.size == WidgetSize.LARGE) 300 else minHeight
            listOf(width to height, 220 to minHeight).distinct().flatMap { (w, h) ->
                listOf(1f, 1.3f).map { scale -> arrayOf<Any>(variant, w, h, scale) }
            }
        }
    }
}
