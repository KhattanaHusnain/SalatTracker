# 10 — Onboarding formats dates differently from the rest of the app

**Severity:** Medium

## Problems
1. **Hijri date** shows as `7/5/1448 AH` on the Location Confirm step; Home and Calendar show `7 Jumada I 1448 AH` via `HijriDate.fullLabel()`. The "AH" suffix is also hardcoded English.
2. **Custom Catch Up range** buttons show raw ISO dates (`Start: 2026-10-23`) via `LocalDate.toString()`. Bulk Update shows `Oct 23`.
3. **Done screen "Location" row** falls back to the calculation method's name when no city name is known, so it can read "Location: Muslim World League".

## Where
- `app/src/main/java/com/salattracker/feature/onboarding/OnboardingScreen.kt:343` (Hijri)
- `app/src/main/java/com/salattracker/feature/onboarding/OnboardingScreen.kt:559` and `:564` (custom range)
- `app/src/main/java/com/salattracker/feature/onboarding/OnboardingScreen.kt:600` (Done summary)
- Reference: `app/src/main/java/com/salattracker/core/ui/Localized.kt:169` (`HijriDate.fullLabel()`)

## Suggested fix
1. Use `hijri.fullLabel()`.
2. Use the same short date formatter as Bulk Update (or the shared helper from bug 09).
3. Fall back to formatted coordinates (`R.string.settings_coordinates_value`) instead of the method name; show the calculation method as its own summary row.

## Done when
Onboarding dates match how the same dates look elsewhere in the app, in all four languages.
