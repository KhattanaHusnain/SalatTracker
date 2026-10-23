# 72 — Calculation method defaults to Muslim World League everywhere

**Severity:** High · **Area:** Bug / prayer-time correctness

## Problem
Every install starts with `calculationMethodId = 3` (MWL), and nothing suggests a better method from the user's location — not onboarding's Location Confirm step (which shows the method with a small "Change" link), and not a later location change. Many regions follow a different authority, and the differences are large enough that users compare against their local mosque and conclude the app is wrong:

| Region | Usual method | Difference vs MWL (typical) |
|---|---|---|
| Pakistan / India / Bangladesh | Karachi (18° / 18°) | Isha a few minutes later |
| North America | ISNA (15° / 15°) | Fajr ~15–25 min later, Isha earlier |
| Saudi Arabia | Umm al-Qura (Isha 90 min after Maghrib) | Isha differs by 10–20+ min |
| Gulf, Egypt, Turkey, Malaysia, Indonesia, Singapore, France, Russia… | Their own presets (all in `CalculationMethod`) | Varies |

The app already supports all of these; it just never picks one.

## Where
- `app/src/main/java/com/salattracker/domain/repository/SettingsRepository.kt:136` (default 3)
- `app/src/main/java/com/salattracker/feature/onboarding/OnboardingViewModel.kt` (`calculationMethod = CalculationMethod.DEFAULT`)
- `app/src/main/java/com/salattracker/data/location/GeocodingRepositoryImpl.kt` (already has the country from reverse geocoding)

## Suggested fix
- Add `CalculationMethod.suggestedFor(countryCode)` (ISO country → method; MWL fallback) as a pure, tested domain function.
- In onboarding, preselect it once the city is known and show why: "Karachi method — commonly used in Pakistan".
- When the location changes to a different country (Edit Location, device location), offer to switch the method; never change it silently once the user picked one.

## Done when
A new user in Pakistan, the US, Saudi Arabia or Turkey gets their region's standard method by default.
