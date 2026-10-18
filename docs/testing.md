# LabTrack — Testing

The suite runs with the normal build — `mvn test` or `mvn clean package` — and must show **8 passing tests** before a build is considered good.

## Test files

| File | Lines | Purpose |
|---|---|---|
| `LabTrackIntegrationTest.java` | ~195 | 5 end-to-end business-rule tests against a real temporary SQLite database |
| `AuthServiceTest.java` | ~30 | Seeding, login, wrong credentials, duplicates, validation |
| `UiSmokeTest.java` | ~70 | Launches the real window, the login dialog and every dialog on a display; skips when headless |
| `InventorySelectionTest.java` | ~50 | Regression test for the table-stability/row-selection bug |
| `TestDb.java` | 17 | Test database bootstrap (shared by all test classes) |

## Test database isolation

`TestDb.DIR` creates a fresh temp directory and sets the system property **`labtrack.db.dir`** when the class loads:

```java
static final Path DIR = createFreshDirectory();   // sets labtrack.db.dir
```

Rules:

- **`TestDb.DIR` must be touched before `DatabaseManager` initializes** — the singleton reads the property once, in its constructor. The integration test does this in `@BeforeAll`; the UI tests do it at the start of the test method.
- All test classes share one temp database per run (the constant is class-level).
- The user's real `~/.labtrack/labtrack.db` is never touched by tests.

## Integration tests (`LabTrackIntegrationTest`)

Ordered (`@TestMethodOrder(OrderAnnotation.class)`) because they build on shared state; each seeds its own categories/items.

1. **`equipmentCrudValidationAndDuplicates`** — create/read/update; duplicate asset tag and duplicate serial rejected; blank code/name and missing category rejected; search by tag/serial/name; duplicate and blank category names rejected.
2. **`archiveRestoreAndSafeDelete`** — archive hides the item, restore brings it back as Available; delete works only with no history, otherwise `OperationException`; deleted items can't be loaded.
3. **`issueReturnOverdueAndBorrowRules`** — issue sets status `Issued` and blocks a second active loan; return sets `Returned`/`Available` and blocks double return; due-before-issue date rejected; two loans by the same borrower share one borrower ID; overdue computed with correct day count; Damaged and POOR items can't be issued.
4. **`damageAndMaintenanceLifecycle`** — damaged return auto-opens a damage record and locks the item; duplicate open records blocked; complete unlocks with the new condition (POOR → stays Damaged); blank resolution rejected; full maintenance cycle on a `ComputerEquipment` (created `computerType=true` and asserted as the correct subclass); damage blocked while the item is issued.
5. **`dashboardReportsAdapterAndPersistence`** — dashboard totals match raw table counts; all 10 report specs present; Adapter maps legacy records to the right subclasses and parses TSV (comments skipped); seeding is idempotent (running it twice doesn't duplicate rows); a new row is visible through a raw JDBC connection; a second `AppContext` sees it too (**persistence across contexts = restart simulation**).

## Authentication tests (`AuthServiceTest`)

- Seeding is idempotent (called twice, one admin); `admin`/`lab123` logs in with role ADMIN; username lookup is case-insensitive; wrong password, unknown user, and blank fields are rejected; duplicate and invalid usernames are rejected; a second account (`bob`) can be created and used to log in.

## UI tests

- **`UiSmokeTest.mainWindowAndDialogsComeUp`** — asserts:
  - the real `MainFrame` shows with the right title,
  - **all 5 sidebar navigation buttons exist** and Reports → Dashboard navigation works (regression for a bug where only "Reports" rendered),
  - every dialog constructs: EquipmentDialog ×3 (add/edit/view), IssueDialog ×2, ReturnDialog, MaintenanceDialog, CompleteMaintenanceDialog.
  - Skipped automatically when no display is available (`Assumptions.assumeFalse(GraphicsEnvironment.isHeadless())`).
- **`InventorySelectionTest.tableStaysCalmAndSelectionSurvives`** — regression for the infinite refresh loop:
  - after load, waits 1 second and asserts **0** table-model events while idle (the pre-fix bug produced ~92,000/sec),
  - then selects row 0, waits 700 ms, and asserts the selection is still there (pre-fix it was wiped to `-1`).

## Running the tests

```bash
mvn test                                  # all 8 tests
mvn test -Dtest=InventorySelectionTest    # a single class
mvn clean package                         # tests + jar in one go
```

With a display (even XWayland), the UI tests run; fully headless, they self-skip and only the 5 integration tests execute.

## Adding a test

- Service/business-rule behaviour → new ordered method in `LabTrackIntegrationTest` (or a new class; make sure `TestDb.DIR` is initialized first).
- Swing behaviour → follow `UiSmokeTest`: guard with the headless assumption, build UI inside `SwingUtilities.invokeAndWait`, dispose windows at the end.
- For private panel state, reflection on the field is acceptable (see `InventorySelectionTest`), but prefer asserting through public behaviour when possible.
