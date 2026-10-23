# 07 — Two different sets of status names

**Severity:** Low (was Medium — re-evaluated: wording inconsistency only)

## Problem
The Day detail sheet uses a second, different set of names for the same statuses.

| Status | Most screens (`labelRes`) | Day detail |
|---|---|---|
| CONGREGATION | Congregation | Jamā'at |
| INDIVIDUAL | Individual | Alone |
| QAZA_PENDING | Qaza Pending | Qaza |
| QAZA_COMPLETED | Qaza Completed | Qaza Done |
| MENSTRUATION | Menstruation | Excused |
| NOT_RECORDED | Not Recorded | Not Set |

"Excused" is also used by the Calendar legend, while pickers say "Menstruation".

## Where
- `app/src/main/res/values/strings.xml:111-122` (and the three translated files)
- `app/src/main/java/com/salattracker/core/ui/DayDetailBottomSheet.kt:186-207`
- `app/src/main/java/com/salattracker/core/ui/Localized.kt` (`PrayerStatus.labelRes`)
- `app/src/main/java/com/salattracker/feature/calendar/CalendarScreen.kt:328-342` (legend)

## Suggested fix
Pick one vocabulary (a short form for chips is fine, but derive it from the same source), route Day detail through `PrayerStatus.labelRes` (or a single `shortLabelRes`), and delete the unused strings in all four locales.

## Done when
Each status has one name used consistently across pickers, chips, legends and Stats.
