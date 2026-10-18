package labtrack.model;
import labtrack.util.Dates;
import java.time.LocalDate;
public class IssueRecord {
    private int id;
    private final int equipmentId, borrowerId;
    private final String equipmentLabel, borrowerLabel, remarks;
    private final LocalDate issuedDate, dueDate, returnedDate;
    private final Condition returnCondition;
    private final IssueStatus status;
    public IssueRecord(int id, int equipmentId, String equipmentLabel, int borrowerId, String borrowerLabel,
                       LocalDate issuedDate, LocalDate dueDate, LocalDate returnedDate,
                       Condition returnCondition, String remarks, IssueStatus status) {
        this.id = id; this.equipmentId = equipmentId; this.equipmentLabel = equipmentLabel;
        this.borrowerId = borrowerId; this.borrowerLabel = borrowerLabel;
        this.issuedDate = issuedDate; this.dueDate = dueDate; this.returnedDate = returnedDate;
        this.returnCondition = returnCondition; this.remarks = remarks; this.status = status;
    }
    public boolean isOverdue() { return status == IssueStatus.ACTIVE && Dates.isOverdue(dueDate); }
    public long overdueDays() { return status == IssueStatus.ACTIVE ? Dates.overdueDays(dueDate) : 0; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEquipmentId() { return equipmentId; }
    public String getEquipmentLabel() { return equipmentLabel; }
    public int getBorrowerId() { return borrowerId; }
    public String getBorrowerLabel() { return borrowerLabel; }
    public LocalDate getIssuedDate() { return issuedDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnedDate() { return returnedDate; }
    public Condition getReturnCondition() { return returnCondition; }
    public String getRemarks() { return remarks; }
    public IssueStatus getStatus() { return status; }
}
