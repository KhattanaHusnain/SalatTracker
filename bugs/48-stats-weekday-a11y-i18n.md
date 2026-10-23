# 48 — Stats: weekday chart and numbers aren't accessible or localized

**Severity:** Low · **Area:** Stats / accessibility & i18n

## Problems
1. **Weekday bars have no values.** Only bar height conveys the %, and the bars have no semantics, so TalkBack reads seven day abbreviations and nothing else. The same applies to the stacked status bars in Summary and By prayer.
2. **Week always starts on Sunday.** `weekOrder` is hardcoded; many users of this app (Pakistan, Gulf, France) expect Monday or Saturday first.
3. **Numbers aren't locale-formatted.** `"$it%"`, `"$label: $count"`, `"${perfect}/${counted}"` and `"(it * 100).toInt()%"` are built by string concatenation, so Arabic gets Latin digits and "%" instead of "٪", and the order isn't adjusted for RTL.
4. **Legend shows gender-hidden statuses** — see bug 08.

## Where
- `app/src/main/java/com/salattracker/feature/stats/StatsScreen.kt:209, 188, 241, 323, 332-379`

## Suggested fix
- Add `Modifier.semantics { contentDescription = "Monday, 85%" }` to each weekday bar and a summary description to each status bar; optionally show the % above each bar.
- Derive week order from `WeekFields.of(locale).firstDayOfWeek`.
- Use `NumberFormat.getPercentInstance(locale)` / `stringResource` format strings for counts and ratios.

## Done when
TalkBack reads every value on the Stats screen, and numbers follow the app language's conventions.
