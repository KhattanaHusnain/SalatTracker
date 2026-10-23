# 28 — Unlabeled icon buttons (minute stepper) for screen readers

**Severity:** Low · **Area:** Accessibility

> **Corrected 2026-10-23 (re-audit).** The first version said these 32 dp buttons fall below the 48 dp minimum touch target. Compose extends hit-testing for pointer-input nodes smaller than `ViewConfiguration.minimumTouchTargetSize` (48 dp), so the touch target is effectively fine. The **missing labels** remain.

## Problem
- **Minute stepper** (Settings → Adjust prayer times, 5 rows): the − / + icons have no content description, so TalkBack announces "Button, unlabeled" ten times, and the new value isn't announced after a tap.
- Visually the buttons are 32 dp and close together. Touch targets are expanded, but neighbouring expanded targets in a dense row can still overlap, so it's worth checking on a device with TalkBack and with a large font scale.

## Where
- `app/src/main/java/com/salattracker/feature/settings/SettingsComponents.kt:125` and `:141` (`MinuteStepper`)
- `app/src/main/java/com/salattracker/feature/reminders/PrayerRemindersScreen.kt:661` (sound preview button — has a description; size only)

## Suggested fix
Add localized content descriptions ("Decrease Fajr adjustment" / "Increase Fajr adjustment") and `semantics { stateDescription = "+5 min" }` on the value.

## Done when
TalkBack reads every icon-only control with a meaningful label and announces the value after changes.
