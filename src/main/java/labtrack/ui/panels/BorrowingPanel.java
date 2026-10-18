package labtrack.ui.panels;
import labtrack.model.Condition;
import labtrack.model.Equipment;
import labtrack.model.EquipmentStatus;
import labtrack.model.IssueRecord;
import labtrack.model.IssueStatus;
import labtrack.service.AppContext;
import labtrack.ui.MainFrame;
import labtrack.ui.dialogs.IssueDialog;
import labtrack.ui.dialogs.ReturnDialog;
import labtrack.util.Tasks;
import labtrack.util.UiUtil;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
/** Issue and return screen: active loans, overdue tracking and full history. */
public class BorrowingPanel extends JPanel implements MainFrame.Refreshable {
    private final AppContext ctx;
    private final JTextField searchField = UiUtil.field("Search borrower or equipment…");
    private final DefaultTableModel activeModel = UiUtil.tableModel("ID", "Equipment", "Borrower", "Issued", "Due", "Days Overdue", "Status");
    private final DefaultTableModel overdueModel = UiUtil.tableModel("ID", "Equipment", "Borrower", "Issued", "Due", "Days Overdue", "Status");
    private final DefaultTableModel historyModel = UiUtil.tableModel("ID", "Equipment", "Borrower", "Issued", "Due", "Returned", "Condition", "Status", "Remarks");
    private final JTable activeTable = UiUtil.table(activeModel);
    private final JTable overdueTable = UiUtil.table(overdueModel);
    private final JTable historyTable = UiUtil.table(historyModel);
    private final JTabbedPane tabs = new JTabbedPane();
    private List<IssueRecord> activeRows = new ArrayList<>(), overdueRows = new ArrayList<>(), historyRows = new ArrayList<>();
    public BorrowingPanel(AppContext ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        searchField.setColumns(24);
        searchField.addActionListener(e -> refresh());
        add(UiUtil.bar(searchField, UiUtil.btn("Search", UiUtil.Kind.PRIMARY, this::refresh),
                UiUtil.btn("+ Issue Equipment", UiUtil.Kind.PRIMARY, this::issueEquipment),
                UiUtil.btn("Record Return", UiUtil.Kind.SUCCESS, this::recordReturn),
                UiUtil.btn("Refresh", UiUtil.Kind.SECONDARY, this::refresh)), BorderLayout.NORTH);
        tabs.addTab("Active Loans", wrap(activeTable, "No active loans. Use \"Issue Equipment\" to lend an item."));
        tabs.addTab("Overdue", wrap(overdueTable, "Nothing is overdue. All active loans are within their due date."));
        tabs.addTab("History", wrap(historyTable, "No borrowing history yet."));
        add(tabs, BorderLayout.CENTER);
    }
    private JPanel wrap(JTable table, String emptyMessage) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UiUtil.BORDER));
        JPanel p = new JPanel(new BorderLayout());
        p.add(scroll, BorderLayout.CENTER);
        p.add(UiUtil.muted(emptyMessage), BorderLayout.SOUTH);
        return p;
    }
    @Override
    public void refresh() {
        Tasks.run(this, () -> new Object[]{ctx.issues().list("ACTIVE", searchField.getText()), ctx.issues().overdue(),
                ctx.issues().list(null, searchField.getText())}, data -> {
            @SuppressWarnings("unchecked")
            List<IssueRecord> active = (List<IssueRecord>) data[0];
            @SuppressWarnings("unchecked")
            List<IssueRecord> overdue = (List<IssueRecord>) data[1];
            @SuppressWarnings("unchecked")
            List<IssueRecord> history = (List<IssueRecord>) data[2];
            activeRows = active; overdueRows = overdue; historyRows = history;
            fill(activeModel, active, false);
            fill(overdueModel, overdue, false);
            fill(historyModel, history, true);
        });
    }
    private void fill(DefaultTableModel model, List<IssueRecord> rows, boolean history) {
        model.setRowCount(0);
        for (IssueRecord r : rows) {
            if (history)
                model.addRow(new Object[]{r.getId(), r.getEquipmentLabel(), r.getBorrowerLabel(), r.getIssuedDate(), r.getDueDate(),
                        r.getReturnedDate() == null ? "" : r.getReturnedDate(), r.getReturnCondition() == null ? "" : r.getReturnCondition().label(), r.getStatus().label(), r.getRemarks()});
            else
                model.addRow(new Object[]{r.getId(), r.getEquipmentLabel(), r.getBorrowerLabel(), r.getIssuedDate(), r.getDueDate(),
                        r.isOverdue() ? r.overdueDays() + " day(s)" : "—", r.getStatus().label()});
        }
    }
    private IssueRecord selected() {
        int idx = tabs.getSelectedIndex();
        JTable table = idx == 0 ? activeTable : idx == 1 ? overdueTable : historyTable;
        List<IssueRecord> rows = idx == 0 ? activeRows : idx == 1 ? overdueRows : historyRows;
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        return modelRow < rows.size() ? rows.get(modelRow) : null;
    }
    private void issueEquipment() {
        Tasks.run(this, () -> ctx.equipment().list(null, 0, null, EquipmentStatus.AVAILABLE, false).stream().filter(Equipment::isBorrowable).toList(), available -> {
            if (available.isEmpty()) { UiUtil.info(this, "No lendable equipment is available right now."); return; }
            IssueDialog dialog = new IssueDialog(UiUtil.owner(this), available, null);
            if (!dialog.open()) return;
            IssueDialog.Request req = dialog.getResult();
            Tasks.runVoid(this, () -> ctx.issues().issue(req.equipmentId(), req.name(), req.code(), req.role(), req.email(), req.phone(), req.issued(), req.due(), req.remarks()),
                    () -> { UiUtil.info(this, "Equipment issued to " + req.name() + "."); refresh(); });
        });
    }
    private void recordReturn() {
        IssueRecord record = selected();
        if (record == null) { UiUtil.info(this, "Select a loan record first."); return; }
        if (record.getStatus() == IssueStatus.RETURNED) { UiUtil.showError(this, "This item was already returned on " + record.getReturnedDate() + "."); return; }
        ReturnDialog dialog = new ReturnDialog(UiUtil.owner(this), record);
        if (!dialog.open()) return;
        ReturnDialog.Request req = dialog.getResult();
        Tasks.runVoid(this, () -> ctx.issues().returnItem(record.getId(), req.returnedDate(), req.condition(), req.remarks()), () -> {
            UiUtil.info(this, req.condition() == Condition.DAMAGED
                    ? "Return recorded. A damage record was opened — the item stays locked for repair."
                    : "Return recorded. The item is available again.");
            refresh();
        });
    }
}
