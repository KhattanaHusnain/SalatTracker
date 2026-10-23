# 50 — Stats screen doesn't answer the questions users actually have (redesign)

**Severity:** High (product) · **Area:** Stats / product · **Type:** Umbrella

## Problem
Stats was assembled from generic dashboard patterns rather than from the questions a person tracking their prayers asks. Of its sections, only **By prayer** is clearly useful. The headline % is the right idea but untrustworthy. The rest repeats other tabs (Goals, Qaza, Home streaks) or shows noise (weekday chart, daily trend line). Meanwhile the metrics this audience cares about (congregation habit, Qaza recovery, data completeness, readable improvement) aren't there.

| Current section | Verdict | Bug |
|---|---|---|
| Headline % + vs last period | Keep, but fix label and trust | 42, 40, 44, 57 |
| Status bar + legend counts | Shrink | 60 |
| Perfect days | Keep as small tile; fix calculation | 39, 60 |
| Qaza made up (x/y) | Replace | 51 |
| Trend line (daily) | Rework to weekly | 52, 41 |
| By prayer | **Keep**; add threshold | 47 |
| By weekday | Remove | 53 |
| Goals card | Remove | 54 |
| 7D / 30D / 40D / 90D / 1Y | Simplify | 55 |

## Target structure
Five cards, each answering one question:
1. **How am I doing?** Completion % for the period vs previous, with data completeness ("based on 132 of 150 prayers logged"). → 42, 57
2. **Where do I struggle?** By prayer, with a meaningful-difference callout. → 47
3. **Congregation** (hidden for Female). Congregation share overall and per prayer, plus a 40-day challenge. → 56
4. **Qaza recovery.** Outstanding backlog over time, linking to the Qaza tab. → 58
5. **Am I improving?** Weekly trend shown only with ≥ 4 weeks of data, plus one plain-language insight. → 52, 59

## Prerequisites
Fix the calculation bugs first so the redesigned numbers are trustworthy: 39, 40, 42.

## Done when
Every card on Stats answers a question a user would ask, nothing duplicates another tab, and every number is explained and correct.
