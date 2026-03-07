import java.util.List;

public class IncomeStatement extends FinancialRecord {
    public IncomeStatement(Ledger ledger) {
        super("INCOME STATEMENT", ledger);
    }

    @Override
    protected void buildBody(StringBuilder sb) {
        List<LedgerAccount> revenues = accountsByType("Revenue");
        List<LedgerAccount> expenses = accountsByType("Expense");

        double totalRevenue = 0;
        double totalExpense = 0;

        sb.append("Revenues\n");
        sb.append("------------------------------\n");
        for (LedgerAccount a : revenues) {
            double amount = a.getBalance();
            totalRevenue += amount;
            sb.append(String.format("%-30s %12.2f\n", a.getName(), amount));
        }
        sb.append(String.format("%-30s %12.2f\n\n", "Total Revenues", totalRevenue));

        sb.append("Expenses\n");
        sb.append("------------------------------\n");
        for (LedgerAccount a : expenses) {
            double amount = a.getBalance();
            totalExpense += amount;
            sb.append(String.format("%-30s %12.2f\n", a.getName(), amount));
        }
        sb.append(String.format("%-30s %12.2f\n\n", "Total Expenses", totalExpense));

        sb.append(String.format("%-30s %12.2f\n", "Net Income", totalRevenue - totalExpense));
    }
}
