package com.salattracker.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import com.salattracker.R

/** Small per-widget setup surface. Variant selection stays fixed by the picker entry. */
class WidgetConfigActivity : Activity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        setContentView(R.layout.activity_widget_config)
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)
        findViewById<TextView>(R.id.widget_config_title).text = info?.loadLabel(packageManager) ?: getString(R.string.widget_config_title)
        val saved = WidgetPreferences(this).read(widgetId)
        val contrastGroup = findViewById<RadioGroup>(R.id.widget_config_contrast)
        val wallpaperGroup = findViewById<RadioGroup>(R.id.widget_config_wallpaper)
        contrastGroup.check(when (saved.contrast) {
            WidgetContrast.LIGHT -> R.id.widget_contrast_light
            WidgetContrast.DARK -> R.id.widget_contrast_dark
            else -> R.id.widget_contrast_auto
        })
        wallpaperGroup.check(if (saved.wallpaper == WidgetWallpaper.LOCK) R.id.widget_wallpaper_lock else R.id.widget_wallpaper_home)
        findViewById<Button>(R.id.widget_config_done).setOnClickListener {
            val contrast = when (contrastGroup.checkedRadioButtonId) {
                R.id.widget_contrast_light -> WidgetContrast.LIGHT
                R.id.widget_contrast_dark -> WidgetContrast.DARK
                else -> null
            }
            val wallpaper = if (wallpaperGroup.checkedRadioButtonId == R.id.widget_wallpaper_lock) WidgetWallpaper.LOCK else WidgetWallpaper.HOME
            WidgetPreferences(this).save(widgetId, contrast, wallpaper)
            WidgetUpdateScheduler.enqueue(this)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }
    }
}
