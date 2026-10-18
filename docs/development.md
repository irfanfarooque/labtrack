# LabTrack — Development Guide

Build setup, project conventions, and how to extend the application.

## Toolchain

| Tool | Version | Notes |
|---|---|---|
| Java | 17 target (`maven.compiler.release=17`) | Built with any newer JDK (this machine runs JDK 25) |
| Maven | 3.9.x | On this machine a local copy is used: `/tmp/opencode/tools/apache-maven-3.9.9` — `export PATH=/tmp/opencode/tools/apache-maven-3.9.9/bin:$PATH` |
| JUnit | 5.10.2 | Test scope only |

Dependencies (all bundled into the final jar by maven-shade-plugin):

- `org.xerial:sqlite-jdbc:3.46.1.0` — embedded database, offline runtime
- `com.formdev:flatlaf:3.4.1` — modern Swing look and feel (falls back to the system L&F if unavailable)
- `org.junit.jupiter:junit-jupiter:5.10.2` (test)

## Common commands

```bash
export PATH=/tmp/opencode/tools/apache-maven-3.9.9/bin:$PATH   # this machine only

mvn clean package      # build + test + target/labtrack-1.0.0.jar
mvn test               # tests only
mvn test -Dtest=Xxx    # single test class
java -jar target/labtrack-1.0.0.jar
```

The shade plugin writes `Main-Class: labtrack.LabTrackApp` into the manifest, so the jar runs standalone (SQLite driver and FlatLaf included). `dependency-reduced-pom.xml` is a shade-plugin artifact — ignore it.

## Source layout (49 files, ~2,500 lines)

```
src/main/java/labtrack/
├── LabTrackApp.java          # main(): L&F → AppContext → seed → MainFrame
├── model/                    # 17 files
│   ├── Equipment.java         # abstract base (implements Borrowable, Maintainable)
│   ├── ElectronicEquipment.java, ComputerEquipment.java
│   ├── Borrowable.java, Maintainable.java          # interfaces
│   ├── IssueRecord.java, MaintenanceRecord.java, Borrower.java, Category.java
│   └── Condition, EquipmentStatus, IssueStatus, MaintenanceType, MaintenanceStatus  # enums
├── dao/                      # 5 files — all SQL, prepared statements only
├── service/                  # 5 files — AppContext + 4 services (business rules)
├── database/DatabaseManager.java                   # singleton, schema, transactions
├── ui/
│   ├── MainFrame.java         # sidebar nav + CardLayout pages
│   ├── panels/                # Dashboard, Inventory, Borrowing, Maintenance, Reports
│   └── dialogs/               # FormDialog base + 5 dialogs
├── adapter/                   # LegacyAssetRecord, EquipmentAdapter, LegacyTsvAdapter, LegacyDataSeeder
├── exception/                 # LabTrackException + Validation/DuplicateRecord/OperationException
└── util/                      # Tasks (SwingWorker), Dates, UiUtil
src/main/resources/legacy_seed.tsv                   # 16 first-run items
src/test/java/labtrack/       # 4 files, ~330 lines
```

## Conventions

- **Layout managers only** — never absolute positioning.
- **No SQL outside `dao/`**, no Swing outside `ui/` (models/DAOs/services are UI-free).
- **DB work off the EDT** via `Tasks.run`/`Tasks.runVoid`; UI updates only in the callback.
- **Multi-statement writes** go through `db.inTransaction(...)`; validate *before* opening the transaction where possible.
- **Business errors** are `LabTrackException` subclasses with user-readable messages — the UI shows `getMessage()` directly, so write messages a lab user understands.
- Every visible control must do something real (no placeholders); every screen refreshes on navigation (`MainFrame.Refreshable`).
- Tests must not touch `~/.labtrack` — always initialize `TestDb.DIR` first.

## Recipes

### Adding a field to equipment

1. Column + DDL in `DatabaseManager.initSchema` (new columns need `IF NOT EXISTS`-style care for existing databases — SQLite supports `ALTER TABLE ... ADD COLUMN`; apply it defensively or accept it only for fresh DBs).
2. Field/getter/setter in `Equipment`.
3. Map it in `EquipmentDao` (`insert`/`map`), validate in `EquipmentService.create/update`.
4. Expose it in `EquipmentDialog` (form row + `fill()` + `build()`), copy it in `InventoryPanel.editSelected()` / `addEquipment()`.
5. Add/extend a test in `LabTrackIntegrationTest`.

### Adding a report

One entry in `ReportService.reportSpecs()`:

```java
new ReportSpec("My Report", new String[]{"Col A", "Col B"}, myDao::myQuery)
```

The Reports screen picks it up automatically — no UI change needed. Query lives in `ReportDao` (or reuse an existing service method).

### Adding a screen

1. New panel in `ui/panels/` implementing `MainFrame.Refreshable`.
2. Register it in `MainFrame`: `addPage("key", new MyPanel(ctx), "Title", "subtitle")` — the sidebar button, header and navigation are created automatically.

### Adding a dialog

Extend `FormDialog`: build fields with `UiUtil.formRow(...)`, implement `build()` to validate and set the result (throw `ValidationException` for bad input — `FormDialog` shows it as an error and keeps the dialog open), then call `open()`. Take the owner `Frame` in the constructor.

## Known implementation details worth remembering

- `Category` must keep `equals()/hashCode()` (by database id) — combo-box selection depends on it; without them the edit dialog silently loses the category.
- `InventoryPanel.reloadingFilter` guard must stay around `reloadCategoryFilter()` — removing it reintroduces the infinite refresh loop (see `InventorySelectionTest`).
- The sidebar button panel needs `GridLayout`, not `BorderLayout` (BorderLayout silently drops all but the last added component — it once showed only "Reports"; `UiSmokeTest` guards this).
- `TestDb.DIR` must be read before `DatabaseManager` first initializes in any JVM run.

## Size budget

Current: main **2,488** lines / tests **332** (was ~4,800 before compression). The UI deliberately uses plain look-and-feel styling (no custom cell renderers, minimal decoration) to stay lean — prefer simplifying over adding visual layers.
