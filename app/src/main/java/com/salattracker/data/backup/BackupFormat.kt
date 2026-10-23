package com.salattracker.data.backup

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.Mosque
import com.salattracker.domain.model.MosqueMode
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.UserPrefs
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalTime
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Backup wire format: versioned JSON, independent of the Room schema, so a backup
 * taken today can still be restored after a future entity/migration change.
 *
 * Bump [BACKUP_FORMAT_VERSION] only when the JSON shape itself changes incompatibly.
 * A device running an older app than the format it downloaded refuses to restore it
 * (see [BackupCodec.decode]) rather than guessing at an unknown shape.
 */
const val BACKUP_FORMAT_VERSION = 1

/** Thrown by [BackupCodec.decode] for anything corrupt, truncated, or from a newer/unknown format. */
class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

// ── Typed snapshot (domain-flavored, no Room imports) ─────────────────────────

data class BackupPrayerEntry(
    val date: LocalDate,
    val prayerType: PrayerType,
    val status: PrayerStatus,
    val updatedAt: Long,
)

data class BackupGoal(
    val id: Long,
    val name: String,
    val category: GoalCategory,
    val frequency: GoalFrequency,
    val targetAmount: Int,
    val unit: String,
    val reminderEnabled: Boolean,
    val reminderTime: LocalTime?,
    val startDate: LocalDate,
    val durationDays: Int,
    val isActive: Boolean,
    /** Weekday bitmask for DAILY goals (bit = DayOfWeek.value - 1); 0 = every day. */
    val daysOfWeek: Int = 0,
    val endsAfterDuration: Boolean = false,
    /** Explicitly allows this goal to receive one check-in for a completed Tasbih round. */
    val usesTasbihCounter: Boolean = false,
)

data class BackupCheckIn(
    val goalId: Long,
    val date: LocalDate,
    val amount: Int,
    val isSkip: Boolean = false,
)

data class BackupSnapshot(
    val prayerEntries: List<BackupPrayerEntry>,
    val goals: List<BackupGoal>,
    val checkIns: List<BackupCheckIn>,
    /** Highest streak per prayer type — current streak is always recomputed after restore, not stored. */
    val highestStreaks: Map<PrayerType, Int>,
    val settings: UserPrefs,
    /** Saved Mosque Mode places. The on/off switch is deliberately not backed up: permissions don't transfer to a new phone. */
    val mosques: List<Mosque> = emptyList(),
)

/** Everything needed to decide whether a remote backup is safe to overwrite, without downloading it. */
data class BackupMeta(
    val formatVersion: Int,
    val revisionId: String,
    val createdAt: Long,
    val appVersionCode: Int,
    val deviceModel: String,
    val prayerEntryCount: Int,
    val oldestPrayerDate: LocalDate?,
    val contentSha256: String,
)

// ── Gson DTOs — every field nullable, since Gson bypasses constructors and leaves a
// missing/corrupt JSON field as null regardless of the Kotlin declared type. ──────

private data class PrayerEntryDto(
    @SerializedName("date") val date: String?,
    @SerializedName("prayerType") val prayerType: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("updatedAt") val updatedAt: Long?,
)

private data class GoalDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("name") val name: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("frequency") val frequency: String?,
    @SerializedName("targetAmount") val targetAmount: Int?,
    @SerializedName("unit") val unit: String?,
    @SerializedName("reminderEnabled") val reminderEnabled: Boolean?,
    @SerializedName("reminderTime") val reminderTime: String?,
    @SerializedName("startDate") val startDate: String?,
    @SerializedName("durationDays") val durationDays: Int?,
    @SerializedName("isActive") val isActive: Boolean?,
    // Added after format v1 shipped; absent in older backups and then default to every-day / ongoing.
    @SerializedName("daysOfWeek") val daysOfWeek: Int?,
    @SerializedName("endsAfterDuration") val endsAfterDuration: Boolean?,
    @SerializedName("usesTasbihCounter") val usesTasbihCounter: Boolean?,
)

private data class CheckInDto(
    @SerializedName("goalId") val goalId: Long?,
    @SerializedName("date") val date: String?,
    @SerializedName("amount") val amount: Int?,
    @SerializedName("isSkip") val isSkip: Boolean?,
)

private data class MosqueDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("name") val name: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("radiusMeters") val radiusMeters: Int?,
)

private data class StreakDto(
    @SerializedName("prayerType") val prayerType: String?,
    @SerializedName("highestStreak") val highestStreak: Int?,
)

private data class SettingsDto(
    @SerializedName("useDeviceLocation") val useDeviceLocation: Boolean?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("calculationMethodId") val calculationMethodId: Int?,
    @SerializedName("reminderEnabled") val reminderEnabled: Map<String, Boolean>?,
    @SerializedName("reminderOffsetMinutes") val reminderOffsetMinutes: Map<String, Int>?,
    @SerializedName("reminderSnoozeMinutes") val reminderSnoozeMinutes: Map<String, Int>?,
    @SerializedName("reminderFullScreenAlert") val reminderFullScreenAlert: Map<String, Boolean>?,
    @SerializedName("reminderVibrationEnabled") val reminderVibrationEnabled: Map<String, Boolean>?,
    @SerializedName("reminderSoundUri") val reminderSoundUri: Map<String, String?>?,
    @SerializedName("use24HourFormat") val use24HourFormat: Boolean?,
    @SerializedName("language") val language: String?,
    @SerializedName("themeMode") val themeMode: String?,
    // Added after format v1; absent in older backups. madhab == null is also how a restore
    // knows the backed-up device hadn't chosen one yet, so it decodes to madhabChosen = false
    // and the one-time prompt still shows after restoring an old backup.
    @SerializedName("madhab") val madhab: String?,
    @SerializedName("highLatitudeRule") val highLatitudeRule: String?,
    @SerializedName("prayerTimeAdjustMinutes") val prayerTimeAdjustMinutes: Map<String, Int>?,
    @SerializedName("jumuahEnabled") val jumuahEnabled: Boolean?,
    @SerializedName("jumuahReminderLeadMinutes") val jumuahReminderLeadMinutes: Int?,
    // Added after format v1; absent in older backups, which then restore with gender unset (null).
    @SerializedName("gender") val gender: String?,
    @SerializedName("locationLabel") val locationLabel: String?,
)

private data class PayloadDto(
    @SerializedName("prayerEntries") val prayerEntries: List<PrayerEntryDto>?,
    @SerializedName("goals") val goals: List<GoalDto>?,
    @SerializedName("checkIns") val checkIns: List<CheckInDto>?,
    @SerializedName("streaks") val streaks: List<StreakDto>?,
    @SerializedName("settings") val settingsDto: SettingsDto?,
    // Added after format v1; absent in older backups, which then restore with no saved mosques.
    @SerializedName("mosques") val mosques: List<MosqueDto>?,
)

private data class MetaDto(
    @SerializedName("formatVersion") val formatVersion: Int?,
    @SerializedName("revisionId") val revisionId: String?,
    @SerializedName("createdAt") val createdAt: Long?,
    @SerializedName("appVersionCode") val appVersionCode: Int?,
    @SerializedName("deviceModel") val deviceModel: String?,
    @SerializedName("prayerEntryCount") val prayerEntryCount: Int?,
    @SerializedName("oldestPrayerDate") val oldestPrayerDate: String?,
    @SerializedName("contentSha256") val contentSha256: String?,
)

private data class BackupFileDto(
    @SerializedName("meta") val meta: MetaDto?,
    @SerializedName("payload") val payload: PayloadDto?,
)

/**
 * Encodes/decodes [BackupSnapshot]s to/from the gzipped JSON bytes stored in Drive.
 * Uses its own [Gson] instance rather than the shared app-wide one, so a future change
 * to the shared instance (e.g. custom type adapters for a different purpose) can never
 * silently change the backup wire format.
 */
object BackupCodec {

    private val gson = GsonBuilder().create()

    /** Deterministic SHA-256 over the payload only (not meta) — used to decide "did anything change". */
    fun contentSha256(snapshot: BackupSnapshot): String = payloadSha256(snapshot.toPayloadDto())

    /** Hashes the DTO exactly as it is/would be serialized — used on both the encode
     * side (via [contentSha256]) and the decode side, so the comparison is byte-for-byte
     * against what was actually serialized, unaffected by any later semantic filtering
     * (e.g. dropping an orphaned check-in) applied when building the domain [BackupSnapshot]. */
    private fun payloadSha256(payload: PayloadDto): String {
        val bytes = gson.toJson(payload).toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun payloadSha256(element: JsonElement): String {
        val bytes = gson.toJson(element).toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun encode(snapshot: BackupSnapshot, meta: BackupMeta): ByteArray {
        val fileDto = BackupFileDto(meta = meta.toDto(), payload = snapshot.toPayloadDto())
        val json = gson.toJson(fileDto)
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(json.toByteArray(Charsets.UTF_8)) }
        return out.toByteArray()
    }

    /**
     * Fully validates before returning anything: a newer format, missing field, unknown
     * enum, or hash mismatch throws [BackupFormatException]. Callers must treat a thrown
     * decode as "abort, local data untouched" — nothing here is partially applied.
     */
    fun decode(gzipped: ByteArray): Pair<BackupMeta, BackupSnapshot> {
        val json = try {
            GZIPInputStream(gzipped.inputStream()).use { it.readBytes() }.toString(Charsets.UTF_8)
        } catch (e: Exception) {
            throw BackupFormatException("Backup file is not valid gzip", e)
        }

        val fileDto = try {
            gson.fromJson(json, BackupFileDto::class.java)
                ?: throw BackupFormatException("Backup file is empty")
        } catch (e: BackupFormatException) {
            throw e
        } catch (e: Exception) {
            throw BackupFormatException("Backup file is not valid JSON", e)
        }

        val metaDto = fileDto.meta ?: throw BackupFormatException("Backup file has no metadata")
        val formatVersion = metaDto.formatVersion
            ?: throw BackupFormatException("Backup file has no format version")
        if (formatVersion > BACKUP_FORMAT_VERSION) {
            throw BackupFormatException(
                "Backup format $formatVersion is newer than this app supports ($BACKUP_FORMAT_VERSION)"
            )
        }

        val payloadDto = fileDto.payload ?: throw BackupFormatException("Backup file has no payload")

        val expectedHash = metaDto.contentSha256
        val actualHash = payloadSha256(payloadDto)
        if (!expectedHash.isNullOrBlank()) {
            val rawPayloadElement = try {
                gson.fromJson(json, JsonObject::class.java)?.get("payload")
            } catch (_: Exception) {
                null
            }
            val rawHash = rawPayloadElement?.let { payloadSha256(it) }
            if (expectedHash != actualHash && (rawHash == null || expectedHash != rawHash)) {
                throw BackupFormatException("Backup content hash does not match — file may be corrupt")
            }
        }

        // Only after the hash is verified do we map to the domain snapshot, which may drop
        // entries that don't belong (e.g. a check-in whose goal isn't in this same backup).
        val snapshot = payloadDto.toSnapshot()

        val meta = BackupMeta(
            formatVersion    = formatVersion,
            revisionId       = metaDto.revisionId ?: throw BackupFormatException("Backup file has no revision id"),
            createdAt        = metaDto.createdAt ?: 0L,
            appVersionCode   = metaDto.appVersionCode ?: 0,
            deviceModel      = metaDto.deviceModel ?: "",
            prayerEntryCount = snapshot.prayerEntries.size,
            oldestPrayerDate = snapshot.prayerEntries.minOfOrNull { it.date },
            contentSha256    = expectedHash.takeIf { !it.isNullOrBlank() } ?: actualHash,
        )
        return meta to snapshot
    }

    // ── snapshot <-> DTO mapping ────────────────────────────────────────────────

    private fun BackupSnapshot.toPayloadDto() = PayloadDto(
        prayerEntries = prayerEntries.map {
            PrayerEntryDto(
                date       = it.date.toString(),
                prayerType = it.prayerType.name,
                status     = it.status.name,
                updatedAt  = it.updatedAt,
            )
        },
        goals = goals.map {
            GoalDto(
                id              = it.id,
                name            = it.name,
                category        = it.category.name,
                frequency       = it.frequency.name,
                targetAmount    = it.targetAmount,
                unit            = it.unit,
                reminderEnabled = it.reminderEnabled,
                reminderTime    = it.reminderTime?.toString(),
                startDate       = it.startDate.toString(),
                durationDays    = it.durationDays,
                isActive        = it.isActive,
                daysOfWeek      = it.daysOfWeek,
                endsAfterDuration = it.endsAfterDuration,
                usesTasbihCounter = it.usesTasbihCounter,
            )
        },
        checkIns = checkIns.map {
            CheckInDto(goalId = it.goalId, date = it.date.toString(), amount = it.amount, isSkip = it.isSkip)
        },
        streaks = PrayerType.values().map { type ->
            StreakDto(prayerType = type.name, highestStreak = highestStreaks[type] ?: 0)
        },
        settingsDto = SettingsDto(
            useDeviceLocation       = settings.useDeviceLocation,
            latitude                = settings.latitude,
            longitude               = settings.longitude,
            calculationMethodId     = settings.calculationMethodId,
            reminderEnabled         = PrayerType.values().associate { it.name to (settings.reminderEnabled[it] ?: true) },
            reminderOffsetMinutes   = PrayerType.values().associate { it.name to (settings.reminderOffsetMinutes[it] ?: 0) },
            reminderSnoozeMinutes   = PrayerType.values().associate { it.name to (settings.reminderSnoozeMinutes[it] ?: 10) },
            reminderFullScreenAlert = PrayerType.values().associate { it.name to (settings.reminderFullScreenAlert[it] ?: true) },
            reminderVibrationEnabled = PrayerType.values().associate { it.name to (settings.reminderVibrationEnabled[it] ?: true) },
            reminderSoundUri = PrayerType.values().associate { type ->
                type.name to settings.reminderSoundUri[type]?.takeUnless { it.startsWith("content:") }
            },
            use24HourFormat         = settings.use24HourFormat,
            language                = settings.language.name,
            themeMode               = settings.themeMode.name,
            // null when the source device hasn't chosen a madhab yet, so a restore on any
            // device knows to still show the one-time prompt (see SettingsDto/toDomain).
            madhab                  = if (settings.madhabChosen) settings.madhab.name else null,
            highLatitudeRule        = settings.highLatitudeRule.name,
            prayerTimeAdjustMinutes = PrayerType.values().associate { it.name to (settings.prayerTimeAdjustMinutes[it] ?: 0) },
            jumuahEnabled           = settings.jumuahEnabled,
            jumuahReminderLeadMinutes = settings.jumuahReminderLeadMinutes,
            gender                  = settings.gender?.name,
            locationLabel           = settings.locationLabel,
        ),
        mosques = mosques.map {
            MosqueDto(id = it.id, name = it.name, latitude = it.latitude, longitude = it.longitude, radiusMeters = it.radiusMeters)
        },
    )

    private fun PayloadDto.toSnapshot(): BackupSnapshot {
        val goals = (goals ?: emptyList()).map { it.toDomain() }
        val goalIds = goals.map { it.id }.toSet()

        return BackupSnapshot(
            prayerEntries = (prayerEntries ?: emptyList()).map { it.toDomain() },
            goals         = goals,
            // Drop check-ins whose goal no longer has a matching entry in this same backup —
            // never leave a dangling goal_id in restored data.
            checkIns      = (checkIns ?: emptyList()).map { it.toDomain() }.filter { it.goalId in goalIds },
            highestStreaks = (streaks ?: emptyList()).associate { dto ->
                val type = PrayerType.valueOf(
                    dto.prayerType ?: throw BackupFormatException("Streak entry missing prayerType")
                )
                type to (dto.highestStreak ?: 0)
            },
            settings = (settingsDto ?: throw BackupFormatException("Backup file has no settings")).toDomain(),
            mosques  = (mosques ?: emptyList()).map { it.toDomain() },
        )
    }

    private fun PrayerEntryDto.toDomain() = BackupPrayerEntry(
        date       = LocalDate.parse(date ?: throw BackupFormatException("Prayer entry missing date")),
        prayerType = PrayerType.valueOf(prayerType ?: throw BackupFormatException("Prayer entry missing prayerType")),
        status     = PrayerStatus.valueOf(status ?: throw BackupFormatException("Prayer entry missing status")),
        updatedAt  = updatedAt ?: throw BackupFormatException("Prayer entry missing updatedAt"),
    )

    private fun GoalDto.toDomain() = BackupGoal(
        id              = id ?: throw BackupFormatException("Goal missing id"),
        name            = name ?: throw BackupFormatException("Goal missing name"),
        category        = GoalCategory.valueOf(category ?: throw BackupFormatException("Goal missing category")),
        frequency       = GoalFrequency.valueOf(frequency ?: throw BackupFormatException("Goal missing frequency")),
        targetAmount    = targetAmount ?: throw BackupFormatException("Goal missing targetAmount"),
        unit            = unit ?: "",
        reminderEnabled = reminderEnabled ?: false,
        reminderTime    = reminderTime?.let(LocalTime::parse),
        startDate       = LocalDate.parse(startDate ?: throw BackupFormatException("Goal missing startDate")),
        durationDays    = durationDays ?: 0,
        isActive        = isActive ?: true,
        daysOfWeek      = daysOfWeek ?: 0,
        endsAfterDuration = endsAfterDuration ?: false,
        usesTasbihCounter = usesTasbihCounter ?: false,
    )

    private fun MosqueDto.toDomain() = Mosque(
        id           = id ?: throw BackupFormatException("Mosque missing id"),
        name         = name ?: throw BackupFormatException("Mosque missing name"),
        latitude     = latitude ?: throw BackupFormatException("Mosque missing latitude"),
        longitude    = longitude ?: throw BackupFormatException("Mosque missing longitude"),
        radiusMeters = radiusMeters ?: MosqueMode.DEFAULT_RADIUS_METERS,
    )

    private fun CheckInDto.toDomain() = BackupCheckIn(
        goalId = goalId ?: throw BackupFormatException("Check-in missing goalId"),
        date   = LocalDate.parse(date ?: throw BackupFormatException("Check-in missing date")),
        amount = amount ?: 0,
        isSkip = isSkip ?: false,
    )

    private fun SettingsDto.toDomain(): UserPrefs {
        fun prayerMapOf(raw: Map<String, Int>?, default: Int): Map<PrayerType, Int> =
            PrayerType.values().associateWith { type ->
                raw?.get(type.name) ?: default
            }
        fun prayerBoolMapOf(raw: Map<String, Boolean>?, default: Boolean): Map<PrayerType, Boolean> =
            PrayerType.values().associateWith { type ->
                raw?.get(type.name) ?: default
            }

        return UserPrefs(
            useDeviceLocation       = useDeviceLocation ?: true,
            latitude                = latitude ?: 0.0,
            longitude               = longitude ?: 0.0,
            calculationMethodId     = calculationMethodId ?: 3,
            reminderEnabled         = prayerBoolMapOf(reminderEnabled, true),
            reminderOffsetMinutes   = prayerMapOf(reminderOffsetMinutes, 0),
            reminderSnoozeMinutes   = prayerMapOf(reminderSnoozeMinutes, 10),
            reminderFullScreenAlert = prayerBoolMapOf(reminderFullScreenAlert, true),
            reminderVibrationEnabled = prayerBoolMapOf(reminderVibrationEnabled, true),
            reminderSoundUri = PrayerType.values().associateWith { type ->
                reminderSoundUri?.get(type.name)?.takeUnless { it.startsWith("content:") }
            },
            use24HourFormat         = use24HourFormat ?: false,
            language = language?.let { runCatching { com.salattracker.domain.model.AppLanguage.valueOf(it) }.getOrNull() }
                ?: com.salattracker.domain.model.AppLanguage.ENGLISH,
            themeMode = themeMode?.let { runCatching { com.salattracker.domain.model.ThemeMode.valueOf(it) }.getOrNull() }
                ?: com.salattracker.domain.model.ThemeMode.SYSTEM,
            // A null madhab means the backed-up device hadn't chosen one, which is also true
            // of every backup taken before this field existed — both cases should still show
            // the one-time prompt after restoring, so madhabChosen tracks nullness directly.
            madhab = madhab?.let { runCatching { com.salattracker.domain.model.Madhab.valueOf(it) }.getOrNull() }
                ?: com.salattracker.domain.model.Madhab.STANDARD,
            madhabChosen = madhab != null,
            highLatitudeRule = highLatitudeRule?.let { runCatching { com.salattracker.domain.model.HighLatitudeRule.valueOf(it) }.getOrNull() }
                ?: com.salattracker.domain.model.HighLatitudeRule.AUTOMATIC,
            prayerTimeAdjustMinutes = prayerMapOf(prayerTimeAdjustMinutes, 0),
            jumuahEnabled = jumuahEnabled ?: true,
            jumuahReminderLeadMinutes = jumuahReminderLeadMinutes ?: 0,
            gender = gender?.let { runCatching { com.salattracker.domain.model.Gender.valueOf(it) }.getOrNull() },
            locationLabel = locationLabel,
        )
    }

    private fun BackupMeta.toDto() = MetaDto(
        formatVersion    = formatVersion,
        revisionId       = revisionId,
        createdAt        = createdAt,
        appVersionCode   = appVersionCode,
        deviceModel      = deviceModel,
        prayerEntryCount = prayerEntryCount,
        oldestPrayerDate = oldestPrayerDate?.toString(),
        contentSha256    = contentSha256,
    )
}
