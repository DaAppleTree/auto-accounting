import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

// class for creating a reports page
public class ReportsPage extends JPanel {
    private static final Color BUTTON_NEUTRAL = new Color(96, 125, 139);
    private static final Color BUTTON_SUCCESS = new Color(46, 125, 50);
    private static final Color BUTTON_INFO = new Color(21, 101, 192);
    private static final Color BUTTON_ACCENT = new Color(123, 31, 162);
    private static final Color BUTTON_PRIMARY = new Color(2, 119, 189);

    private final Journal journal;
    private final Ledger ledger;
    private final JTextArea reportArea;
    private final List<FinancialRecord> reports;

    // constructor for the reports page
    public ReportsPage(Journal journal, Ledger ledger) {
        this.journal = journal;
        this.ledger = ledger;
        this.reports = new ArrayList<>();
        
        // stores different financial report types together using polymorphism
        reports.add(new IncomeStatement(ledger));
        reports.add(new BalanceSheet(ledger));
        
        setLayout(new BorderLayout());
        add(createTopPanel(), BorderLayout.NORTH);
        
        reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane reportScroll = new JScrollPane(reportArea);
        
        // add buttons
        JButton printJournalBtn = new JButton("Print Journal");
        printJournalBtn.addActionListener(e -> printJournal());
        styleButton(printJournalBtn, BUTTON_NEUTRAL);

        JButton incomeStmtBtn = new JButton("Generate Income Statement");
        incomeStmtBtn.addActionListener(e -> showRecordByTitle("INCOME STATEMENT"));
        styleButton(incomeStmtBtn, BUTTON_SUCCESS);

        JButton balanceSheetBtn = new JButton("Generate Balance Sheet");
        balanceSheetBtn.addActionListener(e -> showRecordByTitle("BALANCE SHEET"));
        styleButton(balanceSheetBtn, BUTTON_INFO);

        JButton allReportsBtn = new JButton("Generate All Reports");
        allReportsBtn.addActionListener(e -> generateAllReports());
        styleButton(allReportsBtn, BUTTON_ACCENT);

        JButton ratiosBtn = new JButton("Show Financial Ratios");
        ratiosBtn.addActionListener(e -> showFinancialRatios());
        styleButton(ratiosBtn, BUTTON_PRIMARY);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(printJournalBtn);
        buttonPanel.add(incomeStmtBtn);
        buttonPanel.add(balanceSheetBtn);
        buttonPanel.add(allReportsBtn);
        buttonPanel.add(ratiosBtn);

        add(reportScroll, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // creates top panel
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("REPORTS");
        title.setFont(new Font("Georgia", Font.BOLD, 24));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(title, BorderLayout.CENTER);
        return panel;
    }

    // styles buttons
    private void styleButton(JButton button, Color background) {
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
    }

    // shows record by title
    private void showRecordByTitle(String title) {
        ledger.updateFromJournal(journal);
        for (FinancialRecord report : reports) {
            if (report.getTitle().equals(title)) {
                reportArea.setText(report.generate());
                return;
            }
        }
        reportArea.setText("Report type not found: " + title);
    }

    // handles generate all reports behavior
    private void generateAllReports() {
        ledger.updateFromJournal(journal);
        StringBuilder sb = new StringBuilder();
        for (FinancialRecord report : reports) {
            sb.append(report.generate()).append("\n\n");
        }
        reportArea.setText(sb.toString());
    }

    // prints or renders journal for the user
    private void printJournal() {
        reportArea.setText("JOURNAL ENTRIES\n");
        reportArea.append("==================\n\n");
        
        for (Transaction t : journal.getAllTransactions()) {
            reportArea.append(t.toString() + "\n");
        }
    }

    // displays financial ratios
    private void showFinancialRatios() {
        ledger.updateFromJournal(journal);

        double currentAssets = ledger.getRecursiveTotalByName("Current Asset");
        double currentLiabilities = ledger.getRecursiveTotalByName("Current Liability");
        double totalAssets = ledger.getRecursiveTotalByName("Asset");
        double totalLiabilities = ledger.getRecursiveTotalByName("Liability");
        double equity = ledger.getRecursiveTotalByName("Equity");
        double revenue = ledger.getRecursiveTotalByName("Revenue");
        double expense = ledger.getRecursiveTotalByName("Expense");
        double netIncome = revenue - expense;

        double cash = valueOf("Cash");
        double receivables = valueOf("Accounts Receivable");
        double hstReceivable = valueOf("HST Recoverable");
        double quickAssets = cash + receivables + hstReceivable;

        StringBuilder sb = new StringBuilder();
        sb.append("FINANCIAL RATIOS\n");
        sb.append("==============================\n\n");

        appendRatio(sb, "Current Ratio", "Current Assets / Current Liabilities", ">= 2.0 : 1", formatRatio(currentAssets, currentLiabilities));
        appendRatio(sb, "Quick Ratio", "(Cash + A/R + HST Recoverable) / Current Liabilities", ">= 1.0 : 1", formatRatio(quickAssets, currentLiabilities));
        appendRatio(sb, "Debt-to-Equity", "Total Liabilities / Total Equity", "<= 1.5 : 1", formatRatio(totalLiabilities, equity));
        appendRatio(sb, "Debt Ratio", "Total Liabilities / Total Assets", "<= 0.50", formatDecimalRatio(totalLiabilities, totalAssets));
        appendRatio(sb, "Net Profit Margin", "Net Income / Revenue", ">= 10%", formatPercent(netIncome, revenue));
        appendRatio(sb, "Return on Equity (ROE)", "Net Income / Total Equity", ">= 15%", formatPercent(netIncome, equity));

        reportArea.setText(sb.toString());
    }

    // gets the balance of an account
    private double valueOf(String accountName) {
        LedgerAccount account = ledger.getAccountByName(accountName);
        return account != null ? account.getBalance() : 0;
    }

    // adds financial ratios to table
    private void appendRatio(StringBuilder sb, String name, String definition, String threshold, String currentValue) {
        sb.append(name).append("\n");
        sb.append("  Definition: ").append(definition).append("\n");
        sb.append("  Good Threshold: ").append(threshold).append("\n");
        sb.append("  Current Value: ").append(currentValue).append("\n\n");
    }

    // formats ratio for display
    private String formatRatio(double numerator, double denominator) {
        if (Math.abs(denominator) < 0.0000001) {
            return "N/A";
        }
        return String.format("%.2f : 1", numerator / denominator);
    }

    // formats decimal ratio for display
    private String formatDecimalRatio(double numerator, double denominator) {
        if (Math.abs(denominator) < 0.0000001) {
            return "N/A";
        }
        return String.format("%.2f", numerator / denominator);
    }

    // formats percent for display
    private String formatPercent(double numerator, double denominator) {
        if (Math.abs(denominator) < 0.0000001) {
            return "N/A";
        }
        return String.format("%.2f%%", (numerator / denominator) * 100.0);
    }
}
