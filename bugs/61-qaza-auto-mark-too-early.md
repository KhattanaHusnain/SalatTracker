# 61 — Qaza auto-mark flags prayers as missed while their time is still valid

**Severity:** High · **Area:** Feature value / correctness

## Problem
`QazaAutoMarkWorker` marks any unlogged prayer as **Qaza pending 30 minutes after its start time**. But each prayer's valid time runs much longer than 30 minutes:
- Fajr: until sunrise (often 1–1.5 h)
- Dhuhr: until Asr
- Asr: until sunset
- Maghrib: until Isha
- Isha: until midnight / Fajr

Someone who prays Isha at 22:00 (on time) has it marked as missed at 20:30. Until they log it, Home, the Qaza tab and Stats count it as a missed prayer, and the Qaza total jumps every evening. The feature is meant to catch truly forgotten prayers; as built it mislabels on-time prayers as missed, which undermines trust in the Qaza numbers.

## Where
- `app/src/main/java/com/salattracker/notification/QazaAutoMarkWorker.kt` — `GRACE_PERIOD_MINUTES = 30`, `markOverdue()`

## Suggested fix
- Auto-mark a prayer only after its window ends, i.e. at the start of the next prayer (Fajr at sunrise, Isha at Fajr of the next day). `adhan2` already provides sunrise; the next prayer's time is available from `getPrayerTimes`.
- Keep a short buffer after the window ends (e.g. 15 min) to allow for logging.
- Optionally make auto-marking a setting ("Mark unlogged prayers as missed: after the prayer's time ends / never").
- Tests: Fajr logged at sunrise − 5 min is never auto-marked; Isha unlogged at next Fajr is.

## Done when
A prayer is never auto-marked as missed while it can still be prayed on time.
