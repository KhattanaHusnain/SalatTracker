# 29 — A lot of text is below 12 sp

**Severity:** Low · **Area:** Accessibility / legibility

## Problem
About 50 text styles use 9–11 sp, mostly for labels, captions and legend text. At these sizes Urdu (Noto Nastaliq) and Arabic become hard to read, and contrast-reduced secondary text (`SalatOnSurfaceVariant`) compounds it.

Most affected (count of 9–11 sp uses):
- `feature/stats/StatsScreen.kt` (7)
- `feature/home/HomeScreen.kt` (7)
- `feature/calendar/CalendarScreen.kt` (7)
- `feature/settings/SettingsScreen.kt` (5)
- `feature/bulkupdate/BulkUpdateScreen.kt` (5)
- `feature/onboarding/OnboardingScreen.kt` (4)
- plus Qaza, Goals, Day detail, Edit Location, Quran reader

## Suggested fix
- Set a floor of 12 sp for any text a user needs to read (11 sp only for non-essential decorative micro-labels).
- Move sizes into a small set of typography tokens (`MaterialTheme.typography.labelSmall` etc.) instead of per-call `fontSize`, so they can be tuned once — especially for Nastaliq, which needs more size than Latin at the same sp.

## Done when
No essential text is below 12 sp, and Urdu labels are legible at default font scale.
