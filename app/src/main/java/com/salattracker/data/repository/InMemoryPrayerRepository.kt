package com.salattracker.data.repository

import com.salattracker.domain.model.PrayerEntry
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryPrayerRepository @Inject constructor() : PrayerRepository {

    private val entries =
        MutableStateFlow<Map<LocalDate, MutableList<PrayerEntry>>>(emptyMap())

    override fun observeTodayPrayers(
        date: LocalDate,
    ): Flow<List<PrayerEntry>> =
        entries.map { it[date].orEmpty() }

    override suspend fun getPrayerEntriesForDate(
        date: LocalDate,
    ): List<PrayerEntry> =
        entries.value[date].orEmpty()

    override suspend fun upsertPrayerStatus(
        date: LocalDate,
        prayerType: PrayerType,
        status: PrayerStatus,
    ) {
        val current = entries.value.toMutableMap()

        val dayEntries = current[date]?.toMutableList() ?: mutableListOf()

        val existingIndex =
            dayEntries.indexOfFirst { it.prayerType == prayerType }

        if (existingIndex >= 0) {
            dayEntries[existingIndex] =
                dayEntries[existingIndex].copy(status = status)
        } else {
            dayEntries += PrayerEntry(
                date = date,
                prayerType = prayerType,
                status = status,
            )
        }

        current[date] = dayEntries
        entries.value = current
    }

    override fun observeMonthEntries(
        from: LocalDate,
        to: LocalDate,
    ): Flow<Map<LocalDate, List<PrayerEntry>>> =
        entries.map { all ->
            all.filterKeys { !it.isBefore(from) && !it.isAfter(to) }
        }

    override suspend fun getCachedPrayerTimes(
        date: LocalDate,
    ): Map<PrayerType, LocalTime> =
        mapOf(
            PrayerType.FAJR to LocalTime.of(5, 0),
            PrayerType.DHUHR to LocalTime.of(13, 0),
            PrayerType.ASR to LocalTime.of(16, 30),
            PrayerType.MAGHRIB to LocalTime.of(18, 45),
            PrayerType.ISHA to LocalTime.of(20, 0),
        )

    override suspend fun getHijriLabel(
        date: LocalDate,
    ): String =
        "1 Muharram 1447 AH"

    override suspend fun getStreakSnapshot(): Pair<Int, Int> =
        0 to 0

    override fun observeQazaBalance():
            Flow<Map<PrayerType, Int>> =
        entries.map { allEntries ->
            PrayerType.entries.associateWith { prayerType ->
                allEntries.values
                    .flatten()
                    .count {
                        it.prayerType == prayerType &&
                                it.status == PrayerStatus.QAZA_PENDING
                    }
            }
        }

    override fun observePendingQazaEntries():
            Flow<List<PrayerEntry>> =
        entries.map { allEntries ->
            allEntries.values
                .flatten()
                .filter {
                    it.status == PrayerStatus.QAZA_PENDING
                }
                .sortedByDescending { it.date }
        }

    override suspend fun markQazaCompleted(
        date: LocalDate,
        prayerType: PrayerType,
    ) {
        val current = entries.value.toMutableMap()

        val dayEntries =
            current[date]?.toMutableList() ?: return

        val index = dayEntries.indexOfFirst {
            it.prayerType == prayerType &&
                    it.status == PrayerStatus.QAZA_PENDING
        }

        if (index >= 0) {
            dayEntries[index] =
                dayEntries[index].copy(
                    status = PrayerStatus.QAZA_COMPLETED
                )

            current[date] = dayEntries
            entries.value = current
        }
    }
}