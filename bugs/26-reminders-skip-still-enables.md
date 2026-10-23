# 26 — "Skip for now" on the Reminders step still turns reminders on

**Severity:** High · **Area:** UX / onboarding

## Problems
1. **Skip doesn't skip.** "Skip for now" just moves to the next step. Reminders stay at their default (**on**), and `onFinish()` saves them as enabled — but because the user skipped, the notification permission was never requested. Result: reminders are on in the app but can't show (see bug 17), and the user believes they opted out.
2. **Button label ignores the toggle.** The primary button always says "Turn on reminders", even if the user switched the Prayer reminders toggle off. Tapping it then saves reminders as *off*.

## Where
- `app/src/main/java/com/salattracker/feature/onboarding/OnboardingEntry.kt` — `onRemindersSkip = viewModel::onRemindersNext`
- `app/src/main/java/com/salattracker/feature/onboarding/OnboardingViewModel.kt` — `prayerRemindersEnabled` default `true`; `onFinish()` writes it
- `app/src/main/java/com/salattracker/feature/onboarding/OnboardingScreen.kt` — `RemindersStep` primary button text

## Suggested fix
- Skip: set `prayerRemindersEnabled = false` before advancing, and show "Reminders: Off" on Done.
- Primary button: "Turn on reminders" when the toggle is on (requests permission); "Continue" when off (no permission prompt).

## Done when
Skipping leaves reminders off, and the primary button's label always matches what will happen.
