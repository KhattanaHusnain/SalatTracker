# 35 — Lint is red (7 errors), including plural grammar bugs users will see

**Severity:** Medium · **Area:** QA / build health & i18n

## Problem
`./gradlew :app:lintDebug` fails on every run. The 7 errors have been treated as an accepted "baseline", which means lint can't be used as a quality gate and new errors slip in unnoticed. Several are real bugs:

| # | Issue | Where | User impact |
|---|---|---|---|
| 1–4 | `MissingPermission`: posting notifications without checking `POST_NOTIFICATIONS` | `notification/BackupNotifier.kt:51`, `notification/GoalReminderReceiver.kt:80`, `notification/PrayerReminderReceiver.kt:112, 140` | No crash on current Android (the post is dropped), but nothing records or surfaces that reminders were blocked — see bug 17 |
| 5 | `LocalContextGetResourceValueCall`: `context.getString` in Compose | `feature/bulkupdate/BulkUpdateScreen.kt:530` | Text doesn't update on in-app language change until the screen is recreated |
| 6–7 | `MissingQuantity`: Arabic plurals define only `one`/`other` | `res/values-ar/strings.xml:461, 465` | Wrong grammatical forms for 0, 2, 3–10 and 11–99 in Arabic |

Related warnings with visible impact:
- `MissingQuantity` (French `many`) — `res/values-fr/strings.xml:461, 465`.
- `PluralsCandidate` ×8 — strings like `"%d prayers"`, `"%d verses"`, `"%d Qaza"`, `"Add %1$d prayers"` render as **"1 prayers"** / **"1 verses"** (`res/values/strings.xml:37, 38, 223, 224, 254, 474, 487, 489`, plus `onboarding_catch_up_*`).
- `UnusedResources` ×43 — dead strings/drawables bloating translation work.

## Suggested fix
- Fix the 7 errors (permission check wrapper for `notify`, `stringResource` in Compose, full Arabic quantities).
- Convert the count strings to `<plurals>` in all four locales.
- Then make lint pass, or create `lint-baseline.xml` for the remaining warnings only and fail the build on any **new** error.

## Done when
`lintDebug` passes, and no count in the UI reads "1 prayers"/"1 verses" in any language.
