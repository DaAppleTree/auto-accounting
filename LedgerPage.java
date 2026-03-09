import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

// class for creating and formatting the ledger page
public class LedgerPage extends JPanel {
    private static final Color BUTTON_DANGER = new Color(198, 40, 40);
    private static final Color BUTTON_PRIMARY = new Color(2, 119, 189);
    private static final Color BUTTON_CREATE = new Color(0, 100, 200);
    private static final Color BUTTON_SUCCESS = new Color(56, 142, 60);
    private static final Color BUTTON_ACCENT = new Color(123, 31, 162);
    private static final Color PANEL_NEUTRAL = new Color(245, 245, 245);

    private Journal journal;
    private Ledger ledger;
    private JTable ledgerTable;
    private LedgerTableModel ledgerTableModel;
    private JLabel summaryLabel;
    private JPanel accountCreationPanel;
    private JTextField idField;
    private JTextField nameField;
    private JComboBox<String> typeCombo;
    private JComboBox<String> subtypeCombo;
    private CardLayout ledgerViewLayout;
    private JPanel ledgerViewPanel;
    private JPanel groupedLedgerPanel;
    private JScrollPane groupedLedgerScroll;
    private JToggleButton groupedViewToggle;
    private JButton deleteRowBtn;
    private JButton viewGraphBtn;
    private Runnable journalRefreshAction;

    // predefined subtype options for each account type
    private static final java.util.Map<String, String[]> subtypeMap = new java.util.HashMap<>();
    static {
        subtypeMap.put("Asset", new String[]{"None", "Current Asset", "Fixed Asset"});
        subtypeMap.put("Liability", new String[]{"None", "Current Liability", "Long-Term Liability"});
        subtypeMap.put("Equity", new String[]{"None", "Owner's Equity"});
        subtypeMap.put("Revenue", new String[]{"None", "Service Revenue", "Sales Revenue", "Cost of Goods Sold"});
        subtypeMap.put("Expense", new String[]{"None", "Rent Expense", "Utilities Expense", "COGS"});
    }

    // constructor for the ledger page
    public LedgerPage(Journal journal, Ledger ledger) {
        this.journal = journal;
        this.ledger = ledger;
        
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // top panel for title
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        //center panel for switchable ledger display
        ledgerTableModel = new LedgerTableModel();
        ledgerTable = new JTable(ledgerTableModel);
        ledgerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // adjust column widths
        ledgerTable.getColumnModel().getColumn(0).setPreferredWidth(60); // ID
        ledgerTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Name
        ledgerTable.getColumnModel().getColumn(2).setPreferredWidth(80); // Type
        JScrollPane tableScroll = new JScrollPane(ledgerTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            "Ledger Accounts (All Accounts by ID)", 
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14)
        ));
        groupedLedgerPanel = new JPanel();
        groupedLedgerPanel.setLayout(new BoxLayout(groupedLedgerPanel, BoxLayout.Y_AXIS));
        groupedLedgerScroll = new JScrollPane(groupedLedgerPanel);
        groupedLedgerScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Ledger Accounts (Grouped by Type/Subtype)",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14)
        ));

        ledgerViewLayout = new CardLayout();
        ledgerViewPanel = new JPanel(ledgerViewLayout);
        ledgerViewPanel.add(tableScroll, "FLAT");
        ledgerViewPanel.add(groupedLedgerScroll, "GROUPED");
        add(ledgerViewPanel, BorderLayout.CENTER);
        
        // summary label below table
        summaryLabel = new JLabel(" ");
        add(summaryLabel, BorderLayout.SOUTH);

        // creates right panel for account creation panel
        accountCreationPanel = createAccountCreationPanel();
        JScrollPane rightScroll = new JScrollPane(accountCreationPanel);
        rightScroll.setPreferredSize(new Dimension(350, 0));
        rightScroll.setBorder(BorderFactory.createTitledBorder(
            "Create New Account"
        ));
        add(rightScroll, BorderLayout.EAST);

        // creates bottom panel for control buttons and delete section
        JPanel bottomPanel = createBottomPanel();
        
        // delete selected row button
        deleteRowBtn = new JButton("Delete Selected");
        styleButton(deleteRowBtn, BUTTON_DANGER);
        deleteRowBtn.setEnabled(false);
        deleteRowBtn.addActionListener(e -> {
            int r = ledgerTable.getSelectedRow();
            if (r >= 0) {
                LedgerAccount acc = ledgerTableModel.getAccountAt(r);
                if (acc == null) {
                    return;
                }
                int impactedEntries = journal.countLogicalEntriesForAccount(acc.getId());
                int impactedLines = journal.countTransactionLinesForAccount(acc.getId());
                String warning = "";
                if (impactedLines > 0) {
                    warning = String.format("\n\nWarning: This will also delete %d journal line(s) across %d logical transaction(s).",
                            impactedLines, impactedEntries);
                }
                int choice = JOptionPane.showConfirmDialog(this,
                        "Delete account '" + acc.getName() + "' (ID " + acc.getId() + ")?" + warning,
                        "Confirm", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    if (impactedLines > 0) {
                        journal.deleteTransactionsForAccount(acc.getId());
                    }
                    ledger.removeAccount(acc.getId());
                    ledger.updateFromJournal(journal);
                    refreshLedger();
                    if (journalRefreshAction != null) {
                        journalRefreshAction.run();
                    }
                }
            }
        });
        
        // view graph button
        viewGraphBtn = new JButton("View Graph");
        styleButton(viewGraphBtn, BUTTON_PRIMARY);
        viewGraphBtn.setEnabled(false);
        viewGraphBtn.addActionListener(e -> showGraphForSelected());

        bottomPanel.add(viewGraphBtn);
        bottomPanel.add(deleteRowBtn);
        add(bottomPanel, BorderLayout.SOUTH);
        
        // selection listener to enable/disable delete and graph buttons
        ledgerTable.getSelectionModel().addListSelectionListener(e -> {
            updateRowActionButtonsVisibility();
        });

        refreshLedger();
    }

    // updates journal refresh action for this component
    public void setJournalRefreshAction(Runnable journalRefreshAction) {
        this.journalRefreshAction = journalRefreshAction;
    }

    // creates top panel used by this screen
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel title = new JLabel("LEDGER");
        title.setFont(new Font("Georgia", Font.BOLD, 24));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        
        panel.add(title, BorderLayout.CENTER);
        
        return panel;
    }

    // creates account creation panel used by this screen.
    private JPanel createAccountCreationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // account ID field
        panel.add(new JLabel("Account ID:"));
        panel.add(Box.createVerticalStrut(5));
        idField = new JTextField(15);
        panel.add(idField);
        panel.add(Box.createVerticalStrut(10));

        // account name field
        panel.add(new JLabel("Account Name:"));
        panel.add(Box.createVerticalStrut(5));
        nameField = new JTextField(15);
        panel.add(nameField);
        panel.add(Box.createVerticalStrut(10));

        // account type dropdown
        panel.add(new JLabel("Account Type:"));
        panel.add(Box.createVerticalStrut(5));
        String[] types = {"Asset", "Liability", "Equity", "Revenue", "Expense"};
        typeCombo = new JComboBox<>(types);
        typeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        panel.add(typeCombo);
        panel.add(Box.createVerticalStrut(10));

        // preset ID field based on type selection
        typeCombo.addActionListener(e -> {
            String type = (String) typeCombo.getSelectedItem();
            String prefix = "";
            if ("Asset".equals(type)) prefix = "1";
            else if ("Liability".equals(type)) prefix = "2";
            else if ("Equity".equals(type)) prefix = "3";
            else if ("Revenue".equals(type)) prefix = "4";
            else if ("Expense".equals(type)) prefix = "5";
            idField.setText(prefix);
            updateSubtypeOptions();
        });

        // set default as asset
        idField.setText("1");

        panel.add(new JLabel("Subtype (optional):"));
        panel.add(Box.createVerticalStrut(5));
        subtypeCombo = new JComboBox<>(new String[]{"None"});
        subtypeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        panel.add(subtypeCombo);
        panel.add(Box.createVerticalStrut(20));

        // initialize subtype options
        updateSubtypeOptions();

        // add account button
        JButton addButton = new JButton("Create Account");
        addButton.setFont(new Font("Arial", Font.BOLD, 12));
        styleButton(addButton, BUTTON_CREATE);
        addButton.addActionListener(e -> addNewAccount());
        JPanel createButtonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        createButtonRow.setOpaque(false);
        createButtonRow.add(addButton);
        panel.add(createButtonRow);
        
        panel.add(Box.createVerticalStrut(20));

        // account numbering guide
        JTextArea guideArea = new JTextArea();
        guideArea.setEditable(false);
        guideArea.setBackground(PANEL_NEUTRAL);
        guideArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        guideArea.setText(
            "Account Numbering Guide:\n" +
            "100-199: Assets\n" +
            "200-299: Liabilities\n" +
            "300-399: Equity\n" +
            "400-499: Revenue\n" +
            "500-599: Expenses\n\n" +
            "Examples:\n" +
            "101: Cash (Asset)\n" +
            "201: AP (Liability)\n" +
            "401: Sales (Revenue)\n" +
            "501: Rent (Expense)"
        );
        guideArea.setBorder(BorderFactory.createEtchedBorder());
        
        JScrollPane guideScroll = new JScrollPane(guideArea);
        guideScroll.setPreferredSize(new Dimension(300, 200));
        panel.add(guideScroll);

        return panel;
    }

    // creates bottom panel with many buttons used by this screen
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        JButton refreshBtn = new JButton("Refresh Ledger");
        styleButton(refreshBtn, BUTTON_SUCCESS);
        refreshBtn.addActionListener(e -> refreshLedger());

        JButton trialBalanceBtn = new JButton("View Trial Balance");
        styleButton(trialBalanceBtn, BUTTON_ACCENT);
        trialBalanceBtn.addActionListener(e -> showTrialBalance());

        groupedViewToggle = new JToggleButton("Grouped View");
        styleToggleButton(groupedViewToggle);
        groupedViewToggle.addActionListener(e -> {
            if (groupedViewToggle.isSelected()) {
                ledgerViewLayout.show(ledgerViewPanel, "GROUPED");
            } else {
                ledgerViewLayout.show(ledgerViewPanel, "FLAT");
            }
            updateRowActionButtonsVisibility();
        });

        panel.add(refreshBtn);
        panel.add(trialBalanceBtn);
        panel.add(groupedViewToggle);
        return panel;
    }

    // handles style button behavior
    private void styleButton(JButton button, Color background) {
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
    }

    // handles style toggle button behavior
    private void styleToggleButton(JToggleButton button) {
        button.setBackground(BUTTON_PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
    }

    // checks whether grouped view enabled is true
    private boolean isGroupedViewEnabled() {
        return groupedViewToggle != null && groupedViewToggle.isSelected();
    }

    // updates row action buttons visibility for consistency
    private void updateRowActionButtonsVisibility() {
        if (deleteRowBtn == null || viewGraphBtn == null) {
            return;
        }
        if (isGroupedViewEnabled()) {
            deleteRowBtn.setEnabled(false);
            viewGraphBtn.setEnabled(false);
            return;
        }
        int row = ledgerTable.getSelectedRow();
        LedgerAccount selected = row >= 0 ? ledgerTableModel.getAccountAt(row) : null;
        boolean enabled = selected != null;
        deleteRowBtn.setEnabled(enabled);
        viewGraphBtn.setEnabled(enabled);
    }

    // shows trial balance
    private void showTrialBalance() {
        ledger.updateFromJournal(journal);

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));

        area.append("TRIAL BALANCE\n");
        area.append("==================\n\n");

        double totalDebits = 0;
        double totalCredits = 0;

        for (String name : ledger.getAccountNames()) {
            LedgerAccount acct = ledger.getAccountByName(name);
            if (acct == null || acct.getId() <= 0) {
                continue;
            }

            double balance = acct.getBalance();
            String normalSide = acct.getNormalSide();
            area.append(String.format("%-20s : %10.2f (%s)\n", name, balance, normalSide));

            if ("debit".equals(normalSide)) {
                totalDebits += balance;
            } else {
                totalCredits += balance;
            }
        }

        area.append("\n");
        area.append("==================\n");
        area.append(String.format("Total Debits  : %.2f\n", totalDebits));
        area.append(String.format("Total Credits : %.2f\n", totalCredits));
        if (Math.abs(totalDebits - totalCredits) < 0.01) {
            area.append("Trial Balance is in balance!\n");
        } else {
            area.append("Trial Balance is NOT in balance!\n");
        }

        JScrollPane pane = new JScrollPane(area);
        pane.setPreferredSize(new Dimension(520, 360));
        JOptionPane.showMessageDialog(this, pane, "Trial Balance", JOptionPane.INFORMATION_MESSAGE);
    }

    // adds new account to the current state.
    private void addNewAccount() {
        try {
            int id = Integer.parseInt(idField.getText().trim());
            String name = nameField.getText().trim();
            String type = (String) typeCombo.getSelectedItem();

            // valides account name and ID
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this,"Please enter an account name!","Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (id < 100 || id > 999) {
                JOptionPane.showMessageDialog(this,"Account ID must be between 100 and 999!\nFollow the accounting numbering guide.","Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // try to add account
            String subtype = (String) subtypeCombo.getSelectedItem();
            LedgerAccount parent = null;
            if (subtype != null && !subtype.equals("None")) {
                // find or create parent abstract account under the type
                LedgerAccount typeCategory = ledger.getOrCreateAbstractAccount(type, type, null);
                parent = ledger.getOrCreateAbstractAccount(subtype, type, typeCategory);
            }

            LedgerAccount newAccount = new LedgerAccount(id, name, type, parent);
            ledger.addAccount(newAccount);

            refreshLedger();
            clearFields();

            JOptionPane.showMessageDialog(this,"Account created successfully!","Account Created",JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid numeric ID!",
                "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this,
                e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // clears current input or stored data
    private void clearFields() {
        idField.setText("");
        nameField.setText("");
        typeCombo.setSelectedIndex(0);
    }

    // refreshes ledger based on current data
    public void refreshLedger() {
        List<LedgerAccount> accounts = ledger.getAllAccountsSorted();

        // normal view where all accounts in strict ID order
        ledgerTableModel.setAccounts(accounts);

        // grouped view where separate tables are made for each account type and subtype
        LinkedHashMap<String, LinkedHashMap<String, List<LedgerAccount>>> grouped = buildGroupedAccounts(accounts);
        rebuildGroupedLedgerPanel(grouped);

        if (accounts.isEmpty()) {
            summaryLabel.setText("No accounts in ledger");
        } else {
            long assets = accounts.stream().filter(a -> a.getType().equals("Asset")).count();
            long liabilities = accounts.stream().filter(a -> a.getType().equals("Liability")).count();
            long equity = accounts.stream().filter(a -> a.getType().equals("Equity")).count();
            long revenue = accounts.stream().filter(a -> a.getType().equals("Revenue")).count();
            long expenses = accounts.stream().filter(a -> a.getType().equals("Expense")).count();

            double currentAssetsTotal = ledger.getRecursiveTotalByName("Current Asset");
            double fixedAssetsTotal = ledger.getRecursiveTotalByName("Fixed Asset");
            double currentLiabilitiesTotal = ledger.getRecursiveTotalByName("Current Liability");
            double longTermLiabilitiesTotal = ledger.getRecursiveTotalByName("Long-Term Liability");

            summaryLabel.setText(String.format(
                "<html>Total: %d  Assets:%d Liabilities:%d Equity:%d Revenue:%d Expenses:%d"
                + "<br/>Current Assets:%s  Fixed Assets:%s  Current Liabilities:%s  Long-Term Liabilities:%s</html>",
                accounts.size(), assets, liabilities, equity, revenue, expenses,
                formatMoney(currentAssetsTotal),
                formatMoney(fixedAssetsTotal),
                formatMoney(currentLiabilitiesTotal),
                formatMoney(longTermLiabilitiesTotal)
            ));
        }
        updateRowActionButtonsVisibility();
    }

    // formats money for display
    private String formatMoney(double value) {
        if (value < 0) {
            return String.format("-$%.2f", Math.abs(value));
        }
        return String.format("$%.2f", value);
    }

    // builds grouped accounts for output or rendering
    private LinkedHashMap<String, LinkedHashMap<String, List<LedgerAccount>>> buildGroupedAccounts(List<LedgerAccount> accounts) {
        LinkedHashMap<String, LinkedHashMap<String, List<LedgerAccount>>> grouped = new LinkedHashMap<>();
        grouped.put("Assets", new LinkedHashMap<>());
        grouped.put("Liabilities", new LinkedHashMap<>());
        grouped.put("Equity", new LinkedHashMap<>());
        grouped.put("Revenue", new LinkedHashMap<>());
        grouped.put("Expenses", new LinkedHashMap<>());

        grouped.get("Assets").put("Current Assets", new ArrayList<>());
        grouped.get("Assets").put("Fixed Assets", new ArrayList<>());
        grouped.get("Liabilities").put("Current Liabilities", new ArrayList<>());
        grouped.get("Liabilities").put("Long-Term Liabilities", new ArrayList<>());
        grouped.get("Equity").put("Equity", new ArrayList<>());
        grouped.get("Revenue").put("Revenue", new ArrayList<>());
        grouped.get("Expenses").put("Operating Expenses", new ArrayList<>());
        grouped.get("Expenses").put("Other Expenses", new ArrayList<>());

        for (LedgerAccount account : accounts) {
            if (account == null || account.getId() <= 0) {
                continue;
            }

            String type = account.getType();
            String parentName = account.getParent() != null ? account.getParent().getName() : "";

            if ("Asset".equals(type)) {
                if ("Fixed Asset".equalsIgnoreCase(parentName)) {
                    grouped.get("Assets").get("Fixed Assets").add(account);
                } else {
                    grouped.get("Assets").get("Current Assets").add(account);
                }
            } else if ("Liability".equals(type)) {
                if ("Long-Term Liability".equalsIgnoreCase(parentName)) {
                    grouped.get("Liabilities").get("Long-Term Liabilities").add(account);
                } else {
                    grouped.get("Liabilities").get("Current Liabilities").add(account);
                }
            } else if ("Equity".equals(type)) {
                grouped.get("Equity").get("Equity").add(account);
            } else if ("Revenue".equals(type)) {
                grouped.get("Revenue").get("Revenue").add(account);
            } else if ("Expense".equals(type)) {
                if (parentName.toLowerCase().contains("other")) {
                    grouped.get("Expenses").get("Other Expenses").add(account);
                } else {
                    grouped.get("Expenses").get("Operating Expenses").add(account);
                }
            }
        }
        return grouped;
    }

    // handles rebuild grouped ledger panel behavior
    private void rebuildGroupedLedgerPanel(LinkedHashMap<String, LinkedHashMap<String, List<LedgerAccount>>> grouped) {
        groupedLedgerPanel.removeAll();

        for (Map.Entry<String, LinkedHashMap<String, List<LedgerAccount>>> heading : grouped.entrySet()) {
            JPanel headingPanel = new JPanel();
            headingPanel.setLayout(new BoxLayout(headingPanel, BoxLayout.Y_AXIS));
            headingPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),heading.getKey(),TitledBorder.LEFT,TitledBorder.TOP,new Font("Arial", Font.BOLD, 14)));

            for (Map.Entry<String, List<LedgerAccount>> subtype : heading.getValue().entrySet()) {
                JPanel subtypePanel = new JPanel(new BorderLayout());
                subtypePanel.setBorder(BorderFactory.createTitledBorder(subtype.getKey()));

                List<LedgerAccount> sortedSubtype = mergeSortById(subtype.getValue());
                JTable subtypeTable = createSubtypeTable(sortedSubtype);
                JScrollPane subScroll = new JScrollPane(subtypeTable);
                subScroll.setPreferredSize(new Dimension(500, sortedSubtype.isEmpty() ? 45 : 90));
                subtypePanel.add(subScroll, BorderLayout.CENTER);
                headingPanel.add(subtypePanel);
            }

            groupedLedgerPanel.add(headingPanel);
            groupedLedgerPanel.add(Box.createVerticalStrut(8));
        }

        groupedLedgerPanel.revalidate();
        groupedLedgerPanel.repaint();
    }

    // creates subtype table used in grouped view
    private JTable createSubtypeTable(List<LedgerAccount> accounts) {
        LedgerTableModel model = new LedgerTableModel();
        if (accounts.isEmpty()) {
            List<LedgerAccount> placeholder = new ArrayList<>();
            placeholder.add(new LedgerAccount("(none)", "DISPLAY"));
            model.setAccounts(placeholder);
        } else {
            model.setAccounts(accounts);
        }

        JTable table = new JTable(model);
        table.setEnabled(false);
        table.setRowSelectionAllowed(false);
        table.setFocusable(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);

        // hide type column in grouped view for cleaner display
        table.removeColumn(table.getColumnModel().getColumn(2));
        return table;
    }

    // mergesort algorithm for sorting accounts by ID
    private List<LedgerAccount> mergeSortById(List<LedgerAccount> input) {
        if (input == null || input.size() <= 1) {
            return input == null ? new ArrayList<>() : new ArrayList<>(input);
        }

        int mid = input.size() / 2;
        List<LedgerAccount> left = mergeSortById(input.subList(0, mid));
        List<LedgerAccount> right = mergeSortById(input.subList(mid, input.size()));
        return mergeById(left, right);
    }

    // merges ledger account lists by ID
    private List<LedgerAccount> mergeById(List<LedgerAccount> left, List<LedgerAccount> right) {
        List<LedgerAccount> merged = new ArrayList<>();
        int i = 0;
        int j = 0;

        while (i < left.size() && j < right.size()) {
            if (left.get(i).getId() <= right.get(j).getId()) {
                merged.add(left.get(i++));
            } else {
                merged.add(right.get(j++));
            }
        }
        while (i < left.size()) {
            merged.add(left.get(i++));
        }
        while (j < right.size()) {
            merged.add(right.get(j++));
        }
        return merged;
    }

    // updates subtype options for consistency
    private void updateSubtypeOptions() {
        String selectedType = (String) typeCombo.getSelectedItem();
        String[] options = subtypeMap.getOrDefault(selectedType, new String[]{"None"});
        subtypeCombo.setModel(new DefaultComboBoxModel<>(options));
    }

    // checks for potential errors and then displays graph panel
    private void showGraphForSelected() {
        int row = ledgerTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an account first.", "No Account Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LedgerAccount acc = ledgerTableModel.getAccountAt(row);
        if (acc == null) {
            JOptionPane.showMessageDialog(this, "Please select an actual account row.", "No Account Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<Ledger.BalancePoint> history = ledger.getBalanceHistory(acc.getId(), journal);
        if (history.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No transactions have affected this account.", "No Data", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        AccountGraphDialog dlg = new AccountGraphDialog((JFrame) SwingUtilities.getWindowAncestor(this), acc.getName(), history);
        dlg.setVisible(true);
    }

    // accountgraphdialog manages this part of the accounting application.
    private static class AccountGraphDialog extends JDialog {
        // constructor for accountgraphdialog setup.
        public AccountGraphDialog(JFrame owner, String accountName, List<Ledger.BalancePoint> history) {
            super(owner, "Balance Graph for " + accountName, true);
            setSize(500, 400);
            setLocationRelativeTo(owner);
            add(new GraphPanel(history));
        }
    }

}
