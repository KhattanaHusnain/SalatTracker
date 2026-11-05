package com.salattracker.widget

import android.app.PendingIntent
import android.app.AlarmManager
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import com.salattracker.R
import com.salattracker.feature.main.MainActivity
import com.salattracker.domain.model.PrayerStatus
import java.time.Duration
import java.time.Instant

internal class WidgetRenderer(private val context: Context) {
    fun render(variant: WidgetVariant, widgetId: Int, snapshot: WidgetSnapshot): RemoteViews {
        val layout = if (variant.content == WidgetContent.ALL_FIVE) R.layout.widget_all_five else R.layout.widget_current_next
        return RemoteViews(context.packageName, layout).apply {
            val prefs = WidgetPreferences(context).read(widgetId)
            val colors = widgetColors(variant, prefs)
            setTextColor(R.id.widget_time, colors.primary)
            setTextColor(R.id.widget_date, colors.secondary)
            setTextColor(R.id.widget_location, colors.secondary)
            setTextColor(R.id.widget_status, colors.primary)
            setTextColor(R.id.widget_streak, colors.accent)
            setTextColor(R.id.widget_next, colors.secondary)
            setTextColor(R.id.widget_message, colors.secondary)
            if (variant.content == WidgetContent.ALL_FIVE) {
                setTextColor(R.id.widget_countdown, colors.accent)
            } else {
                setTextColor(R.id.widget_current_name, colors.primary)
                setTextColor(R.id.widget_current_time, colors.secondary)
                setTextColor(R.id.widget_countdown, colors.accent)
                setTextColor(R.id.widget_progress, colors.secondary)
            }
            setTextViewText(R.id.widget_time, "")
            setCharSequence(R.id.widget_time, "setFormat12Hour", if (snapshot.use24Hour) "HH:mm" else "h:mm")
            setCharSequence(R.id.widget_time, "setFormat24Hour", if (snapshot.use24Hour) "HH:mm" else "h:mm")
            setOnClickPendingIntent(R.id.widget_root, launchIntent(widgetId))

            if (!snapshot.configured) {
                renderUnconfigured(this)
                return@apply
            }
            setViewVisibility(R.id.widget_content, View.VISIBLE)
            setViewVisibility(R.id.widget_message, View.GONE)
            setTextViewText(R.id.widget_date, listOfNotNull(snapshot.gregorianDate, snapshot.hijriDate).joinToString(" · "))
            setTextViewText(R.id.widget_location, snapshot.location ?: "")
            setViewVisibility(R.id.widget_location, if (snapshot.location == null || variant.size == WidgetSize.SMALL) View.GONE else View.VISIBLE)
            setTextViewText(R.id.widget_streak, if (snapshot.streakDays > 0) {
                if (variant.size == WidgetSize.SMALL) "♨ ${snapshot.streakDays}d"
                else "♨ ${snapshot.streakPrayer} · ${snapshot.streakDays}d"
            } else "")
            setViewVisibility(R.id.widget_streak, if (snapshot.streakDays > 0) View.VISIBLE else View.GONE)

            if (variant.content == WidgetContent.ALL_FIVE) renderAllFive(this, variant, snapshot, colors)
            else renderCurrentNext(this, variant, snapshot, colors)
        }
    }

    private fun renderUnconfigured(views: RemoteViews) {
        views.setViewVisibility(R.id.widget_content, View.GONE)
        views.setTextViewText(R.id.widget_message, context.getString(R.string.widget_setup_required))
        views.setViewVisibility(R.id.widget_message, View.VISIBLE)
        views.setViewVisibility(R.id.widget_date, View.GONE)
        views.setViewVisibility(R.id.widget_location, View.GONE)
        views.setViewVisibility(R.id.widget_streak, View.GONE)
    }

    private fun renderAllFive(views: RemoteViews, variant: WidgetVariant, snapshot: WidgetSnapshot, colors: WidgetColors) {
        val active = snapshot.activePrayer
        views.setTextViewText(R.id.widget_status, active?.let { "${it.label} · ${context.getString(R.string.widget_ongoing)}" }
            ?: context.getString(R.string.widget_no_active_prayer))
        renderCountdown(views, R.id.widget_countdown, snapshot.activeDeadline, colors.accent, snapshot)
        views.setTextViewText(R.id.widget_next, snapshot.nextPrayer?.let { "${context.getString(R.string.widget_next)}: ${it.label} · ${format(it.time, snapshot.use24Hour, snapshot.locale)}" } ?: "")
        val rowIds = listOf(
            RowIds(R.id.row_0, R.id.row_0_marker, R.id.row_0_name, R.id.row_0_time, R.id.row_0_state),
            RowIds(R.id.row_1, R.id.row_1_marker, R.id.row_1_name, R.id.row_1_time, R.id.row_1_state),
            RowIds(R.id.row_2, R.id.row_2_marker, R.id.row_2_name, R.id.row_2_time, R.id.row_2_state),
            RowIds(R.id.row_3, R.id.row_3_marker, R.id.row_3_name, R.id.row_3_time, R.id.row_3_state),
            RowIds(R.id.row_4, R.id.row_4_marker, R.id.row_4_name, R.id.row_4_time, R.id.row_4_state),
        )
        snapshot.prayers.zip(rowIds).forEach { (prayer, ids) ->
            views.setViewVisibility(ids.row, View.VISIBLE)
            views.setTextViewText(ids.marker, marker(prayer.status, prayer.isActive))
            views.setTextViewText(ids.name, prayer.label)
            views.setTextViewText(ids.time, format(prayer.time, snapshot.use24Hour, snapshot.locale))
            views.setTextViewText(ids.state, when {
                prayer.isActive -> context.getString(R.string.widget_current)
                prayer.isNext -> context.getString(R.string.widget_next)
                prayer.status == PrayerStatus.CONGREGATION || prayer.status == PrayerStatus.INDIVIDUAL -> context.getString(R.string.widget_done)
                else -> ""
            })
            val nameColor = if (prayer.isActive) colors.accent else colors.primary
            views.setTextColor(ids.marker, nameColor)
            views.setTextColor(ids.name, nameColor)
            views.setTextColor(ids.time, colors.secondary)
            views.setTextColor(ids.state, colors.secondary)
        }
    }

    private fun renderCurrentNext(views: RemoteViews, variant: WidgetVariant, snapshot: WidgetSnapshot, colors: WidgetColors) {
        val active = snapshot.activePrayer
        views.setTextViewText(R.id.widget_current_name, active?.label ?: context.getString(R.string.widget_no_active_prayer))
        views.setTextViewText(R.id.widget_current_time, active?.let { format(it.time, snapshot.use24Hour, snapshot.locale) } ?: "")
        renderCountdown(views, R.id.widget_countdown, snapshot.activeDeadline, colors.accent, snapshot)
        views.setTextViewText(R.id.widget_next, snapshot.nextPrayer?.let {
            "${context.getString(R.string.widget_next)} · ${it.label} ${context.getString(R.string.widget_at)} ${format(it.time, snapshot.use24Hour, snapshot.locale)}"
        } ?: "")
        val progress = snapshot.prayers.joinToString("   ") { prayer ->
            val symbol = marker(prayer.status, prayer.isActive)
            "$symbol ${short(prayer.label)}"
        }
        views.setTextViewText(R.id.widget_progress, progress)
    }

    private fun renderCountdown(views: RemoteViews, viewId: Int, deadline: Instant?, color: Int, snapshot: WidgetSnapshot) {
        if (deadline == null || !deadline.isAfter(Instant.now())) {
            views.setViewVisibility(viewId, View.GONE)
            return
        }
        val remaining = Duration.between(Instant.now(), deadline).toMillis().coerceAtLeast(0)
        views.setViewVisibility(viewId, View.VISIBLE)
        views.setTextColor(viewId, color)
        val alarm = context.getSystemService(AlarmManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
            val time = deadline.atZone(java.time.ZoneId.systemDefault()).toLocalTime()
            views.setChronometer(viewId, 0L, null, false)
            views.setTextViewText(viewId, context.getString(R.string.widget_ends_at, format(time, snapshot.use24Hour, snapshot.locale)))
            return
        }
        views.setChronometer(viewId, SystemClock.elapsedRealtime() + remaining, "%s", true)
        views.setChronometerCountDown(viewId, true)
    }

    private fun launchIntent(widgetId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.salattracker.OPEN_WIDGET"
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, widgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun format(time: java.time.LocalTime?, use24: Boolean, locale: java.util.Locale): String = time?.let {
        it.format(java.time.format.DateTimeFormatter.ofPattern(if (use24) "HH:mm" else "h:mm a", locale))
    } ?: "—"

    private fun marker(status: PrayerStatus, active: Boolean): String = when {
        active -> "◉"
        status == PrayerStatus.CONGREGATION || status == PrayerStatus.INDIVIDUAL -> "✓"
        status == PrayerStatus.QAZA_COMPLETED -> "↻"
        status == PrayerStatus.QAZA_PENDING -> "!"
        status == PrayerStatus.MENSTRUATION -> "—"
        else -> "○"
    }

    private fun short(label: String) = label.take(1).uppercase()

    private fun widgetColors(variant: WidgetVariant, prefs: WidgetPreferences.Options): WidgetColors {
        val contrast = prefs.contrast ?: automaticContrast(prefs.wallpaper)
        val darkText = contrast != WidgetContrast.DARK
        return when (variant.style) {
            WidgetStyle.MONOCHROME -> if (darkText) WidgetColors(Color.rgb(20, 24, 23), Color.rgb(75, 84, 81), Color.rgb(20, 24, 23))
                else WidgetColors(Color.WHITE, Color.rgb(222, 232, 228), Color.WHITE)
            WidgetStyle.THEMED -> if (darkText) WidgetColors(Color.rgb(5, 74, 70), Color.rgb(50, 88, 82), Color.rgb(188, 132, 25))
                else WidgetColors(Color.rgb(255, 248, 232), Color.rgb(209, 233, 225), Color.rgb(242, 200, 107))
        }
    }

    private fun automaticContrast(wallpaper: WidgetWallpaper): WidgetContrast {
        val flag = if (wallpaper == WidgetWallpaper.LOCK) WallpaperManager.FLAG_LOCK else WallpaperManager.FLAG_SYSTEM
        val color = runCatching { WallpaperManager.getInstance(context).getWallpaperColors(flag)?.primaryColor?.toArgb() }.getOrNull()
        return if (color != null && ColorUtils.calculateLuminance(color) < 0.45) WidgetContrast.DARK else WidgetContrast.LIGHT
    }

    private data class RowIds(val row: Int, val marker: Int, val name: Int, val time: Int, val state: Int)
    private data class WidgetColors(val primary: Int, val secondary: Int, val accent: Int)
}

internal class WidgetPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("widget_options", Context.MODE_PRIVATE)
    data class Options(val contrast: WidgetContrast?, val wallpaper: WidgetWallpaper)
    fun read(id: Int): Options = Options(
        prefs.getString("contrast_$id", null)?.let { runCatching { WidgetContrast.valueOf(it) }.getOrNull() },
        prefs.getString("wallpaper_$id", WidgetWallpaper.HOME.name)?.let { runCatching { WidgetWallpaper.valueOf(it) }.getOrNull() } ?: WidgetWallpaper.HOME,
    )
    fun save(id: Int, contrast: WidgetContrast?, wallpaper: WidgetWallpaper) {
        prefs.edit().putString("contrast_$id", contrast?.name).putString("wallpaper_$id", wallpaper.name).apply()
    }
    fun remove(id: Int) = prefs.edit().remove("contrast_$id").remove("wallpaper_$id").apply()
}
