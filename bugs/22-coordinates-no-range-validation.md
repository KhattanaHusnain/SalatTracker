# 22 — Coordinates accept out-of-range values with no inline error

**Severity:** Medium · **Area:** UX / form validation

## Problem
Settings → Location → coordinates only checks that the text parses as a number. Latitude `267` or longitude `-500` are saved, prayer times are recalculated from garbage, and the user gets no message. The Save button also gives no hint why it's disabled when a field is blank or malformed.

The design mockup (screen 7b) specified inline validation: red field border plus "Enter a value between −180 and 180."

## Where
- `app/src/main/java/com/salattracker/feature/settings/EditCoordinatesViewModel.kt:70-71`
- `app/src/main/java/com/salattracker/feature/settings/EditCoordinatesScreen.kt:105` (Save `enabled` check), `CoordField` (no error state)

## Suggested fix
- Validate latitude ∈ [−90, 90] and longitude ∈ [−180, 180] in the ViewModel; expose `latitudeError` / `longitudeError` string resources.
- Use `OutlinedTextField(isError = …, supportingText = …)` to show the message under the field.
- Optionally accept a pasted "lat, lon" pair in either field.

## Done when
Out-of-range or non-numeric input shows an inline, localized error and can't be saved.
