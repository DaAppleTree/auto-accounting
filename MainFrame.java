import java.awt.*;
import javax.swing.*;

public class MainFrame extends JFrame {
    private CardLayout contentLayout;
    private JPanel contentPanel;
    private Ledger ledger;
    private Journal journal;

    public MainFrame() {
        setTitle("Accounting System");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        ledger = new Ledger();
        journal = new Journal();
        // rebuild ledger balances from any loaded transactions
        ledger.updateFromJournal(journal);

        // if key sample accounts are missing, populate defaults
        if (ledger.getAccountByName("Cash") == null) {
            LedgerAccount assetCat = ledger.getOrCreateAbstractAccount("Asset", "Asset", null);
            LedgerAccount liabilityCat = ledger.getOrCreateAbstractAccount("Liability", "Liability", null);
            LedgerAccount equityCat = ledger.getOrCreateAbstractAccount("Equity", "Equity", null);
            LedgerAccount revenueCat = ledger.getOrCreateAbstractAccount("Revenue", "Revenue", null);
            LedgerAccount expenseCat = ledger.getOrCreateAbstractAccount("Expense", "Expense", null);

            ledger.addAccount(new LedgerAccount(101, "Cash", "Asset", assetCat));
            ledger.addAccount(new LedgerAccount(102, "Accounts Receivable", "Asset", assetCat));
            ledger.addAccount(new LedgerAccount(103, "Supplies", "Asset", assetCat));
            ledger.addAccount(new LedgerAccount(201, "Accounts Payable", "Liability", liabilityCat));
            ledger.addAccount(new LedgerAccount(301, "Owner's Equity", "Equity", equityCat));
            ledger.addAccount(new LedgerAccount(401, "Service Revenue", "Revenue", revenueCat));
            ledger.addAccount(new LedgerAccount(501, "Rent Expense", "Expense", expenseCat));
            ledger.addAccount(new LedgerAccount(502, "Utilities Expense", "Expense", expenseCat));
            // ensure HST accounts exist as well (Ledger constructor already does this but safe)
            ledger.addAccount(new LedgerAccount(701, "HST Receivable", "Asset", assetCat));
            ledger.addAccount(new LedgerAccount(702, "HST Payable", "Liability", liabilityCat));
        }

        // Sidebar
        add(createSidebar(), BorderLayout.WEST);

        // Content Panel
        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        LedgerPage ledgerPage = new LedgerPage(ledger, journal);
        contentPanel.add(new TransactionPage(journal, ledger, ledgerPage), "TRANSACTIONS");
        contentPanel.add(ledgerPage, "LEDGER");
        contentPanel.add(new ReportsPage(journal, ledger), "REPORTS");
        
        add(contentPanel, BorderLayout.CENTER);
        
        contentLayout.show(contentPanel, "TRANSACTIONS");
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(200, 700));
        sidebar.setBackground(new Color(240, 240, 240));
        sidebar.setLayout(new GridLayout(6, 1, 5, 5));
        
        JButton txBtn = new JButton("Transactions");
        JButton ledgerBtn = new JButton("Ledger");
        JButton reportsBtn = new JButton("Records");

        styleNavButton(txBtn);
        styleNavButton(ledgerBtn);
        styleNavButton(reportsBtn);
        
        txBtn.addActionListener(e -> contentLayout.show(contentPanel, "TRANSACTIONS"));
        ledgerBtn.addActionListener(e -> contentLayout.show(contentPanel, "LEDGER"));
        reportsBtn.addActionListener(e -> contentLayout.show(contentPanel, "REPORTS"));
        
        sidebar.add(txBtn);
        sidebar.add(ledgerBtn);
        sidebar.add(reportsBtn);
        
        return sidebar;
    }

    private void styleNavButton(JButton button) {
        button.setBackground(new Color(46, 125, 50));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}