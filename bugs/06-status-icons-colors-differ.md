# 06 — Same prayer status uses different icons and colours per screen

**Severity:** Medium

## Problem
Each screen maps `PrayerStatus` to its own icon and colour.

**Icons**

| Status | Status picker / Bulk Update | Day detail | Calendar squares |
|---|---|---|---|
| Qaza pending | `ic_pending_actions` | `ic_history_edu` | `ic_schedule` (also used for "Upcoming") |
| Qaza done | `ic_history_edu` | `ic_check_circle_filled` | `ic_check_circle_filled` |

**Colours**
- Theme tokens: Qaza pending = red `#EF4444`, Qaza done = purple `#A855F7` (`core/theme/Color.kt:136-137`).
- Status picker: Qaza done tinted orange `#FF9939`.
- Day detail: Qaza pending and Qaza done both peach `#FFDCC3`/`#6E3900` — indistinguishable by colour.
- Qaza tab pills: pending peach, made-up purple (own constants).

## Where
- `app/src/main/java/com/salattracker/core/ui/PrayerStatusBottomSheet.kt:239-244`
- `app/src/main/java/com/salattracker/feature/bulkupdate/BulkUpdateScreen.kt:459-463`
- `app/src/main/java/com/salattracker/core/ui/DayDetailBottomSheet.kt:186-207`
- `app/src/main/java/com/salattracker/feature/calendar/CalendarScreen.kt:555-559`
- `app/src/main/java/com/salattracker/feature/qaza/QazaComponents.kt:291-294`
- `app/src/main/java/com/salattracker/core/ui/PrayerStatusRing.kt` (`statusRingColor` — the theme-token version)

## Suggested fix
Add one mapping in `core/ui` — `PrayerStatus.iconRes` and `PrayerStatus.color()` (backed by the `SalatStatus*` theme tokens) — and use it everywhere. Decide one icon per status and give Qaza pending and Qaza done distinct colours.

## Done when
A given status looks identical (icon + colour) on every screen, and all colours come from theme tokens.
