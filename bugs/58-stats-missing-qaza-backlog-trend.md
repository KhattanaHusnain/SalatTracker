# 58 — Stats: no view of Qaza recovery over time

**Severity:** Medium · **Area:** Stats / product · **Type:** Missing feature · Part of [50](50-stats-redesign-question-led.md)

## Problem
For users with a Qaza backlog, the most meaningful long-term question is "is the amount I owe going down?". The Qaza tab shows today's outstanding total, pace and ETA, but nowhere shows the backlog's history. Stats' only Qaza element is the misleading tile in bug 51.

## Proposal
A **Qaza recovery** card (hidden when the user has never had any Qaza):
- Outstanding backlog at the start vs end of the period ("412 → 365, −47").
- Prayers made up during the period (by made-up date, not original date).
- Small weekly line of outstanding total.
- Link to the Qaza tab.

Outstanding-at-date can be derived from entries: prayers dated on or before D that were Qaza pending at D. If made-up timestamps aren't stored, use `updatedAt` on `QAZA_COMPLETED` entries (already used by `observeQazaMadeUpSince`).

## Where it would go
- `domain/usecase/QazaProgress.kt` or a new use case
- `data/local/dao/PrayerDao.kt` (query by `updated_at`)
- `feature/stats/*`

## Done when
A user can see whether their Qaza backlog grew or shrank over the selected period.
