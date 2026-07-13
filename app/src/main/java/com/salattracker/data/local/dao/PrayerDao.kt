package com.salattracker.data.local.dao

import androidx.room.*
import com.salattracker.data.local.entity.*
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PrayerDao {

    // ── Single day ────────────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM prayer_entries
        WHERE user_id = :userId AND date = :date
        ORDER BY prayer_type ASC
    """)
    fun observeEntriesForDate(userId: String, date: LocalDate): Flow<List<PrayerEntryEntity>>

    @Query("""
        SELECT * FROM prayer_entries
        WHERE user_id = :userId AND date = :date
    """)
    suspend fun getEntriesForDate(userId: String, date: LocalDate): List<PrayerEntryEntity>

    @Query("""
        SELECT * FROM prayer_entries
        WHERE user_id = :userId AND date = :date AND prayer_type = :prayerType
        LIMIT 1
    """)
    suspend fun getEntry(userId: String, date: LocalDate, prayerType: PrayerType): PrayerEntryEntity?

    // ── Date range (calendar month) ───────────────────────────────────────────

    @Query("""
        SELECT * FROM prayer_entries
        WHERE user_id = :userId AND date >= :from AND date <= :to
        ORDER BY date ASC, prayer_type ASC
    """)
    fun observeEntriesForRange(
        userId: String,
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<PrayerEntryEntity>>

    @Query("""
        SELECT * FROM prayer_entries
        WHERE user_id = :userId AND date >= :from AND date <= :to
        ORDER BY date ASC, prayer_type ASC
    """)
    suspend fun getEntriesForRange(
        userId: String,
        from: LocalDate,
        to: LocalDate,
    ): List<PrayerEntryEntity>

    // ── Upsert ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: PrayerEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<PrayerEntryEntity>)

    // ── Qaza ──────────────────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM prayer_entries
        WHERE user_id = :userId AND status = 'QAZA_PENDING'
        ORDER BY date DESC, prayer_type ASC
    """)
    fun observePendingQaza(userId: String): Flow<List<PrayerEntryEntity>>

    @Query("""
        SELECT prayer_type, COUNT(*) as pendingCount FROM prayer_entries
        WHERE user_id = :userId AND status = 'QAZA_PENDING'
        GROUP BY prayer_type
    """)
    fun observeQazaBalance(userId: String): Flow<List<QazaCountRow>>

    // ── Analytics ─────────────────────────────────────────────────────────────

    @Query("""
        SELECT
            SUM(CASE WHEN status = 'CONGREGATION'   THEN 1 ELSE 0 END) as congregation,
            SUM(CASE WHEN status = 'INDIVIDUAL'     THEN 1 ELSE 0 END) as individual,
            SUM(CASE WHEN status IN ('QAZA_PENDING','QAZA_COMPLETED') THEN 1 ELSE 0 END) as missed,
            COUNT(*) as total
        FROM prayer_entries
        WHERE user_id = :userId AND date >= :from AND date <= :to
    """)
    suspend fun getAggregateStats(
        userId: String,
        from: LocalDate,
        to: LocalDate,
    ): AggregateStatsRow

    @Query("""
        SELECT prayer_type,
            SUM(CASE WHEN status IN ('CONGREGATION','INDIVIDUAL') THEN 1 ELSE 0 END) as done,
            COUNT(*) as total
        FROM prayer_entries
        WHERE user_id = :userId AND date >= :from AND date <= :to
        GROUP BY prayer_type
    """)
    suspend fun getPrayerBreakdown(
        userId: String,
        from: LocalDate,
        to: LocalDate,
    ): List<PrayerBreakdownRow>

    @Query("""
        SELECT date,
            SUM(CASE WHEN status IN ('CONGREGATION','INDIVIDUAL') THEN 1 ELSE 0 END) as completedCount
        FROM prayer_entries
        WHERE user_id = :userId AND date >= :from AND date <= :to
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getDailyCompletionPoints(
        userId: String,
        from: LocalDate,
        to: LocalDate,
    ): List<DailyCompletionRow>

    @Query("""
        SELECT date,
            SUM(CASE WHEN status = 'CONGREGATION' THEN 1 ELSE 0 END) as congregation,
            SUM(CASE WHEN status = 'INDIVIDUAL'   THEN 1 ELSE 0 END) as individual,
            SUM(CASE WHEN status IN ('QAZA_PENDING','QAZA_COMPLETED') THEN 1 ELSE 0 END) as qaza
        FROM prayer_entries
        WHERE user_id = :userId AND date >= :from AND date <= :to
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getDailyStatusCounts(
        userId: String,
        from: LocalDate,
        to: LocalDate,
    ): List<DailyStatusCountRow>
}

// ── Result row helpers ────────────────────────────────────────────────────────

data class QazaCountRow(
    @ColumnInfo(name = "prayer_type")  val prayerType: PrayerType,
    @ColumnInfo(name = "pendingCount") val pendingCount: Int,
)

data class AggregateStatsRow(
    @ColumnInfo(name = "congregation") val congregation: Int,
    @ColumnInfo(name = "individual")   val individual: Int,
    @ColumnInfo(name = "missed")       val missed: Int,
    @ColumnInfo(name = "total")        val total: Int,
)

data class PrayerBreakdownRow(
    @ColumnInfo(name = "prayer_type") val prayerType: PrayerType,
    @ColumnInfo(name = "done")        val done: Int,
    @ColumnInfo(name = "total")       val total: Int,
)

data class DailyCompletionRow(
    @ColumnInfo(name = "date")           val date: LocalDate,
    @ColumnInfo(name = "completedCount") val completedCount: Int,
)

data class DailyStatusCountRow(
    @ColumnInfo(name = "date")         val date: LocalDate,
    @ColumnInfo(name = "congregation") val congregation: Int,
    @ColumnInfo(name = "individual")   val individual: Int,
    @ColumnInfo(name = "qaza")         val qaza: Int,
)