# 25 — Onboarding has no visible Back button, and the progress dots are wrong

**Severity:** Medium · **Area:** UX / onboarding navigation

## Problems
1. **No visible Back.** Steps after Welcome can only go back with the system back gesture. The design (and most onboarding flows) show a back button bottom-left; gesture-navigation users often don't realise back works here, especially right after a choice they want to change.
2. **Progress dots don't reflect real progress.** `StepHeader` shows 8 dots (every enum value except Welcome/Done), but the main path visits only some of them: City Search reuses Location Confirm's dot, Catch Up Add reuses Catch Up's, and a user who goes GPS → confirm never lights the City Search position. Dots jump or stall, so "how much is left" is unclear.
3. **Long flow.** Up to 9 screens before reaching the app (Welcome, About you, School, Location, City search, Confirm, Reminders, Catch up, Catch up add, Done). The design targeted 5 progress steps.

## Where
- `app/src/main/java/com/salattracker/feature/onboarding/OnboardingScreen.kt` — `StepHeader` (dot count/index), step composables (no back control)
- `app/src/main/java/com/salattracker/feature/onboarding/OnboardingViewModel.kt` — `OnboardingStep`

## Suggested fix
- Add a back icon button to `StepHeader` (hidden on Welcome and Done), wired to `viewModel.onBack()`.
- Map steps to 5 progress groups (Welcome, About you, Location [incl. school/search/confirm], Reminders, Catch up) and show "Step n of 5" like the design.
- Consider folding School into the Location Confirm screen (as a row, like the calculation method) to shorten the flow.

## Done when
Every mid-flow step has a visible Back, and the progress indicator advances monotonically to the end.
