# LabTrack — Architecture

How the application is structured, which object-oriented principles and patterns are used where, and how threading works.

## Layered design

```
┌────────────────────────────────────────────────────────┐
│ ui (MainFrame, ui.panels, ui.dialogs)                  │  Swing, EDT only
├────────────────────────────────────────────────────────┤
│ service (AppContext, EquipmentService, IssueService,   │  business rules,
│          MaintenanceService, ReportService, AuthService)│  transactions, auth
├────────────────────────────────────────────────────────┤
│ dao (EquipmentDao, BorrowerDao, IssueDao, UserDao,     │  prepared statements,
│      MaintenanceDao, ReportDao)                        │  result mapping
├────────────────────────────────────────────────────────┤
│ database (DatabaseManager)                             │  connections, schema,
│                                                        │  transaction scope
└────────────────────────────────────────────────────────┘
```

Side packages: `model` (entities used by every layer), `adapter` (legacy import), `exception` (checked exception hierarchy), `util` (Swing helpers, dates).

**Dependency rule:** UI → service → dao → database. UI never touches SQL; DAOs never touch Swing. `AppContext` performs manual dependency injection — it constructs the DAOs and services once and hands them to the UI (`LabTrackApp` → `new AppContext()` → `new MainFrame(ctx)`).

## OOP principles

### Encapsulation

Model classes (`Equipment`, `IssueRecord`, `MaintenanceRecord`, `Borrower`, `Category`) keep fields `private` with getters/setters. `DatabaseManager` hides its connection details behind `getInstance()` / `getConnection()` / `inTransaction()`. UI panels keep their tables, models and listeners `private`; dialogs expose only `open()` and `getResult()`.

### Abstraction

- Services expose intent-level operations (`issue(...)`, `returnItem(...)`, `archive(...)`) — callers never see SQL or transactions.
- `Borrowable` and `Maintainable` abstract *behaviour contracts* behind two small interfaces:

```java
interface Borrowable    { boolean isBorrowable();    String borrowNote(); }
interface Maintainable  { boolean isMaintenanceLocked(); String inspectionChecklist(); }
```

- `ReportService.ReportSpec` + `Rows` abstract "a report" as data (name, columns, loader), so the reports screen iterates a list instead of knowing any query.

### Inheritance

```
Equipment (abstract, implements Borrowable, Maintainable)
 └── ElectronicEquipment          # powered test gear
      └── ComputerEquipment        # stricter rules
```

Each level inherits fields/constructors and overrides behaviour. The DAO rebuilds the correct subclass from the stored `type` column.

### Runtime polymorphism

Calls resolve on the actual object type at runtime:

| Call site | ElectronicEquipment | ComputerEquipment |
|---|---|---|
| `isBorrowable()` | also rejects `POOR` condition (unsafe powered gear) | inherits the electronic rule |
| `borrowNote()` | "Return with all probes, cables and accessories." | "User data must be backed up before return." |
| `inspectionChecklist()` | power-on test, output verification | boot test, OS/antivirus check |
| `equipmentType()` | "Electronic Equipment" | "Computer Equipment" |

The UI and services use these through the `Equipment` reference only — `IssueService.issue()` calls `item.isBorrowable()` without ever checking the concrete class, and `MaintenanceDialog` builds checklists from `inspectionChecklist()`.

### Exception handling

```
LabTrackException (checked, base)
 ├── ValidationException        # bad input (dates, blank fields, bad state)
 ├── DuplicateRecordException   # UNIQUE constraint → friendly message
 └── OperationException         # business-rule refusal, SQL failures
```

- DAOs throw `SQLException`; services translate via `translate(...)` (unwrapping causes) into the hierarchy above.
- `DatabaseManager.isUniqueViolation(...)` converts SQLite UNIQUE violations into `DuplicateRecordException`.
- UI layers catch at one point: `Tasks` shows unexpected failures as an error dialog; dialogs catch and display validation errors inline.

## Design patterns

| Pattern | Where | Why |
|---|---|---|
| **Singleton** | `DatabaseManager` (double-checked locking on a `volatile` field) | one shared configuration point for the JDBC URL; safe from any thread |
| **Adapter** | `LegacyTsvAdapter` implements `EquipmentAdapter`, converts `LegacyAssetRecord` → `Equipment` | legacy tab-separated export feeds the modern model without polluting it; only runs when the DB is empty (`LegacyDataSeeder`) |
| **DAO** | every class in `dao/` | all SQL in one place, prepared statements only |
| **Repository-ish service** | `service/` | business rules + transaction boundaries above raw DAO calls |
| **Composite/strategy-lite** | `ReportService.reportSpecs()` — records holding a `Rows` lambda | adding a report = one `new ReportSpec(...)`, zero UI changes |
| **Template** | `FormDialog` base class owns the dialog frame, buttons, validation-to-error flow; subclasses build the form and `build()` the result | every dialog shares identical open/save/cancel behaviour |
| **Context/DI** | `AppContext` | one object wires the object graph; tests reuse it |
| **Gate/front-controller** | `LabTrackApp.startSession(...)` — login dialog must succeed before `MainFrame` is constructed | unauthenticated users never see data; logout disposes the frame and re-runs the gate |

## Authentication

`AuthService` (wired in `AppContext`) guards the application:

- **Hashing**: PBKDF2-WithHmacSHA256, 256-bit key, per-user 16-byte random salt, 120,000 iterations. Only the hex hash and salt are stored — never plaintext.
- **Verification**: `MessageDigest.isEqual` compares digests in constant time; an unknown username still performs one hash so both failure paths take the same time (no user-enumeration timing signal).
- **Seeding**: `seedDefaultUser()` creates `admin` / `lab123` (role ADMIN) only when the `users` table is empty — safe to call on every launch.
- **Flow**: `LabTrackApp.startSession` shows `LoginDialog` (modal); on success `MainFrame` opens with the username in the sidebar and a **Log out** button wired via `onLogout(...)` that disposes the window and re-runs the gate. Errors surface as a red inline message in the dialog (never as popups) and clear the password field.
- **Storage**: `users` table (username NOCASE-unique, password_hash, salt, role), accessed only through `UserDao`.

## Threading model

Swing is single-threaded (EDT); SQLite work must not block it.

1. **All UI mutation happens on the EDT** — `LabTrackApp.main` wraps startup in `SwingUtilities.invokeLater`.
2. **All database work happens off the EDT** — `Tasks.run(parent, work, onSuccess)` / `Tasks.runVoid(...)` wrap a `SwingWorker`:
   - `doInBackground()` runs the query (background thread),
   - `done()` delivers the result on the EDT and shows an error dialog on failure.
3. Panels call `refresh()` through `Tasks`; dialogs that write data (`addEquipment`, `editSelected`, `archive/delete`) also use `Tasks.runVoid`.
4. **Writes are transactional** — `DatabaseManager.inTransaction(work)` sets `autoCommit(false)`, commits on success, rolls back on any exception. Issue/return and damage/maintenance each run as one atomic unit (e.g. returning an item damaged writes the return *and* opens the damage record in the same transaction, or neither).
5. **Guard against feedback loops** — `InventoryPanel.reloadingFilter` prevents the category combo's programmatic rebuild from re-triggering its own `refresh()` listener (a real bug once fixed: an infinite refresh loop churning ~92k table rebuilds/second).

## UI structure

```
MainFrame (BorderLayout: sidebar | content, CardLayout for pages)
├── sidebar — 5 nav buttons (GridLayout) + branding/footer
├── DashboardPanel     # 6 stat cards + recent activity
├── InventoryPanel     # CRUD table + filters + buttons
├── BorrowingPanel     # tabs: Active Loans / Overdue / History
├── MaintenancePanel   # tabs: Pending Work / Completed
└── ReportsPanel       # report list (JList) + result table
```

- Pages implement `MainFrame.Refreshable` — navigating to a page calls its `refresh()`, so data is always live.
- Dialogs (`EquipmentDialog`, `IssueDialog`, `ReturnDialog`, `MaintenanceDialog`, `CompleteMaintenanceDialog`) extend `FormDialog`, are modal, validate before closing, and return a result object (`getResult()`).
- `UiUtil` centralizes the flat visual language (colors, fonts, `btn(...)`, `card(...)`, `statCard(...)`, non-editable table model); layouts are manager-based throughout (BorderLayout, GridBagLayout, FlowLayout, GridLayout, BoxLayout) — no absolute positioning.
