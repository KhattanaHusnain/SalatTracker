# 32 — Database migrations are untested, and the shipped v11 schema isn't committed

**Severity:** High · **Area:** QA / data safety

## Problem
`SalatDatabase` holds users' entire prayer history and is at version 11 on `master` (12 in the current uncommitted Tasbih work). The code comment notes there are real Play Store installs on v7, so every upgrade path from v7 must work.

- There are **no migration tests**. `androidTest/` contains only the template `ExampleInstrumentedTest`. Nothing exercises `MIGRATION_5_6 … MIGRATION_11_12`.
- The exported schema for **v11 is not in git** (`app/schemas/.../SalatDatabase/11.json` is untracked; 6–10 are committed). Room's `MigrationTestHelper` needs the schema JSON for each version to test a migration, so 10→11 and 11→12 can't currently be tested from a clean checkout.
- `.fallbackToDestructiveMigration()` is enabled. If a migration path is **missing** (e.g. a future version bump without its `Migration`, or a downgrade to an older build), Room silently drops all tables and users lose everything with no error report. *(Corrected in re-audit: a migration that **throws** crashes the app rather than wiping data — still bad, but visible.)*

## Where
- `app/src/main/java/com/salattracker/di/DatabaseModule.kt:33-38`
- `app/src/main/java/com/salattracker/data/local/db/Migrations.kt`
- `app/schemas/com.salattracker.data.local.db.SalatDatabase/` (11.json, 12.json untracked)
- `app/src/androidTest/` (no migration tests)

## Suggested fix
- Commit `11.json` (and `12.json` together with the Tasbih work).
- Add an instrumented `MigrationTest` using `MigrationTestHelper` that creates v7 with sample prayer entries/goals, runs all migrations to latest, and asserts the data survived. Run it in CI on an emulator.
- Consider limiting destructive fallback to versions below 5 explicitly (`fallbackToDestructiveMigrationFrom(1, 2, 3, 4)`) so a broken future migration fails loudly instead of wiping data.

## Done when
Every migration from v7 to the latest is covered by a passing test, all schema files are committed, and destructive fallback can't trigger for v5+.
