# LabTrack — User Guide

Screen-by-screen guide to the application, including the business rules the app enforces.

## Starting the app

```bash
java -jar target/labtrack-1.0.0.jar
```

The **login screen** appears first. On the very first launch a sample account is created automatically:

| Username | Password |
|---|---|
| `admin` | `lab123` |

Type the credentials and press **Log In** (or hit Enter). Wrong credentials show an error and clear the password field; **Exit** closes the app. After signing in, the main window opens on the **Dashboard**. The sidebar shows who is signed in and has a **Log out** button that returns to the login screen.

All data lives locally in `~/.labtrack/labtrack.db` — no server, no internet. On the first launch (empty database) the app also imports 16 sample items from its bundled legacy export so every screen has data to show.

---

## 1. Dashboard

Six statistic cards, refreshed live whenever you navigate to this screen:

| Card | Meaning |
|---|---|
| Total Equipment | All non-archived items in the inventory |
| Available | Items that can be lent right now |
| Currently Issued | Items with an active loan |
| Damaged | Items locked by a damage report |
| Under Maintenance | Items locked by a maintenance request |
| Overdue Returns | Active loans past their due date |

Below the cards, **Recent Activity** shows the last 9 recorded events (issues, returns, damage/maintenance reports) from the database.

---

## 2. Equipment Inventory

The main CRUD screen. Toolbar: search box, category/condition/status filters, "Include archived" checkbox, then the action buttons.

### Search and filter

- Typing in the search box and pressing **Enter** searches name, asset tag, serial and manufacturer.
- The three dropdowns and the checkbox filter live (each change reloads the table).
- **Refresh** reloads manually.

### + Add Equipment

Opens the add dialog. Fields:

- **Asset tag** (required, must be unique — e.g. `AST-0042`)
- **Name** (required)
- **Type** — Electronic Equipment or Computer Equipment (this is the inheritance subclass; it affects borrowing rules and inspection checklists)
- **Category** (required — create one first with **+ Category** if the list is empty)
- **Serial number** (optional, but must be unique if given)
- **Manufacturer, Location, Purchase date** (`yyyy-MM-dd`), **Condition, Notes**

Validation runs before saving: blank required fields, bad dates, duplicate asset tags and duplicate serial numbers are all rejected with a clear error message.

### Edit Selected

Select a row, click **Edit Selected** — the same dialog opens pre-filled (including the correct category). Save updates the record and the table reloads immediately.

Double-clicking a row opens it in **read-only view**.

### Archive / Restore

- **Archive** hides an item from normal views but keeps it (and all its history) in the database. Archived items cannot be lent; edit them by ticking *Include archived* and restoring first.
- **Restore** brings it back.

### Delete

Permanent — and only allowed when the item has **no borrowing or maintenance history at all**. Otherwise the app refuses and tells you to use Archive instead. This protects historical records.

### + Category

Adds a new category name, which then appears in the Add/Edit dialogs and the category filter.

### Status values

`Available → Issued → Available` (lending cycle), `Damaged` / `Maintenance` (locked, set by reports), `Archived` (hidden). Locked items cannot be lent until resolved.

---

## 3. Issue & Return

Three tabs: **Active Loans**, **Overdue**, **History**.

### + Issue Equipment

Select an available item, fill in:

- Borrower name and Borrower ID (both required; a returning borrower is matched by ID)
- Role, email, phone (optional)
- Issue date and due date (required; due date cannot be before the issue date)
- Remarks

Enforced rules:

- Only items in a lendable state can be issued (status Available, not Damaged/Poor — computers and electronics have stricter rules than the base class).
- An item with an **active loan** cannot be issued again.
- On success the item's status becomes *Issued* and the record appears under Active Loans.

### Record Return

Select an active loan, then **Record Return**:

- Return date and return condition are required.
- An item cannot be returned twice.
- If the return condition is **Damaged**, the app automatically opens a damage record and locks the item (status *Damaged*).
- Otherwise the item goes back to *Available*.

### Overdue tracking

The **Overdue** tab lists active loans past their due date with the number of days overdue; the dashboard counts them too. Nothing is marked overdue automatically in the database — overdue is computed from the due date at query time.

---

## 4. Damage & Maintenance

Two tabs: **Pending Work**, **Completed**.

### Report Damage… / Request Maintenance…

Pick an item, describe the problem, enter who reported it. The item is immediately **locked** (status *Damaged* or *Maintenance*) and cannot be issued while a record is pending.

### Start Work

Moves a pending record to *In progress*.

### Complete…

Select a record, enter resolution notes and the new condition. The record moves to the **Completed** tab, and the item is **unlocked** (available again with its updated condition).

---

## 5. Reports

Pick one of 10 reports on the left; the right side runs a live SQL query and shows the result with a row count.

1. Complete Inventory
2. Equipment by Category & Condition
3. Items per Category
4. Available vs Issued (by Status)
5. Active Borrowing Records
6. Overdue Returns (with days overdue)
7. Damaged Equipment
8. Pending Maintenance
9. Completed Maintenance
10. Equipment Borrowing History

**Refresh** re-runs the selected report against the current data.

---

## Troubleshooting

| Problem | Solution |
|---|---|
| Changes don't appear | Press **Refresh** on the screen, or switch screens (navigation refreshes automatically) |
| Want a clean slate | Close the app, `rm -rf ~/.labtrack`, relaunch (re-seeds the 16 sample items) |
| Database somewhere else | Launch with `-Dlabtrack.db.dir=/some/path` |
| Editing raw data directly | You can open `~/.labtrack/labtrack.db` with any SQLite tool, but prefer the app — manual edits can violate status rules (e.g. setting an item Available while a loan is active) |
| Old instance running | Close it first; two instances on one database can cause "database is locked" errors |
