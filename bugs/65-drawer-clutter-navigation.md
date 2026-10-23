# 65 — Drawer mixes core tools with low-value pages; important features are buried

**Severity:** Low · **Area:** Feature value / navigation

## Problem
The navigation drawer has 10 items of very different value, in one flat list:

| Item | Value | Comment |
|---|---|---|
| Bulk Update | High (utility) | Fine here |
| Prayer Reminders | High | Belongs with Settings, or reachable from Home's next-prayer card |
| Mosque Mode | Medium (niche) | Fine here |
| Backup | High | Fine here (or under Settings) |
| Stats | Medium | Moved off the bottom nav; currently a secondary destination |
| Tasbih | — | In progress, not reviewed |
| Donate | Low (to user) | Meta page |
| Developers | Low | Meta page |
| Quran credits | Required (licensing), low use | Meta page |
| Settings | High | Usually its own item at the bottom |

Three meta pages (Donate, Developers, Credits) take as much space as daily tools, and Prayer Reminders, one of the most-used settings, is separate from Settings.

## Where
- `app/src/main/java/com/salattracker/feature/main/AppDrawer.kt`

## Suggested fix
- Group the drawer: **Tools** (Bulk Update, Mosque Mode, Tasbih, Stats), then **Settings** (Settings, Prayer Reminders, Backup).
- Move Donate, Developers and Quran credits into a single **About** screen reached from Settings (keep credits reachable — they're required for content licences).
- Add a shortcut from Home's next-prayer card to Prayer Reminders.

## Done when
The drawer shows only things a user does regularly, and meta pages live under About.
