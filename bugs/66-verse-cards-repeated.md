# 66 — Random verse card repeated on Home, Calendar and the alarm; adds little on Calendar

**Severity:** Low · **Area:** Feature value / content placement

## Problem
The same random-verse card appears on Home, the full-screen prayer alarm, and in Calendar under both the Month view (`CalendarScreen.kt:105`) and the Week view (`:142`). On Home and the alarm it fits ("a reflection before prayer"). On Calendar it pushes the month grid and day details down, on a screen people open to check or edit a day. It also reloads a different random verse each time, and is always English (bug 04).

## Where
- `app/src/main/java/com/salattracker/feature/calendar/CalendarScreen.kt:103-105, 140-142`
- `app/src/main/java/com/salattracker/feature/calendar/CalendarViewModel.kt` (`loadVerse`, `VerseRepository` dependency)

## Suggested fix
Remove the verse card (and the `VerseRepository` dependency) from Calendar. Keep it on Home and the alarm, and fix its language (bug 04).

## Done when
Verses appear only where they add to the moment, and Calendar leads with the calendar.
