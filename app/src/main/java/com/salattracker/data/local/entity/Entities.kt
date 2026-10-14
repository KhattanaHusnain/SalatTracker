package com.salattracker.data.local.entity

import androidx.room.*
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
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

// ── Verse cache entity ────────────────────────────────────────────────────────
// Curated prayer-related verses fetched from the Quran.com API and cached so
// they're available offline once fetched at least once.

@Entity(tableName = "verse_cache")
data class VerseCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "verse_key")  val verseKey: String,     // e.g. "2:238"
    @ColumnInfo(name = "text")       val text: String,
    @ColumnInfo(name = "citation")   val citation: String,     // e.g. "Al-Baqarah 2:238"
    @ColumnInfo(name = "cached_at")  val cachedAt: Long = System.currentTimeMillis(),
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