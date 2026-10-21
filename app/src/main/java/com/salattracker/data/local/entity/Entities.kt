package com.salattracker.data.local.entity

import androidx.room.*
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.QuranContentPackType
import java.time.LocalDate
import java.time.LocalTime

// ── Converters ────────────────────────────────────────────────────────────────

class Converters {
    @TypeConverter fun fromLocalDate(d: LocalDate): String = d.toString()
    @TypeConverter fun toLocalDate(s: String): LocalDate   = LocalDate.parse(s)

    @TypeConverter fun fromLocalTime(t: LocalTime?): String? = t?.toString()
    @TypeConverter fun toLocalTime(s: String?): LocalTime?   = s?.let(LocalTime::parse)

    @TypeConverter fun fromPrayerType(t: PrayerType): String   = t.name
    @TypeConverter fun toPrayerType(s: String): PrayerType     = PrayerType.valueOf(s)

    @TypeConverter fun fromPrayerStatus(s: PrayerStatus): String = s.name
    @TypeConverter fun toPrayerStatus(s: String): PrayerStatus   = PrayerStatus.valueOf(s)

    @TypeConverter fun fromGoalCategory(c: GoalCategory): String = c.name
    @TypeConverter fun toGoalCategory(s: String): GoalCategory   = GoalCategory.valueOf(s)

    @TypeConverter fun fromGoalFrequency(f: GoalFrequency): String = f.name
    @TypeConverter fun toGoalFrequency(s: String): GoalFrequency   = GoalFrequency.valueOf(s)

    @TypeConverter fun fromQuranContentPackType(t: QuranContentPackType): String = t.name
    @TypeConverter fun toQuranContentPackType(s: String): QuranContentPackType   = QuranContentPackType.valueOf(s)
}

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
)

// ── Streak snapshot entity ────────────────────────────────────────────────────
// One row per (user, prayer type) — streaks are tracked independently per prayer.

@Entity(
    tableName   = "streak_snapshots",
    primaryKeys = ["user_id", "prayer_type"],
)
data class StreakSnapshotEntity(
    @ColumnInfo(name = "user_id")            val userId: String,
    @ColumnInfo(name = "prayer_type")        val prayerType: PrayerType,
    @ColumnInfo(name = "current_streak")     val currentStreak: Int = 0,
    @ColumnInfo(name = "highest_streak")     val highestStreak: Int = 0,
    @ColumnInfo(name = "last_calculated_at") val lastCalculatedAt: Long = 0L,
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

// ── Spiritual goal entity ─────────────────────────────────────────────────────

@Entity(tableName = "spiritual_goals")
data class SpiritualGoalEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")               val id: Long = 0,
    @ColumnInfo(name = "name")             val name: String,
    @ColumnInfo(name = "category")         val category: GoalCategory,
    @ColumnInfo(name = "frequency")        val frequency: GoalFrequency,
    @ColumnInfo(name = "target_amount")    val targetAmount: Int,
    @ColumnInfo(name = "unit")             val unit: String,
    @ColumnInfo(name = "reminder_enabled") val reminderEnabled: Boolean,
    @ColumnInfo(name = "reminder_time")    val reminderTime: LocalTime?,
    @ColumnInfo(name = "start_date")       val startDate: LocalDate,
    @ColumnInfo(name = "duration_days")    val durationDays: Int,
    @ColumnInfo(name = "is_active")        val isActive: Boolean = true,
    // Bitmask of due weekdays for DAILY goals (bit = DayOfWeek.value - 1); 0 means every day.
    @ColumnInfo(name = "days_of_week", defaultValue = "0")         val daysOfWeek: Int = 0,
    // When false the goal is ongoing and duration_days is ignored.
    @ColumnInfo(name = "ends_after_duration", defaultValue = "0")  val endsAfterDuration: Boolean = false,
)

// ── Goal check-in entity ──────────────────────────────────────────────────────
// One row per check-in event — progress for a goal's current period is the sum
// of check-ins whose date falls within that period (today / this week / this month).

@Entity(tableName = "goal_check_ins")
data class GoalCheckInEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")      val id: Long = 0,
    @ColumnInfo(name = "goal_id") val goalId: Long,
    @ColumnInfo(name = "date")    val date: LocalDate,
    @ColumnInfo(name = "amount")  val amount: Int,
    // An excused day: neither counted as hit nor as missed, and never breaks a streak.
    @ColumnInfo(name = "is_skip", defaultValue = "0") val isSkip: Boolean = false,
)
// ── Mosque entity (Mosque Mode) ───────────────────────────────────────────────

@Entity(tableName = "mosques")
data class MosqueEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")            val id: Long = 0,
    @ColumnInfo(name = "name")          val name: String,
    @ColumnInfo(name = "latitude")      val latitude: Double,
    @ColumnInfo(name = "longitude")     val longitude: Double,
    @ColumnInfo(name = "radius_meters") val radiusMeters: Int,
)

// ── Quran user-data entities ──────────────────────────────────────────────────
// Per-device state only (bookmarks, history, download registry). The Quran text itself and the
// content those downloaded packs unlock live in their own separate database(s) — see
// data/local/quran/, added alongside the reading UI in a later phase.

@Entity(tableName = "quran_bookmarks")
data class QuranBookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")             val id: Long = 0,
    @ColumnInfo(name = "chapter_number") val chapterNumber: Int,
    @ColumnInfo(name = "from_verse")     val fromVerse: Int,
    @ColumnInfo(name = "to_verse")       val toVerse: Int,
    @ColumnInfo(name = "note")           val note: String? = null,
    @ColumnInfo(name = "created_at")     val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "quran_read_history")
data class QuranReadHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")             val id: Long = 0,
    @ColumnInfo(name = "chapter_number") val chapterNumber: Int,
    @ColumnInfo(name = "verse_number")   val verseNumber: Int,
    @ColumnInfo(name = "read_at")        val readAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "quran_search_history")
data class QuranSearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")           val id: Long = 0,
    @ColumnInfo(name = "query")        val query: String,
    @ColumnInfo(name = "searched_at")  val searchedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "quran_content_packs")
data class QuranContentPackEntity(
    @PrimaryKey
    @ColumnInfo(name = "pack_id")       val packId: String,
    @ColumnInfo(name = "type")          val type: QuranContentPackType,
    @ColumnInfo(name = "language")      val language: String,
    @ColumnInfo(name = "title")         val title: String,
    @ColumnInfo(name = "author")        val author: String? = null,
    @ColumnInfo(name = "size_bytes")    val sizeBytes: Long,
    @ColumnInfo(name = "source_url")    val sourceUrl: String,
    @ColumnInfo(name = "version")       val version: Int,
    @ColumnInfo(name = "is_downloaded") val isDownloaded: Boolean = false,
    @ColumnInfo(name = "local_path")    val localPath: String? = null,
    @ColumnInfo(name = "downloaded_at") val downloadedAt: Long? = null,
)
