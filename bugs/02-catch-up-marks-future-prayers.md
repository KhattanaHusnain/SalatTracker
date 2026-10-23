# 02 — Onboarding Catch Up marks today's upcoming prayers as Qaza

**Severity:** High (data correctness)

## Problem
The onboarding Catch Up step writes `QAZA_PENDING` for all five prayers on every day in the chosen range, **including today**. Prayers whose time hasn't arrived yet today get marked as missed.

Bulk Update already handles this correctly by skipping prayers that aren't markable yet.

## Where
- `app/src/main/java/com/salattracker/feature/onboarding/OnboardingViewModel.kt:247` (`onApplyCatchUp`) — builds `(date, type)` for every type with no markability check.
- Correct reference: `app/src/main/java/com/salattracker/feature/bulkupdate/BulkUpdateViewModel.kt:193` uses `isPrayerMarkable(date, todayTimes?.get(type), today)`.

## Steps to reproduce
1. Fresh install, go through onboarding at e.g. 10:00.
2. Catch Up → "Last 7 days" → Add.
3. Open Home: today's Dhuhr/Asr/Maghrib/Isha already show as Qaza.

## Suggested fix
In `onApplyCatchUp`, fetch today's prayer times via `prayerRepository.getPrayerTimes(LocalDate.now())` and filter pairs with `isPrayerMarkable(...)`, same as Bulk Update. Update `catchUpPrayerCount` (used on the button label) so the count matches what will actually be written, or keep it as an estimate and say so.

## Done when
- Running Catch Up mid-day never marks a not-yet-due prayer.
- A unit test covers it (OnboardingViewModel currently has no tests).
