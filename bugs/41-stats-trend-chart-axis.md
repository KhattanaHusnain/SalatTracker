# 41 — Stats: the trend chart's x-axis misrepresents time

**Severity:** Medium · **Area:** Stats / chart

## Problems
1. **Gaps are hidden.** Buckets with nothing recorded are dropped, and the remaining points are plotted at evenly-spaced indices (`points.indices`). Ten days of no data between two points look like consecutive days, and the line connects straight across them.
2. **Duplicate month labels on 1Y.** Monthly buckets are calendar months and labelled `MMM`. A 365-day range starting 24 Oct 2025 contains both Oct 2025 (8 days) and Oct 2026 (23 days), so the axis shows **"Oct" twice**, and the two partial months are plotted as if they were full months.
3. **Out-of-range label on 90D.** Weekly buckets are counted back from today, so the oldest bucket's start date (its label) can fall before the selected 90-day range.
4. **Label crowding on 30D/40D (needs device check).** Daily buckets give 30–40 labels (`d/M`) across a ~300 dp chart. Vico draws every label by default, so they likely overlap.

## Where
- `app/src/main/java/com/salattracker/feature/stats/StatsCharts.kt:43-73`
- `app/src/main/java/com/salattracker/domain/usecase/StatsSummary.kt:88-101` (`completionTrend`)
- `app/src/main/java/com/salattracker/feature/stats/StatsViewModel.kt:133-134` (drops null buckets)

## Suggested fix
- Plot x by actual bucket index from the range start (keep empty buckets as gaps, or draw them as a break in the line) instead of by list position.
- For 1Y, use 12 rolling 30-day buckets or label as `MMM yy`; or limit the range to whole months.
- Clamp the first weekly bucket's label to the range start.
- Set a horizontal item placer that shows every Nth label (e.g. ~6 labels max).

## Done when
The x-axis spacing matches real time, labels are unique and readable, and no label lies outside the selected range.
