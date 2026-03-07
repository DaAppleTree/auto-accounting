import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class LedgerPage extends JPanel {
    private Ledger ledger;
    private Journal journal;
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

    // predefined subtype options for each account type
    private static final java.util.Map<String, String[]> subtypeMap = new java.util.HashMap<>();
    static {
        subtypeMap.put("Asset", new String[]{"None", "Current Asset", "Fixed Asset"});
        subtypeMap.put("Liability", new String[]{"None", "Current Liability", "Long-Term Liability"});
        subtypeMap.put("Equity", new String[]{"None", "Owner's Equity"});
        subtypeMap.put("Revenue", new String[]{"None", "Service Revenue", "Sales Revenue", "Cost of Goods Sold"});
        subtypeMap.put("Expense", new String[]{"None", "Rent Expense", "Utilities Expense", "COGS"});
    }

    public LedgerPage(Ledger ledger, Journal journal) {
        this.ledger = ledger;
        this.journal = journal;
        
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top: Title
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // Center: switchable ledger display (flat table or grouped tables)
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

        // Right: Account creation panel
        accountCreationPanel = createAccountCreationPanel();
        JScrollPane rightScroll = new JScrollPane(accountCreationPanel);
        rightScroll.setPreferredSize(new Dimension(350, 0));
        rightScroll.setBorder(BorderFactory.createTitledBorder(
            "Create New Account"
        ));
        add(rightScroll, BorderLayout.EAST);

        // Bottom: Control buttons and delete section
        JPanel bottomPanel = createBottomPanel();
        
        // delete selected row button (hidden until an account is selected)
        deleteRowBtn = new JButton("Delete Selected");
        styleButton(deleteRowBtn, new Color(198, 40, 40));
        deleteRowBtn.setVisible(false);
        deleteRowBtn.addActionListener(e -> {
            int r = ledgerTable.getSelectedRow();
            if (r >= 0) {
                LedgerAccount acc = ledgerTableModel.getAccountAt(r);
                if (acc == null) {
                    return;
                }
                int choice = JOptionPane.showConfirmDialog(this,
                        "Delete account '" + acc.getName() + "' (ID " + acc.getId() + ")?",
                        "Confirm", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    ledger.removeAccount(acc.getId());
                    refreshLedger();
                }
            }
        });
        
        // view graph button
        viewGraphBtn = new JButton("View Graph");
        styleButton(viewGraphBtn, new Color(2, 119, 189));
        viewGraphBtn.setVisible(false);
        viewGraphBtn.addActionListener(e -> showGraphForSelected());
        
        JPanel combinedBottom = new JPanel(new BorderLayout());
        combinedBottom.add(bottomPanel, BorderLayout.NORTH);
        JPanel rowButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        rowButtons.add(viewGraphBtn);
        rowButtons.add(deleteRowBtn);
        combinedBottom.add(rowButtons, BorderLayout.SOUTH);
        add(combinedBottom, BorderLayout.SOUTH);
        
        // selection listener to show/hide delete and graph buttons
        ledgerTable.getSelectionModel().addListSelectionListener(e -> {
            int row = ledgerTable.getSelectedRow();
            LedgerAccount selected = row >= 0 ? ledgerTableModel.getAccountAt(row) : null;
            boolean sel = selected != null && !isGroupedViewEnabled();
            deleteRowBtn.setVisible(sel);
            viewGraphBtn.setVisible(sel);
        });

        refreshLedger();
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel title = new JLabel("LEDGER MANAGEMENT SYSTEM");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        
        panel.add(title, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createAccountCreationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));



        // Account ID field
        panel.add(new JLabel("Account ID:"));
        panel.add(Box.createVerticalStrut(5));
        idField = new JTextField(15);
        panel.add(idField);
        panel.add(Box.createVerticalStrut(10));

        // Account Name field
        panel.add(new JLabel("Account Name:"));
        panel.add(Box.createVerticalStrut(5));
        nameField = new JTextField(15);
        panel.add(nameField);
        panel.add(Box.createVerticalStrut(10));

        // Account Type dropdown
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
        // set initial preset for Asset (default)
        idField.setText("1");

        panel.add(new JLabel("Subtype (optional):"));
        panel.add(Box.createVerticalStrut(5));
        subtypeCombo = new JComboBox<>(new String[]{"None"});
        subtypeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        panel.add(subtypeCombo);
        panel.add(Box.createVerticalStrut(20));

        // Initialize subtype options
        updateSubtypeOptions();

        // Add Account button
        JButton addButton = new JButton("Create Account");
        addButton.setFont(new Font("Arial", Font.BOLD, 12));
        styleButton(addButton, new Color(0, 100, 200));
        addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addButton.addActionListener(e -> addNewAccount());
        panel.add(addButton);
        
        panel.add(Box.createVerticalStrut(20));

        // Account numbering guide
        JTextArea guideArea = new JTextArea();
        guideArea.setEditable(false);
        guideArea.setBackground(new Color(245, 245, 245));
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

    private JPanel createBottomPanel() {
        // bottom panel now only holds refresh; graph button is provided alongside delete
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        JButton refreshBtn = new JButton("Refresh Ledger");
        styleButton(refreshBtn, new Color(56, 142, 60));
        refreshBtn.addActionListener(e -> refreshLedger());

        JButton trialBalanceBtn = new JButton("View Trial Balance");
        styleButton(trialBalanceBtn, new Color(123, 31, 162));
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

    private void styleButton(JButton button, Color background) {
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
    }

    private void styleToggleButton(JToggleButton button) {
        button.setBackground(new Color(2, 119, 189));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
    }

    private boolean isGroupedViewEnabled() {
        return groupedViewToggle != null && groupedViewToggle.isSelected();
    }

    private void updateRowActionButtonsVisibility() {
        if (deleteRowBtn == null || viewGraphBtn == null) {
            return;
        }
        if (isGroupedViewEnabled()) {
            deleteRowBtn.setVisible(false);
            viewGraphBtn.setVisible(false);
            return;
        }
        int row = ledgerTable.getSelectedRow();
        LedgerAccount selected = row >= 0 ? ledgerTableModel.getAccountAt(row) : null;
        boolean visible = selected != null;
        deleteRowBtn.setVisible(visible);
        viewGraphBtn.setVisible(visible);
    }

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
            LedgerAccount acct = ledger.getAccount(name);
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

    private void addNewAccount() {
        try {
            int id = Integer.parseInt(idField.getText().trim());
            String name = nameField.getText().trim();
            String type = (String) typeCombo.getSelectedItem();

            // Validation
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Please enter an account name!",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (id < 100 || id > 999) {
                JOptionPane.showMessageDialog(this,
                    "Account ID must be between 100 and 999!\n" +
                    "Follow the accounting numbering guide.",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Try to add account
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

            JOptionPane.showMessageDialog(this,
                "Account created successfully!",
                "Account Created",
                JOptionPane.INFORMATION_MESSAGE
            );

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

    private void clearFields() {
        idField.setText("");
        nameField.setText("");
        typeCombo.setSelectedIndex(0);
    }

    public void refreshLedger() {
        List<LedgerAccount> accounts = ledger.getAllAccountsSorted();

        // flat mode: show all accounts in strict ID order
        ledgerTableModel.setAccounts(accounts);

        // grouped mode: separate tables for each heading/subheading
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

    private String formatMoney(double value) {
        if (value < 0) {
            return String.format("-$%.2f", Math.abs(value));
        }
        return String.format("$%.2f", value);
    }

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

    private void rebuildGroupedLedgerPanel(LinkedHashMap<String, LinkedHashMap<String, List<LedgerAccount>>> grouped) {
        groupedLedgerPanel.removeAll();

        for (Map.Entry<String, LinkedHashMap<String, List<LedgerAccount>>> heading : grouped.entrySet()) {
            JPanel headingPanel = new JPanel();
            headingPanel.setLayout(new BoxLayout(headingPanel, BoxLayout.Y_AXIS));
            headingPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                heading.getKey(),
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14)
            ));

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
        // hide Type column in grouped view for cleaner display
        table.removeColumn(table.getColumnModel().getColumn(2));
        return table;
    }

    // merge-sort accounts by ID to explicitly incorporate merge of sorted lists.
    private List<LedgerAccount> mergeSortById(List<LedgerAccount> input) {
        if (input == null || input.size() <= 1) {
            return input == null ? new ArrayList<>() : new ArrayList<>(input);
        }

        int mid = input.size() / 2;
        List<LedgerAccount> left = mergeSortById(input.subList(0, mid));
        List<LedgerAccount> right = mergeSortById(input.subList(mid, input.size()));
        return mergeById(left, right);
    }

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

    private String truncate(String str, int length) {
        if (str.length() <= length) return str;
        return str.substring(0, length - 3) + "...";
    }

    private void updateSubtypeOptions() {
        String selectedType = (String) typeCombo.getSelectedItem();
        String[] options = subtypeMap.getOrDefault(selectedType, new String[]{"None"});
        subtypeCombo.setModel(new DefaultComboBoxModel<>(options));
    }

    // ---------- graph support ----------
    private void showGraphForSelected() {
        int row = ledgerTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an account first.",
                    "No Account Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LedgerAccount acc = ledgerTableModel.getAccountAt(row);
        if (acc == null) {
            JOptionPane.showMessageDialog(this, "Please select an actual account row.",
                    "No Account Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<Ledger.BalancePoint> history = ledger.getBalanceHistory(acc.getId(), journal);
        if (history.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No transactions have affected this account.",
                    "No Data", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        AccountGraphDialog dlg = new AccountGraphDialog((JFrame) SwingUtilities.getWindowAncestor(this), acc.getName(), history);
        dlg.setVisible(true);
    }

    private static class AccountGraphDialog extends JDialog {
        public AccountGraphDialog(JFrame owner, String accountName, List<Ledger.BalancePoint> history) {
            super(owner, "Balance Graph for " + accountName, true);
            setSize(500, 400);
            setLocationRelativeTo(owner);
            add(new GraphPanel(history));
        }
    }

    private static class GraphPanel extends JPanel {
        private final List<Ledger.BalancePoint> history;
        private long minEpoch;
        private long maxEpoch;
        private double minBal;
        private double maxBal;

        public GraphPanel(List<Ledger.BalancePoint> history) {
            this.history = history;
            computeBounds();
        }

        private void computeBounds() {
            if (history.isEmpty()) return;
            minEpoch = Long.MAX_VALUE;
            maxEpoch = Long.MIN_VALUE;
            minBal = Double.MAX_VALUE;
            maxBal = Double.MIN_VALUE;
            for (Ledger.BalancePoint bp : history) {
                long e = bp.date.toEpochDay();
                minEpoch = Math.min(minEpoch, e);
                maxEpoch = Math.max(maxEpoch, e);
                minBal = Math.min(minBal, bp.balance);
                maxBal = Math.max(maxBal, bp.balance);
            }
            double pad = (maxBal - minBal) * 0.1;
            if (pad == 0) pad = 1;
            minBal -= pad;
            maxBal += pad;
            if (minEpoch == maxEpoch) {
                minEpoch -= 1;
                maxEpoch += 1;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (history.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g;
            // nicer rendering
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            setBackground(Color.WHITE);
            // remember original font for later
            Font origFont = g2.getFont();
            int w = getWidth();
            int h = getHeight();
            int margin = 40;
            // draw axes
            g2.setColor(Color.BLACK);
            g2.drawLine(margin, h - margin, w - margin, h - margin);
            g2.drawLine(margin, margin, margin, h - margin);
            // x-axis label at right end of axis
            String xLabel = "Date";
            int xW = g2.getFontMetrics().stringWidth(xLabel);
            g2.drawString(xLabel, w - margin - xW/2, h - 10);
            // y-axis label rotated and moved left of tick labels
            String yLabel = "Balance";
            AffineTransform orig = g2.getTransform();
            // translate to left of margin and center vertically
            int labelX = margin / 2;
            int labelY = h / 2;
            g2.translate(labelX, labelY);
            g2.rotate(-Math.PI / 2);
            g2.drawString(yLabel, 0, 0);
            g2.setTransform(orig);

            // draw ticks on y-axis
            int yTicks = 5;
            g2.setColor(Color.DARK_GRAY);
            Font tickFont = origFont.deriveFont(10f);
            g2.setFont(tickFont);
            for (int i = 0; i <= yTicks; i++) {
                double val = minBal + (maxBal - minBal) * i / yTicks;
                int y = h - margin - (int) ((val - minBal) / (maxBal - minBal) * (h - 2 * margin));
                g2.drawLine(margin - 5, y, margin, y);
                String label = String.format("%.2f", val);
                g2.drawString(label, 5, y + 5);
            }
            // ensure max value drawn at very top as a numeric label as well (redundant with loop)
            String topVal = String.format("%.2f", maxBal);
            g2.drawString(topVal, 5, margin - 5);
            // draw ticks on x-axis - only a few labels evenly spaced
            int desiredLabels = 4;
            int count = history.size();
            int step = Math.max(1, count / (desiredLabels - 1));
            for (int idx = 0; idx < count; idx += step) {
                Ledger.BalancePoint bp = history.get(idx);
                double frac = (bp.date.toEpochDay() - minEpoch) / (double) (maxEpoch - minEpoch);
                int x = margin + (int) (frac * (w - 2 * margin));
                g2.drawLine(x, h - margin, x, h - margin + 5);
                String label = bp.date.toString();
                int strW = g2.getFontMetrics().stringWidth(label);
                g2.drawString(label, x - strW / 2, h - margin + 20);
            }
            // intermediate ticks with no labels
            for (int idx = 0; idx < count; idx++) {
                if (idx % step != 0) {
                    Ledger.BalancePoint bp = history.get(idx);
                    double frac = (bp.date.toEpochDay() - minEpoch) / (double) (maxEpoch - minEpoch);
                    int x = margin + (int) (frac * (w - 2 * margin));
                    g2.drawLine(x, h - margin, x, h - margin + 3);
                }
            }
            g2.setFont(origFont);

            int pxPrev = 0, pyPrev = 0;
            boolean first = true;
            g2.setColor(Color.BLUE);
            for (Ledger.BalancePoint bp : history) {
                double fracX = (bp.date.toEpochDay() - minEpoch) / (double) (maxEpoch - minEpoch);
                double fracY = (bp.balance - minBal) / (maxBal - minBal);
                int px = margin + (int) (fracX * (w - 2 * margin));
                int py = h - margin - (int) (fracY * (h - 2 * margin));
                if (first) {
                    first = false;
                } else {
                    g2.drawLine(pxPrev, pyPrev, px, py);
                }
                g2.fillOval(px - 3, py - 3, 6, 6);
                pxPrev = px;
                pyPrev = py;
            }
        }
    }

}