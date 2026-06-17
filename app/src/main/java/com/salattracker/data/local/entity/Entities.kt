package com.salattracker.data.local.entity

import androidx.room.*
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import java.time.LocalDate

// ── Converters ────────────────────────────────────────────────────────────────

class Converters {
    @TypeConverter fun fromLocalDate(d: LocalDate): String = d.toString()
    @TypeConverter fun toLocalDate(s: String): LocalDate   = LocalDate.parse(s)

    @TypeConverter fun fromPrayerType(t: PrayerType): String   = t.name
    @TypeConverter fun toPrayerType(s: String): PrayerType     = PrayerType.valueOf(s)

    @TypeConverter fun fromPrayerStatus(s: PrayerStatus): String = s.name
    @TypeConverter fun toPrayerStatus(s: String): PrayerStatus   = PrayerStatus.valueOf(s)

    @TypeConverter fun fromSyncState(s: SyncState): String = s.name
    @TypeConverter fun toSyncState(s: String): SyncState   = SyncState.valueOf(s)
}

enum class SyncState { PENDING, SYNCED, CONFLICT }

// ── PrayerEntry entity ────────────────────────────────────────────────────────

@Entity(
    tableName  = "prayer_entries",
    indices    = [Index(value = ["user_id", "date", "prayer_type"], unique = true)],
)
data class PrayerEntryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")          val id: Long = 0,
    @ColumnInfo(name = "user_id")     val userId: String,
    @ColumnInfo(name = "date")        val date: LocalDate,
    @ColumnInfo(name = "prayer_type") val prayerType: PrayerType,
    @ColumnInfo(name = "status")      val status: PrayerStatus,
    @ColumnInfo(name = "updated_at")  val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sync_state")  val syncState: SyncState = SyncState.PENDING,
)

// ── Streak snapshot entity ────────────────────────────────────────────────────

@Entity(tableName = "streak_snapshots")
data class StreakSnapshotEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")           val userId: String,
    @ColumnInfo(name = "current_streak")    val currentStreak: Int = 0,
    @ColumnInfo(name = "highest_streak")    val highestStreak: Int = 0,
    @ColumnInfo(name = "last_calculated_at")val lastCalculatedAt: Long = 0L,
)

// ── Qaza balance entity ───────────────────────────────────────────────────────

@Entity(
    tableName  = "qaza_balances",
    primaryKeys = ["user_id", "prayer_type"],
)
data class QazaBalanceEntity(
    @ColumnInfo(name = "user_id")      val userId: String,
    @ColumnInfo(name = "prayer_type")  val prayerType: PrayerType,
    @ColumnInfo(name = "pending_count")val pendingCount: Int = 0,
)

// ── Prayer times cache entity ─────────────────────────────────────────────────

@Entity(
    tableName   = "prayer_times_cache",
    primaryKeys = ["date", "latitude", "longitude", "method"],
)
data class PrayerTimeCacheEntity(
    @ColumnInfo(name = "date")       val date: LocalDate,
    @ColumnInfo(name = "latitude")   val latitude: Double,
    @ColumnInfo(name = "longitude")  val longitude: Double,
    @ColumnInfo(name = "method")     val method: Int,
    @ColumnInfo(name = "fajr")       val fajr: String,
    @ColumnInfo(name = "dhuhr")      val dhuhr: String,
    @ColumnInfo(name = "asr")        val asr: String,
    @ColumnInfo(name = "maghrib")    val maghrib: String,
    @ColumnInfo(name = "isha")       val isha: String,
    @ColumnInfo(name = "cached_at")  val cachedAt: Long = System.currentTimeMillis(),
)

// ── Sync queue entity ─────────────────────────────────────────────────────────

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")              val id: Long = 0,
    @ColumnInfo(name = "op_type")         val opType: String,      // "UPSERT" | "DELETE"
    @ColumnInfo(name = "collection_path") val collectionPath: String,
    @ColumnInfo(name = "document_id")     val documentId: String,
    @ColumnInfo(name = "payload_json")    val payloadJson: String,
    @ColumnInfo(name = "created_at")      val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "retry_count")     val retryCount: Int = 0,
)