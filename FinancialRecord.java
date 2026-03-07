import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public abstract class FinancialRecord {
    protected final Ledger ledger;
    private final String title;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    protected FinancialRecord(String title, Ledger ledger) {
        this.title = title;
        this.ledger = ledger;
    }

    public String getTitle() {
        return title;
    }

    public final String generate() {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");
        sb.append("Date: ").append(fmt.format(LocalDate.now())).append("\n");
        sb.append("========================================\n\n");
        buildBody(sb);
        return sb.toString();
    }

    protected abstract void buildBody(StringBuilder sb);

    protected List<LedgerAccount> accountsByType(String type) {
        return ledger.getAllAccountsSorted().stream()
            .filter(a -> a != null && a.getId() > 0 && type.equals(a.getType()))
            .collect(Collectors.toList());
    }
}
