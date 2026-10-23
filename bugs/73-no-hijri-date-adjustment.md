# 73 — No Hijri date adjustment; dates and Islamic events can be a day off

**Severity:** Medium · **Area:** Bug / Hijri calendar

## Problem
Hijri dates come from Java's built-in `HijrahDate` (the Umm al-Qura calendar). Many countries — notably Pakistan, India, Bangladesh, and much of the UK — start months by local moon sighting, often **one day later** than Umm al-Qura. There's no setting to shift the date, so for many users of this app:
- The Hijri date on Home, Calendar and onboarding is a day off.
- Islamic events (Ramadan start, Laylat al-Qadr, Eid al-Fitr, Arafah, Eid al-Adha) show on the wrong day.

Every major prayer app offers a ±1/±2 day Hijri adjustment for this reason.

## Where
- `app/src/main/java/com/salattracker/domain/usecase/HijriDate.kt` (`toHijriOrNull`)
- `app/src/main/java/com/salattracker/domain/usecase/IslamicEvents.kt`
- Settings has no related control

## Suggested fix
- Add `hijriAdjustmentDays: Int` (−2…+2, default 0) to `UserPrefs` (and backup), and a stepper in Settings → Location & prayer times.
- Apply it inside the conversion (shift the Gregorian date before converting) so Home, Calendar, onboarding and events all agree.
- Optionally suggest −1 when the user's country is one that usually sights locally.

## Done when
A user can align the app's Hijri date and event days with their local announcement.
