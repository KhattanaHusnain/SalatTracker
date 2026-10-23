# 80 — Rename the app to "Istiqamah: The Salah Tracker"

**Severity:** Medium · **Type:** Product / branding task

## What
The app is currently called **"Salah Tracker"**. It will be renamed **"Istiqamah: The Salah Tracker"** (*istiqāmah* — steadfastness, staying consistent).

## Decisions needed before changing strings
1. **Short vs full name.** 28 characters is too long for a home-screen label: most launchers show ~10–12 characters, so it would truncate to "Istiqamah: T…". Recommended:
   - `app_name` (launcher, recents, system settings): **Istiqamah**
   - new `app_full_name`: **Istiqamah: The Salah Tracker** — for the Play Store title (30-char limit, fits), onboarding welcome, About/Developers, and the Google sign-in consent screen.
2. **Script in Arabic/Urdu.** Today the name stays in Latin script in every language ("مرحبًا بك في Salah Tracker"). Options: keep "Istiqamah" in Latin everywhere (brand consistency), or use the native word — Arabic **استقامة**, Urdu **استقامت** — with the subtitle translated. Pick one and apply it to all four locales.
3. **Tagline translations** for "The Salah Tracker" in ar / ur / fr, if the full name is localized.

## Change these (12 strings × 4 locales = 48 entries)
In `app/src/main/res/values/strings.xml` and `values-ar`, `values-ur`, `values-fr` (same keys):

| Key | Current (en) | Use |
|---|---|---|
| `app_name` | Salah Tracker | short name |
| `topbar_default` | Salah Tracker | short name |
| `onboarding_welcome_title` | Welcome to Salah Tracker | short or full name |
| `backup_notif_remote_changed_body` | Open Salah Tracker to choose… | short name |
| `donate_title` | Support Salah Tracker | short name |
| `donate_body` | Salah Tracker is built and maintained… | short name |
| `donate_email_subject` | Donation — Salah Tracker | short name |
| `donate_email_body` | …support Salah Tracker's development | short name |
| `developers_subtitle` | The people behind Salah Tracker | short name |
| `developer_contribute_body` | Salah Tracker is open to contributions… | short name |
| `developer_contribute_email_subject` | Contributing — Salah Tracker | short name |
| `developer_contribute_email_body` | …contribute to Salah Tracker… | short name |

The manifest already uses `@string/app_name` (`AndroidManifest.xml:21`), so the launcher label follows `app_name`.

## Also update (outside the app's strings)
- **Google Cloud OAuth consent screen** (project `salat-tracker-sowp`): the app name shown when connecting Google Drive backup.
- **Play Console**: store listing title, short/full description, screenshots and feature graphic that show the old name; privacy policy and support pages.
- `NetworkModule.kt:81` User-Agent `"SalatTracker/…"` (sent to the Overpass API for Mosque search): update to the new name for API etiquette.
- `settings.gradle.kts:25` `rootProject.name = "Salat Tracker"` (cosmetic, IDE only).
- Docs that mention the name (`FUTURE_FEATURES.md`, `QURAN_CONTENT_SOURCES.md`, README if any).

## Do NOT change (would break existing installs or backups)
| Identifier | Where | Why |
|---|---|---|
| `applicationId = "com.salattracker"` | `app/build.gradle.kts:19` | Play identity; changing it makes a different app and existing users can't update |
| `namespace` / package `com.salattracker` | `app/build.gradle.kts:11` | Internal only; renaming is churn with no user benefit |
| Database file `salat_tracker.db` | `di/DatabaseModule.kt:31` | Renaming orphans every user's prayer history |
| Drive backup file `salat-backup.json.gz` | `data/remote/api/DriveApiService.kt:8` | Renaming makes existing backups invisible to restore |
| WorkManager / DataStore / notification-channel IDs | various | Renaming duplicates work and loses saved settings / channel preferences |

## Done when
- The launcher, recents, notifications, onboarding, About/Donate/Developers, emails and the Google sign-in consent screen all show the new name, in all four languages.
- An existing install updates in place with all data, settings and Drive backups intact.
