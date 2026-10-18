package labtrack.ui.panels;
import labtrack.service.AppContext;
import labtrack.service.ReportService;
import labtrack.ui.MainFrame;
import labtrack.util.Tasks;
import labtrack.util.UiUtil;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
/** Report catalogue: pick a report on the left, see the live query result on the right. */
public class ReportsPanel extends JPanel implements MainFrame.Refreshable {
    private final AppContext ctx;
    private final List<ReportService.ReportSpec> reports;
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> reportList = new JList<>(listModel);
    private final DefaultTableModel tableModel = UiUtil.tableModel();
    private final JTable table = UiUtil.table(tableModel);
    private final JLabel resultTitle = UiUtil.h2("Select a report"), resultCount = UiUtil.muted("");
    public ReportsPanel(AppContext ctx) {
        this.ctx = ctx;
        this.reports = ctx.reports().reportSpecs();
        reports.forEach(r -> listModel.addElement(r.name()));
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JPanel catalogue = new JPanel(new BorderLayout(0, 10));
        catalogue.setBackground(java.awt.Color.WHITE);
        catalogue.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiUtil.BORDER), BorderFactory.createEmptyBorder(16, 14, 16, 14)));
        catalogue.setPreferredSize(new Dimension(270, 0));
        catalogue.add(UiUtil.h2("Reports"), BorderLayout.NORTH);
        JScrollPane listScroll = new JScrollPane(reportList);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        catalogue.add(listScroll, BorderLayout.CENTER);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(UiUtil.BORDER));
        JPanel head = new JPanel(new BorderLayout(0, 4));
        head.setOpaque(false);
        head.add(resultTitle, BorderLayout.NORTH);
        head.add(resultCount, BorderLayout.SOUTH);
        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(head, BorderLayout.NORTH);
        north.add(UiUtil.bar(UiUtil.btn("Refresh", UiUtil.Kind.SECONDARY, this::loadSelected)), BorderLayout.SOUTH);
        JPanel result = new JPanel(new BorderLayout(0, 12));
        result.setOpaque(false);
        result.add(north, BorderLayout.NORTH);
        result.add(tableScroll, BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, catalogue, result);
        split.setResizeWeight(0.24);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
        reportList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reportList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && reportList.getSelectedIndex() >= 0) loadSelected();
        });
    }
    @Override
    public void refresh() {
        if (reportList.getSelectedIndex() < 0 && !listModel.isEmpty()) reportList.setSelectedIndex(0);
        else loadSelected();
    }
    private void loadSelected() {
        int index = reportList.getSelectedIndex();
        if (index < 0) return;
        ReportService.ReportSpec spec = reports.get(index);
        resultTitle.setText(spec.name());
        Tasks.run(this, spec.loader()::load, rows -> {
            tableModel.setColumnIdentifiers(spec.columns());
            tableModel.setRowCount(0);
            for (String[] row : rows) tableModel.addRow(row);
            resultCount.setText(rows.size() + " row(s) · from live database query");
        });
    }
}
