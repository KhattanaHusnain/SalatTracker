package com.salattracker.data.local.dao

import androidx.room.*
import com.salattracker.data.local.entity.*
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PrayerDao {

    @Query("SELECT MIN(date) FROM prayer_entries WHERE user_id = :userId")
    suspend fun getFirstRecordedPrayerDate(userId: String): LocalDate?

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

    /** Complete local history, oldest first. Used to rebuild derived streak caches. */
    @Query("""
        SELECT * FROM prayer_entries
        WHERE user_id = :userId
        ORDER BY date ASC, prayer_type ASC
    """)
    suspend fun getAllEntries(userId: String): List<PrayerEntryEntity>

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

    @Query("""
        SELECT COUNT(*) FROM prayer_entries
        WHERE user_id = :userId AND status = 'QAZA_COMPLETED'
    """)
    fun observeQazaMadeUpCount(userId: String): Flow<Int>

    /** Every prayer made up (status = QAZA_COMPLETED) on or after [sinceMillis] — [updated_at] is
     * each row's "made up at" timestamp, newest first. Used to compute the pace window on the
     * Qaza tab (see [com.salattracker.domain.usecase.qazaProgress]). */
    @Query("""
        SELECT * FROM prayer_entries
        WHERE user_id = :userId AND status = 'QAZA_COMPLETED' AND updated_at >= :sinceMillis
        ORDER BY updated_at DESC
    """)
    fun observeQazaMadeUpSince(userId: String, sinceMillis: Long): Flow<List<PrayerEntryEntity>>

    /** The most recently made-up prayers, newest first — powers the "Recently made up" section. */
    @Query("""
        SELECT * FROM prayer_entries
        WHERE user_id = :userId AND status = 'QAZA_COMPLETED'
        ORDER BY updated_at DESC
        LIMIT :limit
    """)
    fun observeRecentQazaMadeUp(userId: String, limit: Int): Flow<List<PrayerEntryEntity>>

    // ── Analytics ─────────────────────────────────────────────────────────────
    // Every status-count query returns the same 5 columns (congregation / individual /
    // qazaPending / qazaCompleted / excused) so the domain layer can build one StatusCounts
    // from any of them — see PrayerRepository.StatusCounts.

    @Query("""
        SELECT prayer_type,
            SUM(CASE WHEN status = 'CONGREGATION'   THEN 1 ELSE 0 END) as congregation,
            SUM(CASE WHEN status = 'INDIVIDUAL'     THEN 1 ELSE 0 END) as individual,
            SUM(CASE WHEN status = 'QAZA_PENDING'   THEN 1 ELSE 0 END) as qazaPending,
            SUM(CASE WHEN status = 'QAZA_COMPLETED' THEN 1 ELSE 0 END) as qazaCompleted,
            SUM(CASE WHEN status = 'MENSTRUATION'   THEN 1 ELSE 0 END) as excused
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
            SUM(CASE WHEN status = 'CONGREGATION'   THEN 1 ELSE 0 END) as congregation,
            SUM(CASE WHEN status = 'INDIVIDUAL'     THEN 1 ELSE 0 END) as individual,
            SUM(CASE WHEN status = 'QAZA_PENDING'   THEN 1 ELSE 0 END) as qazaPending,
            SUM(CASE WHEN status = 'QAZA_COMPLETED' THEN 1 ELSE 0 END) as qazaCompleted,
            SUM(CASE WHEN status = 'MENSTRUATION'   THEN 1 ELSE 0 END) as excused
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

data class PrayerBreakdownRow(
    @ColumnInfo(name = "prayer_type")   val prayerType: PrayerType,
    @ColumnInfo(name = "congregation")  val congregation: Int,
    @ColumnInfo(name = "individual")    val individual: Int,
    @ColumnInfo(name = "qazaPending")   val qazaPending: Int,
    @ColumnInfo(name = "qazaCompleted") val qazaCompleted: Int,
    @ColumnInfo(name = "excused")       val excused: Int,
)

data class DailyStatusCountRow(
    @ColumnInfo(name = "date")          val date: LocalDate,
    @ColumnInfo(name = "congregation")  val congregation: Int,
    @ColumnInfo(name = "individual")    val individual: Int,
    @ColumnInfo(name = "qazaPending")   val qazaPending: Int,
    @ColumnInfo(name = "qazaCompleted") val qazaCompleted: Int,
    @ColumnInfo(name = "excused")       val excused: Int,
)
