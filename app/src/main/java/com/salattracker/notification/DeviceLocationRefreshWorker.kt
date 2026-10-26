package com.salattracker.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.salattracker.domain.repository.GeocodingRepository
import com.salattracker.domain.repository.LocationProvider
import com.salattracker.domain.repository.PrayerReminderRescheduler
import com.salattracker.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.math.roundToInt

/** Refreshes an opted-in device location after a system signal that may indicate travel. */
@HiltWorker
class DeviceLocationRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val locationProvider: LocationProvider,
    private val geocodingRepository: GeocodingRepository,
    private val prayerReminderRescheduler: PrayerReminderRescheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = settingsRepository.getCurrentPrefs()
        if (!prefs.useDeviceLocation) return Result.success()

        val (latitude, longitude) = locationProvider.getCurrentLocation() ?: return Result.success()
        if (latitude.roundedForComparison() == prefs.latitude.roundedForComparison() &&
            longitude.roundedForComparison() == prefs.longitude.roundedForComparison()
        ) return Result.success()

        val city = runCatching { geocodingRepository.reverseGeocode(latitude, longitude) }.getOrNull()
        settingsRepository.setLocation(latitude, longitude)
        settingsRepository.setLocationLabel(city?.fullLabel)
        prayerReminderRescheduler.rescheduleAll()
        return Result.success()
    }

    private fun Double.roundedForComparison(): Double = (this * 100).roundToInt() / 100.0

    companion object {
        private const val WORK_NAME = "device_location_refresh"

        fun enqueue(workManager: WorkManager) {
            workManager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<DeviceLocationRefreshWorker>().build(),
            )
        }
    }
}
