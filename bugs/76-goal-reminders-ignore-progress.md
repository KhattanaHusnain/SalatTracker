# 76 — Goal reminders nag after the goal is done; "Check in" from the notification always logs 1

**Severity:** Medium · **Area:** Bug / Goals reminders

## Problems
1. **Reminders ignore progress.** `nextReminderAt` only checks whether the goal is *due* that day. A daily goal already completed at 06:00 still reminds at 08:00, and weekly/monthly goals ("due" every day of their window) remind **every day**, even after the week's or month's target is met.
2. **"Check in" logs one unit.** The notification's Check in action calls `checkIn(goalId)` with the default `amount = 1`. For "Read 10 pages" that logs 1 page; the user likely thinks they've marked the goal done.

## Where
- `app/src/main/java/com/salattracker/domain/usecase/GoalSchedule.kt:122-131` (`nextReminderAt`)
- `app/src/main/java/com/salattracker/notification/GoalReminderReceiver.kt:46, 59-81`
- `app/src/main/java/com/salattracker/domain/repository/GoalRepository.kt:30` (`amount: Int = 1` default)

## Suggested fix
- In the receiver (or scheduler), skip the notification when the current period's target is already met; schedule the next one for the next period.
- Make the action match the goal: "Mark done" (logs the remaining amount to reach today's target) for single-step goals; for multi-unit goals open the app's day editor instead of logging 1.
- Tests for both rules.

## Done when
Goal reminders only appear when there's still something to do, and their action does what it says.
