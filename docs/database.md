# LabTrack — Database

SQLite database managed by `labtrack.database.DatabaseManager` (Singleton).

## File location

| Mode | Path |
|---|---|
| Default | `~/.labtrack/labtrack.db` |
| Override | `-Dlabtrack.db.dir=<dir>` → `<dir>/labtrack.db` |
| Tests | temp directory per test run (`TestDb` sets the system property before the singleton initializes) |

The parent directory is created automatically on first open.

## Lifecycle rules

- **Created once**: `initSchema` runs `CREATE TABLE IF NOT EXISTS ...` on every launch — an existing database is opened as-is, never dropped or recreated.
- **Seeded once**: `LegacyDataSeeder.seedIfEmpty(...)` imports `src/main/resources/legacy_seed.tsv` (16 legacy items) *only if* the equipment table is empty. Subsequent launches skip seeding entirely.
- **Connection style**: short-lived connections per operation (`DriverManager.getConnection(jdbcUrl)`), `PRAGMA foreign_keys = ON` applied at open.
- **Single process**: run only one app instance against a database at a time (concurrent writers can raise `database is locked`).

## Schema

### `categories`

```sql
id    INTEGER PRIMARY KEY AUTOINCREMENT
name  TEXT NOT NULL COLLATE NOCASE UNIQUE
```

User-extensible categories (+ Category button). Case-insensitive uniqueness: "Oscilloscope" and "oscilloscope" are the same category.

### `equipment`

```sql
id            INTEGER PRIMARY KEY AUTOINCREMENT
code          TEXT NOT NULL UNIQUE               -- asset tag, e.g. AST-0042
name          TEXT NOT NULL
type          TEXT NOT NULL DEFAULT 'ELECTRONIC' -- ELECTRONIC | COMPUTER → class hierarchy
category_id   INTEGER NOT NULL REFERENCES categories(id)
serial_number TEXT UNIQUE                        -- optional, unique when present
manufacturer  TEXT
location      TEXT
purchase_date TEXT                               -- ISO yyyy-MM-dd
condition     TEXT NOT NULL DEFAULT 'GOOD'       -- EXCELLENT|GOOD|FAIR|POOR|DAMAGED
status        TEXT NOT NULL DEFAULT 'AVAILABLE'  -- AVAILABLE|ISSUED|DAMAGED|MAINTENANCE|ARCHIVED
notes         TEXT
created_at    TEXT NOT NULL DEFAULT (datetime('now','localtime'))
```

- `code` UNIQUE → duplicate asset tags rejected (`DuplicateRecordException`).
- `serial_number` UNIQUE → duplicates rejected, `NULL` allowed (SQLite allows multiple NULLs in a UNIQUE column).
- `type` drives the DAO when rebuilding the polymorphic hierarchy (`ElectronicEquipment` vs `ComputerEquipment`).
- `status` is the lifecycle lock: `DAMAGED`/`MAINTENANCE` block issuing; `ARCHIVED` hides the row from normal lists.

### `users`

```sql
id            INTEGER PRIMARY KEY AUTOINCREMENT
username      TEXT NOT NULL COLLATE NOCASE UNIQUE
password_hash TEXT NOT NULL          -- hex PBKDF2-WithHmacSHA256 (256-bit), 120k iterations
salt          TEXT NOT NULL          -- hex 16-byte random salt, per user
role          TEXT NOT NULL DEFAULT 'USER'   -- USER | ADMIN
```

Login accounts. The `COLLATE NOCASE UNIQUE` username means "Admin" and "admin" are the same account. Passwords are stored **only** as salted PBKDF2 hashes — plaintext never touches the database. Created and used exclusively through `UserDao`/`AuthService`; `seedDefaultUser()` populates the sample `admin` account when the table is empty.

### `borrowers`

```sql
id     INTEGER PRIMARY KEY AUTOINCREMENT
name   TEXT NOT NULL
code   TEXT NOT NULL UNIQUE      -- borrower/registration ID, used for find-or-create
role   TEXT NOT NULL DEFAULT 'STUDENT'
email  TEXT
phone  TEXT
```

Borrowers are created on first issue (`findOrCreate` inside the issue transaction) and reused when the code matches.

### `issue_records`

```sql
equipment_id    INTEGER NOT NULL REFERENCES equipment(id) ON DELETE CASCADE
borrower_id     INTEGER NOT NULL REFERENCES borrowers(id)
issued_date     TEXT NOT NULL
due_date        TEXT NOT NULL
returned_date   TEXT
return_condition TEXT
remarks         TEXT
status          TEXT NOT NULL DEFAULT 'ACTIVE'   -- ACTIVE | RETURNED
```

- One row per loan; a second `ACTIVE` row for the same equipment is blocked in the service layer (`countActiveForEquipment`).
- Overdue is **computed**, not stored: `findOverdue()` selects active rows with `due_date < date('now','localtime')`.
- `ON DELETE CASCADE` — but equipment deletion is only allowed with no history at all, so cascades never fire in practice.

### `maintenance_records`

```sql
equipment_id     INTEGER NOT NULL REFERENCES equipment(id) ON DELETE CASCADE
type             TEXT NOT NULL          -- DAMAGE | MAINTENANCE
description      TEXT NOT NULL
resolution_notes TEXT
status           TEXT NOT NULL DEFAULT 'PENDING'   -- PENDING | IN_PROGRESS | COMPLETED
reported_date    TEXT NOT NULL
resolved_date    TEXT
reported_by      TEXT
```

Open records (`PENDING`/`IN_PROGRESS`) lock their equipment until `complete(...)` writes the resolution, sets `COMPLETED`, and unlocks the item in the same transaction.

### Indexes

```sql
idx_issue_equipment  ON issue_records(equipment_id)
idx_maint_equipment  ON maintenance_records(equipment_id)
```

## Transactions

All multi-statement writes go through `DatabaseManager.inTransaction(work)`:

```java
try (Connection c = getConnection()) {
    c.setAutoCommit(false);
    try { T result = work.run(c); c.commit(); return result; }
    catch (Exception e) { c.rollback(); throw ...; }
}
```

| Operation | Atomic unit |
|---|---|
| `IssueService.issue` | validate → load equipment → duplicate-loan check → find/create borrower → insert loan → set status `ISSUED` |
| `IssueService.returnItem` | validate → load loan → mark returned → update condition → *(if damaged: insert damage record + set status `DAMAGED`, else set status `AVAILABLE`)* |
| `MaintenanceService.reportDamage/requestMaintenance` | insert record → set equipment status locked |
| `MaintenanceService.complete` | mark record completed → update condition → unlock equipment |
| `EquipmentService.delete` | history check → delete row |
| Seeding | one item at a time (category lookup/create + insert) |

On any failure the whole unit rolls back — a failed issue never leaves an `ISSUED` status without its loan record, and a damaged return never leaves a return without its damage report.

## Error translation

| Source | Becomes |
|---|---|
| SQLite UNIQUE violation | `DuplicateRecordException` (via `DatabaseManager.isUniqueViolation`) |
| Business-rule refusal in service | `ValidationException` / `OperationException` |
| Wrapped causes in `SQLException` | unwrapped by `IssueService.translate(...)` back to `LabTrackException` |

## Reporting queries

`ReportDao` (used by `ReportService` and `DashboardPanel`) runs aggregate SQL over the same tables: dashboard counts by status + overdue count, recent activity (last 9 events), category/condition summaries, status summary, and the 10 report views listed in the user guide. Reports read **live** data — every selection re-queries the database.
