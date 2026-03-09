import java.util.List;

// class for creating an income statement
public class IncomeStatement extends FinancialRecord {
    // constructor for an income statement
    public IncomeStatement(Ledger ledger) {
        super("INCOME STATEMENT", ledger);
    }

    @Override
    // builds text for output or rendering
    protected void buildText(StringBuilder sb) {
        List<LedgerAccount> revenues = accountsByType("Revenue");
        List<LedgerAccount> expenses = accountsByType("Expense");

        // 2D arrays for storing revenue and expense accounts
        Object[][] revenueRows = buildRows(revenues);
        Object[][] expenseRows = buildRows(expenses);

        double totalRevenue = 0;
        double totalExpense = 0;

        sb.append("Revenues\n");
        sb.append("------------------------------\n");
        for (Object[] row : revenueRows) {
            String name = (String) row[0];
            double amount = (double) row[1];
            totalRevenue += amount;
            sb.append(String.format("%-30s %12.2f\n", name, amount));
        }
        sb.append(String.format("%-30s %12.2f\n\n", "Total Revenues", totalRevenue));

        sb.append("Expenses\n");
        sb.append("------------------------------\n");
        for (Object[] row : expenseRows) {
            String name = (String) row[0];
            double amount = (double) row[1];
            totalExpense += amount;
            sb.append(String.format("%-30s %12.2f\n", name, amount));
        }
        sb.append(String.format("%-30s %12.2f\n\n", "Total Expenses", totalExpense));
        sb.append(String.format("%-30s %12.2f\n", "Net Income", totalRevenue - totalExpense));
    }

}
