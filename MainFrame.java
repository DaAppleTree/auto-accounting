import java.awt.*;
import javax.swing.*;

// class for creating the mainframe
public class MainFrame extends JFrame {
    private static final Color SIDEBAR_BACKGROUND = new Color(240, 240, 240);
    private static final Color NAV_BUTTON_BACKGROUND = new Color(200, 214, 201);

    private final CardLayout contentLayout;
    private final JPanel contentPanel;
    private final Ledger ledger;
    private final Journal journal;

    // constructor for the mainframe
    public MainFrame() {
        setTitle("AutoAccountant");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        ledger = new Ledger();
        journal = new Journal();

        // rebuild ledger balances from any loaded transactions
        ledger.updateFromJournal(journal);

        // create default accounts
        if (ledger.getAccountByName("Cash") == null) {
            LedgerAccount assetCat = ledger.getOrCreateRoot("Asset");
            LedgerAccount liabilityCat = ledger.getOrCreateRoot("Liability");
            LedgerAccount equityCat = ledger.getOrCreateRoot("Equity");
            LedgerAccount revenueCat = ledger.getOrCreateRoot("Revenue");
            LedgerAccount expenseCat = ledger.getOrCreateRoot("Expense");

            ledger.addAccount(new LedgerAccount(101, "Cash", "Asset", assetCat));
            ledger.addAccount(new LedgerAccount(102, "Accounts Receivable", "Asset", assetCat));
            ledger.addAccount(new LedgerAccount(201, "Accounts Payable", "Liability", liabilityCat));
            ledger.addAccount(new LedgerAccount(301, "Owner's Equity", "Equity", equityCat));
            ledger.addAccount(new LedgerAccount(701, "HST Recoverable", "Asset", assetCat));
            ledger.addAccount(new LedgerAccount(702, "HST Payable", "Liability", liabilityCat));
        }

        // adds a sidebar
        add(createSidebar(), BorderLayout.WEST);

        // adds a content Panel
        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        LedgerPage ledgerPage = new LedgerPage(journal, ledger);
        TransactionPage transactionPage = new TransactionPage(journal, ledger, ledgerPage);
        ReportsPage reportsPage = new ReportsPage(journal, ledger);
        ledgerPage.setJournalRefreshAction(transactionPage::refreshJournalDisplay);
        contentPanel.add(transactionPage, "JOURNAL");
        contentPanel.add(ledgerPage, "LEDGER");
        contentPanel.add(reportsPage, "REPORTS");
        
        add(contentPanel, BorderLayout.CENTER);
        
        contentLayout.show(contentPanel, "JOURNAL");
    }

    // creates sidebar used by this screen
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(200, 700));
        sidebar.setBackground(SIDEBAR_BACKGROUND);
        sidebar.setLayout(new GridLayout(6, 1, 5, 5));
        
        JButton txBtn = new JButton("Journal");
        JButton ledgerBtn = new JButton("Ledger");
        JButton reportsBtn = new JButton("Reports");

        styleNavButton(txBtn);
        styleNavButton(ledgerBtn);
        styleNavButton(reportsBtn);
        
        txBtn.addActionListener(e -> contentLayout.show(contentPanel, "JOURNAL"));
        ledgerBtn.addActionListener(e -> contentLayout.show(contentPanel, "LEDGER"));
        reportsBtn.addActionListener(e -> contentLayout.show(contentPanel, "REPORTS"));
        
        sidebar.add(txBtn);
        sidebar.add(ledgerBtn);
        sidebar.add(reportsBtn);
        
        return sidebar;
    }

    // handles style navigation button behavior for mainframe
    private void styleNavButton(JButton button) {
        button.setBackground(NAV_BUTTON_BACKGROUND);
        button.setForeground(new Color(45, 60, 45));
        button.setFont(new Font("Georgia", Font.BOLD, 18));
        button.setMargin(new Insets(12, 8, 12, 8));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
    }

    // main method for running the program
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}
