# 74 — Ja'fari users get two alarms at once (Dhuhr+Asr, Maghrib+Isha)

**Severity:** Medium · **Area:** Bug / reminders

## Problem
For the Ja'fari school, the calculator sets Asr's time equal to Dhuhr's and Isha's equal to Maghrib's (combined prayers). `AlarmScheduler` then schedules five independent alarms, so at Dhuhr time **two** alarms fire together:
- two notifications (ids differ by prayer type) with two sounds;
- two full-screen alarms. The second arrives via `onNewIntent`, which calls `recreate()`, so the Dhuhr screen flashes and is replaced by the Asr one.

The same happens at Maghrib (with Isha). The labels are already combined ("Zuhrain", "Maghribain"), so the user sees duplicates of one combined prayer.

## Where
- `app/src/main/java/com/salattracker/data/prayertimes/AdhanPrayerTimeCalculator.kt:72-74`
- `app/src/main/java/com/salattracker/notification/AlarmScheduler.kt` (`schedule` — no madhab awareness)
- `app/src/main/java/com/salattracker/notification/PrayerReminderActivity.kt` (`onNewIntent` → `recreate()`)

## Suggested fix
For Ja'fari, schedule one alarm per combined pair (Dhuhr and Maghrib), and let that one notification/alarm screen offer marking both prayers (e.g. "Mark Zuhrain prayed" writes Dhuhr + Asr). Skip the Asr/Isha alarms, or use their own reminder settings only if the user explicitly enables separate reminders.

## Done when
A Ja'fari user gets exactly one reminder per combined prayer time.
