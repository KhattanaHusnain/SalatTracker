# 36 — No tests on several critical paths (Home, onboarding, streaks, alarms, workers)

**Severity:** Medium · **Area:** QA / test coverage

## Current state (2026-10-23)
- **319 unit tests in 30 suites — all passing**; build + assemble OK.
- **Instrumented tests:** none (only the template `ExampleInstrumentedTest`).
- **ViewModels with tests:** 10 of 26 — Qaza, Goals, Stats, Calendar, Settings, BulkUpdate, MosqueMode, GoalDetails, MissedPrayers, EditCoordinates.

## Untested, ranked by risk
| Area | Why it matters |
|---|---|
| `PrayerRepositoryImpl` (streak calculation, bulk upsert, Qaza balance) | Core numbers on Home/Stats; no direct test |
| `OnboardingViewModel` | First-run flow; bugs 02, 19, 20, 26 would have been caught |
| `HomeViewModel` | Main screen; next-prayer/countdown/markability logic |
| `AlarmScheduler` / `PrayerReminderWorker` / `PrayerReminderReceiver` | Reminder timing (bugs 16, 31) |
| `QazaAutoMarkWorker` | Writes Qaza entries automatically |
| `PrayerRemindersViewModel` | Reminder settings, Jumu'ah, gender gating |
| `BackupViewModel` | Restore UX (repository is tested) |
| Room migrations | See bug 32 |
| `DataStoreSettingsRepository`, `GeocodingRepositoryImpl` | Persistence & parsing of every preference |
| Quran ViewModels (7) | Reader/Mushaf/bookmarks/history |
| Compose UI | No screenshot or UI tests for any screen |

## Suggested plan
1. Unit tests for `OnboardingViewModel`, `HomeViewModel`, `PrayerRemindersViewModel` with the existing fakes.
2. Pure-JVM tests for the streak calculation (extract it from `PrayerRepositoryImpl` if needed) and for AlarmScheduler's trigger-time computation (extract `triggerMillis` logic).
3. Instrumented: Room migration test (bug 32), one smoke UI test through onboarding.
4. Enable JaCoCo coverage reporting so gaps are measurable.

## Done when
Every item above has at least happy-path + one edge-case coverage, and coverage is reported on each build.
