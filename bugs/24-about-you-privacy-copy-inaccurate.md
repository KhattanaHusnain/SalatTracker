# 24 — "About you" says gender never leaves the device, but it's included in Drive backup

**Severity:** Medium · **Area:** UX / trust & privacy copy

## Problem
Onboarding's About You step says: *"This tailors your prayer tracking. It never leaves your device."* But gender is written into the Google Drive backup (`BackupFormat.kt`, `SettingsDto.gender`). The Location step's copy gets this right ("Included in your Google Drive backup if you turn that on").

A privacy statement that's untrue — on a sensitive field — is a trust problem, and possibly a Play data-safety issue.

## Where
- `app/src/main/res/values*/strings.xml` — `onboarding_about_you_subtitle` (all four locales)
- `app/src/main/java/com/salattracker/data/backup/BackupFormat.kt` — `gender` field

## Suggested fix
Reword to match the Location step, e.g. *"This tailors your prayer tracking. It's stored on this device, and in your Google Drive backup if you turn that on."* Update ar/ur/fr too. Check the Play Console data-safety form.

## Done when
Every privacy statement in onboarding matches what the app actually does with the data.
