# 75 — Home rolls to the next day at midnight, while Isha is still valid until Fajr

**Severity:** Medium · **Area:** UX / day boundary

## Problem
Home's "today" changes at 00:00 (`ticker.map { it.toLocalDate() }`). Isha remains valid until Fajr, and many people pray it late. After midnight:
- Home shows the new day's five prayers, all upcoming, so last night's Isha **can't be logged from Home**. The user has to find yesterday in Calendar.
- The next-prayer card counts down to Fajr, but the prayer the user is most likely about to pray (last night's Isha) isn't visible.

Combined with bug 61 (auto-mark after 30 min) and bug 70 (notifications record the tap date), late-Isha users are hit three ways.

## Where
- `app/src/main/java/com/salattracker/feature/home/HomeViewModel.kt:101` (`todayDate`)
- `app/src/main/java/com/salattracker/domain/usecase/HomeDashboard.kt` (current/next prayer)

## Suggested fix
Treat the "prayer day" as running from Fajr to the next Fajr for Home: between midnight and Fajr, keep showing the previous date's timeline (or show last night's Isha as a pending row at the top until Fajr). Keep Calendar on civil dates.

## Done when
At 00:30 a user can open Home and log last night's Isha in one tap.
