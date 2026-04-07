import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

// abstract class for financial records shown in the reports page
public abstract class FinancialRecord {

    // protected so subclasses can access ledger data directly
    protected final Ledger ledger;
    
    // record title shown in generated output.
    private final String title;
    
    // shared date format for report header output
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // constructor used by subclasses
    protected FinancialRecord(String title, Ledger ledger) {
        this.title = title;
        this.ledger = ledger;
    }

    // returns the display title for this record type.
    public String getTitle() {
        return title;
    }

    // builds the full record text with a common header and subclass-specific body
    public final String generate() {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");
        sb.append("As of ").append(fmt.format(LocalDate.now())).append("\n");
        sb.append("========================================\n\n");
        buildText(sb);
        return sb.toString();
    }

    // abstract method for subclasses to append record-specific lines
    protected abstract void buildText(StringBuilder sb);

    // shared helper that returns only leaf accounts matching a specific type
    protected ArrayList<LedgerAccount> accountsByType(String type) {
        ArrayList<LedgerAccount> matchingAccounts = new ArrayList<>();

        for (LedgerAccount account : ledger.getAllAccountsSorted()) {
            if (account != null && account.getId() > 0 && type.equals(account.getType())) {
                matchingAccounts.add(account);
            }
        }

        return matchingAccounts;
    }

    // shared helper that builds table rows from account name and balance values
    protected Object[][] buildRows(ArrayList<LedgerAccount> accounts) {
        Object[][] rows = new Object[accounts.size()][2];
        for (int i = 0; i < accounts.size(); i++) {
            LedgerAccount account = accounts.get(i);
            rows[i][0] = account.getName();
            rows[i][1] = account.getBalance();
        }
        return rows;
    }
}

