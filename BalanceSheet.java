import java.util.List;

// class for creating a balance sheet
public class BalanceSheet extends FinancialRecord {
    // constructor for a balance sheet
    public BalanceSheet(Ledger ledger) {
        super("BALANCE SHEET", ledger);
    }

    @Override
    // builds text for output or rendering
    protected void buildText(StringBuilder sb) {
        List<LedgerAccount> assets = accountsByType("Asset");
        List<LedgerAccount> liabilities = accountsByType("Liability");
        List<LedgerAccount> equity = accountsByType("Equity");

        // 2D arrays for storing asset, liability, and equity accounts
        Object[][] assetRows = buildRows(assets);
        Object[][] liabilityRows = buildRows(liabilities);
        Object[][] equityRows = buildRows(equity);

        double totalAssets = 0;
        double totalLiabilities = 0;
        double totalEquity = 0;

        sb.append("Assets\n");
        sb.append("------------------------------\n");
        for (Object[] row : assetRows) {
            String name = (String) row[0];
            double amount = (double) row[1];
            totalAssets += amount;
            sb.append(String.format("%-30s %12.2f\n", name, amount));
        }
        sb.append(String.format("%-30s %12.2f\n\n", "Total Assets", totalAssets));

        sb.append("Liabilities\n");
        sb.append("------------------------------\n");
        for (Object[] row : liabilityRows) {
            String name = (String) row[0];
            double amount = (double) row[1];
            totalLiabilities += amount;
            sb.append(String.format("%-30s %12.2f\n", name, amount));
        }
        sb.append(String.format("%-30s %12.2f\n\n", "Total Liabilities", totalLiabilities));

        sb.append("Equity\n");
        sb.append("------------------------------\n");
        for (Object[] row : equityRows) {
            String name = (String) row[0];
            double amount = (double) row[1];
            totalEquity += amount;
            sb.append(String.format("%-30s %12.2f\n", name, amount));
        }
        sb.append(String.format("%-30s %12.2f\n\n", "Total Equity", totalEquity));
        sb.append(String.format("%-30s %12.2f\n", "Liabilities + Equity", totalLiabilities + totalEquity));
        sb.append(String.format("%-30s %12.2f\n", "Total Equity Change", totalAssets - (totalLiabilities + totalEquity)));
    }

}
