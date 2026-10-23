# 78 — Opening Goals from a goal notification stacks a second Goals screen

**Severity:** Low · **Area:** Bug / navigation

## Problem
Tapping a goal reminder opens `MainActivity` with `EXTRA_OPEN_ROUTE = "goals"`. `MainScaffold` handles every deep link with `navController.navigate(initialRoute)`. For the **Goals tab** that pushes a second Goals destination on top of Home instead of switching tabs, so Back goes to Home rather than leaving the app, the bottom bar's tab state is inconsistent, and repeated taps keep stacking. Backup and Mosque Mode are real pushed screens, so plain `navigate` is right for them.

## Where
- `app/src/main/java/com/salattracker/feature/main/MainActivity.kt:262-265` (`navController.navigate(initialRoute)`)
- `app/src/main/java/com/salattracker/notification/GoalReminderReceiver.kt:61` (`ROUTE_GOALS`)

## Suggested fix
For routes that are bottom-nav tabs (`isTabRoute`), use the existing `navigateToTab(...)` helper instead of `navigate`.

## Done when
Opening Goals from a notification lands on the Goals tab exactly as tapping the tab would.
