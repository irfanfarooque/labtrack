# LabTrack — Laboratory Equipment Management System

A desktop application for tracking laboratory equipment, lending (issue/return), damage and maintenance, with live statistics and database-driven reports. Built with Java Swing and SQLite — single executable jar, fully offline. Secured by a login screen with salted PBKDF2 password hashing.

## Features

- **Login gate** — sign in before the app opens; salted PBKDF2 password hashing, first-run sample admin account, logout from the sidebar.
- **Dashboard** — 6 live statistics (total, available, issued, damaged, maintenance, overdue) and a recent-activity feed.
- **Equipment inventory** — add, edit, search, filter (category / condition / status / archived), unique asset-tag and serial-number validation, archive/restore, history-safe delete, custom categories.
- **Issue & Return** — lend items to registered borrowers, record returns, automatic overdue tracking, duplicate-issue and invalid-state blocking, all write operations run in database transactions.
- **Damage & Maintenance** — report damage or request maintenance (locks the item until resolved), start work, complete records with resolution notes and condition update.
- **Reports** — 10 report views over live SQL queries (inventory, summaries, loans, overdue, damage, maintenance, borrowing history).

## Requirements

- Java 17 or newer (`java -version`)
- Maven 3.9+ for building (no system install needed to *run* — the jar bundles everything)

## Build

```bash
mvn clean package
```

This compiles the app, runs the full test suite, and produces the executable jar:

```
target/labtrack-1.0.0.jar
```

> **Note for this machine:** Maven is not installed system-wide here; a local copy lives at `/tmp/opencode/tools/apache-maven-3.9.9`. Use:
> ```bash
> export PATH=/tmp/opencode/tools/apache-maven-3.9.9/bin:$PATH
> mvn clean package
> ```

## Run

```bash
java -jar target/labtrack-1.0.0.jar
```

The app opens on a **login screen**. First run seeds a sample account:

| Username | Password | Role |
|---|---|---|
| `admin` | `lab123` | ADMIN |

The password is stored only as a salted PBKDF2 hash (never plaintext). To add accounts or change the admin password, use the `AuthService.createUser(...)` service method or re-seed after clearing the `users` table.

No internet connection is required at runtime — the SQLite driver and the FlatLaf look-and-feel are bundled inside the jar.

## Data storage

| What | Where |
|---|---|
| Database file | `~/.labtrack/labtrack.db` |
| Custom location | `java -Dlabtrack.db.dir=/path/to/dir -jar target/labtrack-1.0.0.jar` |
| Reset all data | Close the app, then `rm -rf ~/.labtrack` (recreated and re-seeded on next start) |

The database is **never recreated** while it exists — it is created and seeded only on the first run (16 sample items imported from a legacy TSV export). All data persists across restarts.

## Tests

```bash
mvn test
```

7 tests: 5 business-rule integration tests, 1 UI smoke test (skips when headless), and 1 UI regression test guarding table stability and row selection. See [docs/testing.md](docs/testing.md).

## Documentation

| Document | Contents |
|---|---|
| [docs/user-guide.md](docs/user-guide.md) | Screen-by-screen usage and business rules |
| [docs/architecture.md](docs/architecture.md) | Layered design, OOP principles, patterns, threading |
| [docs/database.md](docs/database.md) | Schema, constraints, transactions, persistence |
| [docs/testing.md](docs/testing.md) | Test suite structure and how to run/extend it |
| [docs/development.md](docs/development.md) | Project layout, build config, how to add features |

## Project layout

```
src/main/java/labtrack/
├── LabTrackApp.java     # entry point
├── model/               # entities, enums, interfaces (Equipment hierarchy)
├── dao/                 # SQL data access (prepared statements)
├── service/             # business rules, transactions, reports
├── database/            # DatabaseManager singleton + schema
├── ui/                  # MainFrame, panels/, dialogs/
├── adapter/             # legacy TSV import (Adapter pattern) + first-run seeder
├── exception/           # checked exception hierarchy
└── util/                # Swing tasks, dates, UI helpers
src/main/resources/
└── legacy_seed.tsv      # first-run seed data (16 items)
src/test/java/labtrack/  # JUnit 5 tests
```

~2,700 lines of application code across 53 files, ~370 lines of tests.
