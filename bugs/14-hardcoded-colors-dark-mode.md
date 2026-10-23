# 14 — White text on light-mint gradients in dark mode (Home hero, verse banner, prayer alarm)

**Severity:** Medium (upgraded in re-evaluation) · **Area:** Visual / accessibility · **Confirm visually on a device**

## Problem
Three prominent surfaces put fixed **white** text and icons on a gradient between `SalatPrimaryContainer` and `SalatPrimary`. In light theme that's dark green → darker green, which is fine. In **dark theme** `SalatPrimary` is a light mint (`#95D3BA`), so the gradient runs dark green → light mint, and the white text on the light end has a contrast ratio of about **1.7:1** (WCAG needs 4.5:1 for body text).

| Surface | Gradient | Where |
|---|---|---|
| Home hero card (next prayer, countdown, Hijri date, event chip) | `PrimaryContainer → Primary` | `feature/home/HomeScreen.kt:153` (white text at 159–283) |
| Home verse banner | `PrimaryContainer → Primary` | `feature/home/HomeScreen.kt:545-548` (white text 556–566) |
| Full-screen prayer alarm | `PrimaryContainer → Primary → Black` | `notification/PrayerReminderScreen.kt:76` |

Theme values: `core/theme/Color.kt:83-85` (light) and `:149-151` (dark).

## Smaller fixed-colour issues (lower impact)
- Streak flame `Color(0xFFFF9939)` — `HomeScreen.kt:438`
- Calendar square icon colours `Color(0xFF412402)` / `Color.White` — `CalendarScreen.kt:565`
- Qaza pill colours and the status sheet's orange — see bug 06

## Suggested fix
- Add theme tokens for these hero surfaces, e.g. `heroGradientStart/End` and `onHero`, with dark-theme values that keep both ends dark (e.g. `#0B513D → #06382A`) — or use `SalatOnPrimary`/`SalatOnPrimaryContainer` for text instead of fixed white.
- Check every white-on-gradient element in dark mode with a contrast checker.

## Done when
All text on these surfaces meets 4.5:1 contrast in both light and dark themes.
