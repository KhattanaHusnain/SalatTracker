# 30 — Copy mismatches in onboarding and untranslated units

**Severity:** Low · **Area:** UX writing / i18n

## Problems
1. **Welcome subtitle is outdated.** It says *"Please select your preferred language to begin your journey."* but the step now also picks the theme.
2. **Theme section label.** The Welcome step reuses `onboarding_theme_title` ("Choose Your Vibe") as a small section label above the theme chips. It was written as a full-screen headline and reads oddly as a label next to "Language".
3. **Units hardcoded as "m".** Jumu'ah lead-time chips show `"${minutes}m"` and the notification's snooze action shows `"Snooze 10m"` — not localized, and "m" is ambiguous (minutes vs metres) in other languages.
4. **"AH" hardcoded** in onboarding's Hijri date (also in bug 10).

## Where
- `app/src/main/res/values*/strings.xml` — `onboarding_welcome_subtitle`, `onboarding_theme_title`
- `app/src/main/java/com/salattracker/feature/onboarding/OnboardingScreen.kt` — Welcome step section labels; Hijri row
- `app/src/main/java/com/salattracker/feature/reminders/PrayerRemindersScreen.kt:289` — `JumuahLeadChip`
- `app/src/main/java/com/salattracker/notification/PrayerReminderReceiver.kt:107` — snooze action label

## Suggested fix
- Subtitle: e.g. *"Choose your language and theme. You can change both later."* (the design's copy).
- Add a plain `onboarding_theme_label` ("Theme") for the section label.
- Add a plural/format string for minutes (e.g. `%d min`) and use it in both places.

## Done when
Onboarding copy matches what each screen does, and no unit abbreviations are hardcoded.
