# 05 — Backup/restore silently drops some reminder settings

**Severity:** Medium (was High — re-evaluated: settings reset after restore; no prayer data lost)

## Problem
Google Drive backup includes some per-prayer reminder settings but not others, so after a restore some settings come back and the rest silently reset to defaults.

| Setting | In backup? |
|---|---|
| Reminder on/off, offset | Yes |
| Snooze minutes | **No** |
| Full-screen alert | **No** |
| Vibration | **No** |
| Sound | **No** (a device `content://` URI may be intentionally excluded) |
| Location label (city name) | **No** |

## Where
- `app/src/main/java/com/salattracker/data/backup/BackupFormat.kt:140` — `SettingsDto` field list; also `toPayloadDto()` / `toDomain()` in the same file.
- `app/src/main/java/com/salattracker/data/backup/BackupLocalSource.kt` — `saveSettings(...)` call.
- `SettingsRepository.saveSettings(...)` signature doesn't take these fields.

## Suggested fix
- Add nullable `reminderSnoozeMinutes`, `reminderFullScreenAlert`, `reminderVibrationEnabled`, `locationLabel` to `SettingsDto` (absent in old backups → defaults), and thread them through `saveSettings`.
- For sound: back up only the app-owned values (null = Azan, `device_default`, `""` = silent); drop device-specific `content://` URIs on restore and document it.
- Add round-trip cases to `BackupCodecTest`.

## Done when
Back up → change all reminder settings → restore → every setting matches the backed-up state (except a device-specific sound URI, if excluded by design).
