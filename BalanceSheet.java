import java.util.List;

public class BalanceSheet extends FinancialRecord {
    public BalanceSheet(Ledger ledger) {
        super("BALANCE SHEET", ledger);
    }

    @Override
    protected void buildBody(StringBuilder sb) {
        List<LedgerAccount> assets = accountsByType("Asset");
        List<LedgerAccount> liabilities = accountsByType("Liability");
        List<LedgerAccount> equity = accountsByType("Equity");

        double totalAssets = 0;
        double totalLiabilities = 0;
        double totalEquity = 0;

        sb.append("Assets\n");
        sb.append("------------------------------\n");
        for (LedgerAccount a : assets) {
            double amount = a.getBalance();
            totalAssets += amount;
            sb.append(String.format("%-30s %12.2f\n", a.getName(), amount));
        }
        sb.append(String.format("%-30s %12.2f\n\n", "Total Assets", totalAssets));

        sb.append("Liabilities\n");
        sb.append("------------------------------\n");
        for (LedgerAccount a : liabilities) {
            double amount = a.getBalance();
            totalLiabilities += amount;
            sb.append(String.format("%-30s %12.2f\n", a.getName(), amount));
        }
        sb.append(String.format("%-30s %12.2f\n\n", "Total Liabilities", totalLiabilities));

        sb.append("Equity\n");
        sb.append("------------------------------\n");
        for (LedgerAccount a : equity) {
            double amount = a.getBalance();
            totalEquity += amount;
            sb.append(String.format("%-30s %12.2f\n", a.getName(), amount));
        }
        sb.append(String.format("%-30s %12.2f\n\n", "Total Equity", totalEquity));

        sb.append(String.format("%-30s %12.2f\n", "Liabilities + Equity", totalLiabilities + totalEquity));
        sb.append(String.format("%-30s %12.2f\n", "Difference (Assets - L+E)", totalAssets - (totalLiabilities + totalEquity)));
    }
}
