package com.salattracker.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class SalatWidgetProvider : AppWidgetProvider() {
    abstract val variant: WidgetVariant
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = update(context, ids)
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) = update(context, intArrayOf(id))
    override fun onDeleted(context: Context, ids: IntArray) {
        val preferences = WidgetPreferences(context)
        ids.forEach(preferences::remove)
        WidgetUpdateScheduler.enqueue(context)
    }
    private fun update(context: Context, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { WidgetUpdater(context).update(variant, ids) } finally { pending.finish() }
        }
    }
}

object WidgetUpdater {
    operator fun invoke(context: Context) = Updater(context.applicationContext)
    class Updater(private val context: Context) {
        suspend fun update(variant: WidgetVariant, ids: IntArray) {
            if (ids.isEmpty()) return
            val entry = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
            val snapshot = WidgetSnapshotBuilder(context, entry.prayerRepository(), entry.settingsRepository()).build()
            update(variant, ids, snapshot)
            WidgetUpdateScheduler.scheduleTransition(context, snapshot.nextTransitionAt)
        }
        private fun update(variant: WidgetVariant, ids: IntArray, snapshot: WidgetSnapshot) {
            val manager = AppWidgetManager.getInstance(context)
            val renderer = WidgetRenderer(context)
            ids.forEach { id -> manager.updateAppWidget(id, renderer.render(variant, id, snapshot)) }
        }
        suspend fun updateAll() {
            val entry = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
            val snapshot = WidgetSnapshotBuilder(context, entry.prayerRepository(), entry.settingsRepository()).build()
            WidgetVariant.entries.forEach { variant ->
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, providerClass(variant)))
                update(variant, ids, snapshot)
            }
            WidgetUpdateScheduler.scheduleTransition(context, snapshot.nextTransitionAt)
        }
    }
}

object WidgetUpdateScheduler {
    private const val PERIODIC_WORK = "salat_widget_periodic_refresh"
    private const val IMMEDIATE_WORK = "salat_widget_immediate_refresh"
    fun enqueuePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }
    fun enqueue(context: Context) {
        val request = androidx.work.OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(IMMEDIATE_WORK, ExistingWorkPolicy.REPLACE, request)
    }
    fun scheduleTransition(context: Context, at: Instant) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, WidgetTransitionReceiver::class.java).setAction("com.salattracker.WIDGET_TRANSITION")
        val pending = PendingIntent.getBroadcast(context, 8021, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val whenMillis = at.toEpochMilli()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pending)
        else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pending)
    }
}

class WidgetRefreshWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching { WidgetUpdater(applicationContext).updateAll(); Result.success() }.getOrElse { Result.retry() }
}

class WidgetTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = WidgetUpdateScheduler.enqueue(context)
}
