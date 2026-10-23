# 09 — ~~Dates use the app language on some screens, the phone language on others~~

**Status:** ❌ Rejected in re-evaluation (2026-10-23) · was Medium

## Original claim
Formatters without an explicit locale (Home, Calendar, Bulk Update, the status sheet, Backup, Quran history) would use the phone's language, while Qaza/Goals/Stats pass the app locale, so month names could differ between screens.

## Why it's rejected
The app sets its language with `AppCompatDelegate.setApplicationLocales` (`core/locale/LocaleManager.kt`). On Android 13+ the framework's per-app language changes the process default locale, and on older versions AppCompat also updates the default locale list when it applies the configuration. So `Locale.getDefault()` follows the app language in both cases, and the two formatter styles produce the same output. A language change also recreates the activity, so the `remember {}`-cached formatters are rebuilt.

## What remains (code tidiness only, no user impact)
Two styles of date formatting exist side by side. When bug 01's shared time/date helper is added, migrate both styles onto it. No separate fix needed.
