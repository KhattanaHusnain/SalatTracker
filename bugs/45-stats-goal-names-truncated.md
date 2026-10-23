# 45 — Stats: goal names cut at 13 characters even when there's room

**Severity:** Low · **Area:** Stats / layout & i18n · **Moot if [54](54-stats-goals-card-duplicates-goals-tab.md) is done** (removing the Goals card from Stats removes this code).

## Problems
1. **Forced truncation.** Goal names go through a private `String.shortLabel()` that cuts anything over 14 characters to 13 + "…", *and* the `Text` already has `maxLines = 1` + ellipsis. "Tahajjud Prayer" becomes "Tahajjud Pray…" on a wide screen. Character counts also don't match visual width for Arabic/Urdu.
2. **Grammar.** The subtitle is `"${loggedAmount} ${unit}"`, giving "1 Pages" / "1 Time(s)" (see bug 35 for plurals).

## Where
- `app/src/main/java/com/salattracker/feature/stats/StatsScreen.kt:415` (`item.goalName.shortLabel()`), `:417`, `:440` (`shortLabel` definition)

## Suggested fix
Remove the custom `shortLabel()` and rely on `maxLines = 1` + `TextOverflow.Ellipsis`. Use a plural resource for amount + unit.

## Done when
Goal names use the full available width and counts read correctly in every language.
