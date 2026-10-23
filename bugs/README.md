# Bugs and improvements

Every item below was **re-evaluated one by one against the code on 2026-10-23**. This list is the authoritative severity and status; the individual files have the details, evidence and suggested fixes. Paths in the files are relative to the repo root, and line numbers may drift.

**Totals:** 80 logged → 78 open (12 High · 40 Medium · 26 Low) · 1 rejected · 1 merged

**Types:** Bug (wrong behaviour or data) · UX · A11y (accessibility) · i18n (translation/locale) · Product (change or remove a feature) · Enhancement (missing feature) · Tech debt · Pre-merge (uncommitted work)

**Verdicts:** ✅ Confirmed · ✏️ Corrected (claim fixed in the file) · ↕ Severity changed · ⛔ Blocked/moot (depends on another item) · ❌ Rejected · 🆕 Found in round 2 (70–78)

## Suggested order
1. **Data and core reliability:** 61, 70, 72, 79, 26, 02, 19, 17, 03, 32
2. **Distribution:** 63 (install size), 35 (lint gate)
3. **Before any Stats redesign:** 39, 40, 42, then the umbrella 50
4. Everything else by severity

## High (12)

| # | Type | Issue | Verdict |
|---|---|---|---|
| [02](02-catch-up-marks-future-prayers.md) | Bug | Onboarding Catch Up marks today's upcoming prayers as Qaza | ✅ |
| [03](03-notifications-not-localized.md) | i18n | Prayer notifications are always in English (other notifications are localized) | ✅ |
| [17](17-notification-denial-silent.md) | UX | Denying notifications is silent; reminders look on but never arrive | ✅ |
| [19](19-onboarding-stuck-after-allow-location.md) | UX | Onboarding gets stuck after "Allow location" | ✅ |
| [26](26-reminders-skip-still-enables.md) | Bug | "Skip for now" on Reminders still turns reminders on | ✅ |
| [32](32-migrations-untested-schema-missing.md) | Bug | DB migrations untested; v11 schema not committed; destructive fallback on | ✏️ wording |
| [50](50-stats-redesign-question-led.md) | Product | Stats doesn't answer users' questions — question-led redesign (umbrella) | ✅ |
| [61](61-qaza-auto-mark-too-early.md) | Bug | Qaza auto-mark flags prayers as missed while their time is still valid | ✅ |
| [63](63-mushaf-fonts-bloat-install-size.md) | Product | Mushaf fonts make the app 231 MB | ✅ |
| [70](70-notification-actions-wrong-date.md) | Bug | Marking from a notification after midnight records the wrong day | 🆕 |
| [72](72-calc-method-not-suggested-by-location.md) | Bug | Calculation method defaults to MWL everywhere; no regional suggestion | 🆕 |
| [79](79-daily-verse-not-from-shipped-quran.md) | Bug | Daily ayah isn't from the shipped Quran: made-up fallback line, no Arabic, not daily, always English | 🆕 (absorbs 04) |

## Medium (40)

| # | Type | Issue | Verdict |
|---|---|---|---|
| [01](01-24-hour-setting-ignored.md) | Bug | 24-hour time setting is saved but never used | ↕ High→Medium |
| [05](05-backup-skips-reminder-settings.md) | Bug | Backup/restore drops some reminder settings | ↕ High→Medium |
| [06](06-status-icons-colors-differ.md) | UX | Same prayer status uses different icons/colours per screen | ✅ |
| [08](08-legends-ignore-gender.md) | Bug | Stats/Calendar legends ignore the gender rule | ✅ |
| [10](10-onboarding-date-formatting.md) | UX | Onboarding formats dates differently from the rest of the app | ✅ |
| [11](11-stale-location-label.md) | Bug | Saved city name can go stale after the location changes | ✅ |
| [12](12-raw-error-messages.md) | UX | Raw exception text shown to users | ✅ |
| [14](14-hardcoded-colors-dark-mode.md) | A11y | White text on light-mint gradients in dark mode (Home hero, verse banner, alarm) | ✏️ ↕ Low→Medium |
| [16](16-exact-alarm-permission-never-requested.md) | UX | Exact alarms: Android 12 revocation unhandled; `USE_EXACT_ALARM` Play declaration | ✏️ ↕ High→Medium |
| [18](18-full-screen-alert-can-be-no-op.md) | UX | Full-screen alert toggle can be a no-op on Android 14+ | ✅ |
| [20](20-catch-up-no-confirm-or-undo.md) | UX | Catch Up writes hundreds of Qaza entries with no confirm or undo | ↕ High→Medium |
| [22](22-coordinates-no-range-validation.md) | UX | Coordinates accept out-of-range values with no inline error | ✅ |
| [23](23-settings-location-denial-silent.md) | UX | Denying location in Settings leaves the switch on, no explanation | ✅ |
| [24](24-about-you-privacy-copy-inaccurate.md) | UX | "About you" privacy copy is untrue (gender is in Drive backup) | ✅ |
| [25](25-onboarding-navigation-progress.md) | UX | Onboarding has no visible Back; progress dots are wrong | ✅ |
| [31](31-alarms-not-rescheduled-on-system-events.md) | Bug | Alarms follow a location change only when the app opens; daily chain can lapse | ✏️ ↕ High→Medium |
| [33](33-onboarding-lost-on-process-death.md) | Bug | Onboarding progress lost if the app is killed mid-setup | ✅ |
| [34](34-quran-tab-blank-during-first-seed.md) | UX | Quran tab blank while first-launch seeding runs | ✅ (needs device timing) |
| [35](35-lint-fails-plural-grammar.md) | i18n | Lint red (7 errors) incl. Arabic plurals; "1 prayers"/"1 verses" | ✅ |
| [36](36-test-coverage-gaps.md) | Tech debt | No tests for Home, onboarding, streaks, alarms, workers, migrations | ✅ |
| [39](39-stats-excused-days-count-as-failed.md) | Bug | Fully-excused days count as failed Perfect days | ↕ High→Medium |
| [40](40-stats-ignore-unrecorded-past-prayers.md) | Bug | Unrecorded past prayers are invisible to Stats, inflating completion % | ↕ High→Medium |
| [41](41-stats-trend-chart-axis.md) | Bug | Trend x-axis hides gaps, duplicates "Oct" on 1Y, crowded labels | ✅ (crowding needs device) |
| [42](42-stats-prayed-on-time-label.md) | UX | "Prayed on Time" headline doesn't measure timeliness | ✅ |
| [51](51-stats-qaza-made-up-tile-misleading.md) | Product | "Qaza made up" tile only counts prayers missed inside the period | ✅ |
| [52](52-stats-daily-trend-is-noise.md) | Product | Daily trend line is noise | ✅ |
| [56](56-stats-missing-congregation-habit.md) | Enhancement | No congregation metric or real 40-day challenge | ✅ |
| [57](57-stats-missing-data-completeness.md) | Enhancement | No "based on N of M prayers logged" | ✅ |
| [58](58-stats-missing-qaza-backlog-trend.md) | Enhancement | No Qaza backlog trend over time | ✅ |
| [62](62-quran-bookmarks-history-unreachable.md) | Product | Quran Bookmarks & Reading History unreachable; bookmarks can't be created | ✅ |
| [64](64-unused-bundled-quran-data.md) | Tech debt | ~14 MB of bundled Quran data no screen uses; tafsir loaded for nothing | ✅ |
| [67](67-best-streak-never-decreases.md) | Bug | "Best streak" can never go down; Bulk Update defaults to Congregation | ✅ |
| [68](68-streak-recalc-cost.md) | Tech debt | Every prayer save runs one query per day of history to recalc streaks | ✅ (unmeasured) |
| [69](69-tasbih-pre-merge-issues.md) | Pre-merge | Tasbih: no day rollover, duplicated due rule, brittle unit check, uncapped credits | ✏️ point 5 removed |
| [73](73-no-hijri-date-adjustment.md) | Bug | No Hijri date adjustment; dates and Islamic events can be a day off | 🆕 |
| [74](74-jafari-double-alarms.md) | Bug | Ja'fari users get two alarms at once (Dhuhr+Asr, Maghrib+Isha) | 🆕 |
| [75](75-home-day-boundary-midnight.md) | UX | Home rolls over at midnight while Isha is valid until Fajr | 🆕 |
| [76](76-goal-reminders-ignore-progress.md) | Bug | Goal reminders nag after the goal is done; notification Check in logs 1 unit | 🆕 |
| [77](77-mushaf-navigation-and-resume.md) | UX | Mushaf: no swipe, spinner per page, stuck at Juz end, position not always saved | 🆕 |
| [80](80-rename-app-istiqamah.md) | Product | Rename the app to "Istiqamah: The Salah Tracker" (48 strings, OAuth consent, Play listing; keep IDs) | 🆕 task |

## Low (26)

| # | Type | Issue | Verdict |
|---|---|---|---|
| [07](07-two-sets-of-status-names.md) | UX | Two different sets of status names | ↕ Medium→Low |
| [13](13-switch-and-button-styles.md) | UX | Switches and primary buttons styled inconsistently | ✅ |
| [15](15-duplicated-code.md) | Tech debt | Duplicated Azan preview, city search and helper components | ✅ |
| [21](21-quran-destructive-actions-no-undo.md) | UX | Bookmark delete / clear history are instant with no undo | ↕ Medium→Low · ⛔ blocked by 62 |
| [27](27-settings-reminders-flash-defaults.md) | UX | Settings / Prayer Reminders flash default values before loading | ✅ |
| [28](28-small-unlabeled-tap-targets.md) | A11y | Unlabeled icon buttons in the minute stepper | ✏️ ↕ Medium→Low |
| [29](29-text-below-12sp.md) | A11y | ~50 text styles below 12 sp | ✅ |
| [30](30-onboarding-and-unit-copy.md) | UX | Onboarding copy mismatches; hardcoded "m"/"AH" units | ✅ |
| [37](37-tests-depend-on-wall-clock.md) | Tech debt | Tests depend on the real clock | ✅ |
| [38](38-status-sheet-edge-cases.md) | Bug | Status sheet saves with nothing chosen; gender-hidden current status | ✅ |
| [43](43-stats-nothing-missed-when-empty.md) | Bug | "Nothing missed" shown when nothing was recorded | ✅ |
| [44](44-stats-zero-delta-shown-red.md) | UX | No change vs last period shown in red; noisy deltas | ✅ |
| [45](45-stats-goal-names-truncated.md) | UX | Goal names cut at 13 chars on Stats | ✅ · ⛔ moot if 54 |
| [46](46-stats-by-prayer-empty-state-dead.md) | Bug | By prayer never shows its empty state | ✅ |
| [47](47-stats-weakest-callouts-noisy.md) | UX | Weakest prayer/day callouts fire on trivial differences | ✅ |
| [48](48-stats-weekday-a11y-i18n.md) | A11y | Weekday chart: no values/TalkBack, Sunday-first, mixed digit styles | ✅ (point 4 = 08) |
| [49](49-stats-refresh-and-double-load.md) | Tech debt | Stats doesn't roll over at midnight; loads everything twice | ✅ |
| [53](53-stats-weekday-card-low-value.md) | Product | By weekday card is sampling noise — remove | ✅ |
| [54](54-stats-goals-card-duplicates-goals-tab.md) | Product | Goals card duplicates the Goals tab — remove | ✅ |
| [55](55-stats-period-options.md) | Product | 40D implies a feature that isn't there; 1Y weak | ✅ |
| [59](59-stats-missing-plain-language-insights.md) | Enhancement | No plain-language takeaway on Stats | ✅ |
| [60](60-stats-raw-counts-and-overlap.md) | Product | Raw status counts; Perfect days overlaps Home | ✅ |
| [65](65-drawer-clutter-navigation.md) | Product | Drawer mixes core tools with meta pages | ✅ |
| [66](66-verse-cards-repeated.md) | Product | Verse card on Calendar adds little | ✏️ wording |
| [71](71-notification-not-cleared-after-logging.md) | Bug | Prayer notification stays after the prayer is logged in the app | 🆕 |
| [78](78-goals-deep-link-stacks-tab.md) | Bug | Goal notification stacks a second Goals screen instead of switching tab | 🆕 |

## Merged (1)

| # | Issue | Merged into |
|---|---|---|
| [04](04-home-verse-always-english.md) | Home verse is always the English translation | [79](79-daily-verse-not-from-shipped-quran.md) |

## Rejected (1)

| # | Issue | Why |
|---|---|---|
| [09](09-dates-mixed-locale.md) | Dates use app language on some screens, phone language on others | AppCompat per-app locale makes `Locale.getDefault()` follow the app language, so both formatter styles match. Code-style note only. |

## How this list was built
Six code reviews on 2026-10-23: consistency audit (01–15), UX review (16–30), QA with automated checks (31–38; 319 unit tests passing, lint 7 errors), Stats review (39–49), Stats product review (50–60), feature-value review (61–66), and a re-audit (67–69). Then a one-by-one re-evaluation of all items against the code, which corrected 9 items and rejected 1. A second bug-finding round (70–78) covered notification actions, prayer-time defaults, Hijri dates, Ja'fari reminders, the day boundary, goal reminders, Mushaf navigation and deep links.

**Checked and fine:** no reachable crash paths from `!!` or `first()`; all manifest receivers/services are `exported="false"`; arrow and chevron icons auto-mirror for RTL; string keys are in parity across all four locales; Qaza progress/ETA and quick-log; Home next-prayer and countdown; calendar grid and markability; goal schedule and stats; backup encode/decode and restore guards; Mosque Mode ringer handling; Goal/Backup/Mosque notifications are localized; deep-link routes are whitelisted; backup restore writes all Room data in one transaction and re-applies language, reminders and streaks.

**Not reviewed:** on-device visuals (layout, contrast, truncation). This was a code-based pass. Items marked "needs device" should be confirmed on a phone.
