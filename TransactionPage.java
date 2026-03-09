import java.awt.*;
import java.awt.event.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableCellRenderer;

// class for creating a transaction page
public class TransactionPage extends JPanel {
    private static final Color BUTTON_DANGER = new Color(198, 40, 40);
    private static final Color BUTTON_PRIMARY = new Color(2, 119, 189);
    private static final Color BUTTON_SUCCESS = new Color(0, 150, 0);
    private static final Color BUTTON_NEUTRAL = new Color(220, 224, 228);
    private static final Color BUTTON_INFO = new Color(21, 101, 192);
    private static final Color BUTTON_CALENDAR_DAY = new Color(136, 146, 156);
    private static final Color BUTTON_CALENDAR_TODAY = new Color(102, 187, 106);
    private static final Color PANEL_NEUTRAL = new Color(245, 245, 245);

    private final Journal journal;
    private final Ledger ledger;
    private final LedgerPage ledgerPage;
    private final JTable journalTable;
    private final TransactionTableModel tableModel;
    private JPanel inputPanel;
    private final JScrollPane rightScroll;
    private JButton dateButton;
    private JTextField amountField;
    private JTextArea descriptionField;
    private JCheckBox hstPurchaseCheckbox;
    private JCheckBox hstSaleCheckbox;
    private JCheckBox remittanceCheckbox;
    private JTextField debitSearchField;
    private JTextField creditSearchField;
    private JTextField hstDebitField;
    private JTextField hstCreditField;
    private JComponent tutorialScroll;
    private LocalDate selectedDate = LocalDate.now();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // selected accounts
    private LedgerAccount selectedDebitAccount;
    private LedgerAccount selectedCreditAccount;

    // constructor for a transaction page
    public TransactionPage(Journal journal, Ledger ledger, LedgerPage ledgerPage) {
        this.journal = journal;
        this.ledger = ledger;
        this.ledgerPage = ledgerPage;
        
        setLayout(new BorderLayout(10,10));
        setBorder(new EmptyBorder(10,10,10,10));

        // header section includes title and an initial hidden tutorial
        JPanel header = new JPanel(new BorderLayout());
        header.add(createTopPanel(), BorderLayout.NORTH);
        header.add(createTutorialPanel(), BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // center panel for journal table
        this.tableModel = new TransactionTableModel(ledger);
        this.journalTable = new JTable(this.tableModel);
        journalTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        journalTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        TextAreaRenderer groupRenderer = new TextAreaRenderer(this.tableModel);
        journalTable.setDefaultRenderer(String.class, groupRenderer);
        journalTable.setDefaultRenderer(Object.class, groupRenderer);

        // create columns for date, particulars, account ID, debit accounts, and credit accounts
        journalTable.getColumnModel().getColumn(0).setPreferredWidth(95);
        journalTable.getColumnModel().getColumn(1).setPreferredWidth(360);
        journalTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        journalTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        journalTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        journalTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane scrollPane = new JScrollPane(journalTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Journal Entries (sorted by date)",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14)
        ));
        add(scrollPane, BorderLayout.CENTER);

        // bottom panel for delete button
        JButton deleteRowBtn = new JButton("Delete Selected");
        styleButton(deleteRowBtn, BUTTON_DANGER);
        deleteRowBtn.setEnabled(false);
        deleteRowBtn.addActionListener(e -> {
            int viewRow = journalTable.getSelectedRow();
            int journalIndex = tableModel.getJournalIndexForRow(viewRow);
            if (journalIndex >= 0) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "Delete selected transaction?","Confirm",JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    if (journal.deleteTransactionByIndex(journalIndex)) {
                        refreshJournal();
                        ledger.updateFromJournal(journal);
                        if (ledgerPage != null) ledgerPage.refreshLedger();
                    }
                }
            }
        });
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        wrapper.add(deleteRowBtn);
        add(wrapper, BorderLayout.SOUTH);

        final boolean[] syncingSelection = new boolean[] { false };
        journalTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            if (syncingSelection[0]) {
                return;
            }

            int selectedRow = journalTable.getSelectedRow();
            boolean has = selectedRow >= 0;
            deleteRowBtn.setEnabled(has);
            if (!has) {
                return;
            }

            int key = tableModel.getSelectionKeyForRow(selectedRow);
            if (key == Integer.MIN_VALUE) {
                return;
            }

            syncingSelection[0] = true;
            try {
                ListSelectionModel sm = journalTable.getSelectionModel();
                sm.setValueIsAdjusting(true);
                sm.clearSelection();
                for (int row = 0; row < journalTable.getRowCount(); row++) {
                    if (tableModel.getSelectionKeyForRow(row) == key) {
                        sm.addSelectionInterval(row, row);
                    }
                }
            } finally {
                journalTable.getSelectionModel().setValueIsAdjusting(false);
                syncingSelection[0] = false;
            }
        });
        deleteRowBtn.setEnabled(false);

        // right panel for inputting new transactions
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(350, 600));
        createInputPanel();
        this.rightScroll = new JScrollPane(inputPanel);
        rightScroll.setPreferredSize(new Dimension(350,600));
        rightScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rightScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "New Transaction",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14)
        ));
        rightPanel.add(rightScroll, BorderLayout.CENTER);

        add(rightPanel, BorderLayout.EAST);

        refreshJournal();
    }

    // creates input panel used by this screen
    private void createInputPanel() {
        inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));

        // title handled by scroll pane border
        inputPanel.setBorder(new EmptyBorder(10,10,10,10));

        // date selection
        dateButton = new JButton(formatter.format(selectedDate));
        styleButton(dateButton, BUTTON_PRIMARY);
        dateButton.addActionListener(e -> showCalendar());
        
        // description field
        descriptionField = new JTextArea(3, 20);
        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descriptionField);
        descScroll.setPreferredSize(new Dimension(300, 80));
        amountField = new JTextField(15);
        amountField.setMaximumSize(new Dimension(300, 30));
        hstPurchaseCheckbox = new JCheckBox("Add 13% HST (purchase)");
        hstSaleCheckbox = new JCheckBox("Add 13% HST (sale)");
        remittanceCheckbox = new JCheckBox("Add remittance entry");
        hstPurchaseCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
        hstSaleCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
        remittanceCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);

        // mutual exclusivity
        hstPurchaseCheckbox.addItemListener(e -> {
            if (hstPurchaseCheckbox.isSelected()) hstSaleCheckbox.setSelected(false);
            if (hstPurchaseCheckbox.isSelected()) remittanceCheckbox.setSelected(false);
            updateHstFields();
        });
        hstSaleCheckbox.addItemListener(e -> {
            if (hstSaleCheckbox.isSelected()) hstPurchaseCheckbox.setSelected(false);
            if (hstSaleCheckbox.isSelected()) remittanceCheckbox.setSelected(false);
            updateHstFields();
        });
        remittanceCheckbox.addItemListener(e -> {
            if (remittanceCheckbox.isSelected()) {
                hstPurchaseCheckbox.setSelected(false);
                hstSaleCheckbox.setSelected(false);
            }
            updateHstFields();
        });
        
        // debit and credit account fields with dropdown autofill
        debitSearchField = new JTextField(15);
        debitSearchField.setMaximumSize(new Dimension(300, 30));
        setupAccountPopup(debitSearchField, true);
        creditSearchField = new JTextField(15);
        creditSearchField.setMaximumSize(new Dimension(300, 30));
        setupAccountPopup(creditSearchField, false);

        // uneditable HST account fields
        hstDebitField = new JTextField(15);
        hstDebitField.setMaximumSize(new Dimension(300,30));
        hstDebitField.setEditable(false);
        hstDebitField.setVisible(false);
        hstDebitField.setBackground(PANEL_NEUTRAL);
        hstDebitField.setToolTipText(null);
        hstCreditField = new JTextField(15);
        hstCreditField.setMaximumSize(new Dimension(300,30));
        hstCreditField.setEditable(false);
        hstCreditField.setVisible(false);
        hstCreditField.setBackground(PANEL_NEUTRAL);
        hstCreditField.setToolTipText(null);

        // add components
        inputPanel.add(createLabeledField("Date:", dateButton));
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(createLabeledField("Description:", descScroll));
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(createLabeledField("Amount ($):", amountField));
        inputPanel.add(hstPurchaseCheckbox);
        inputPanel.add(hstSaleCheckbox);
        inputPanel.add(remittanceCheckbox);
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(createLabeledField("Debit Account:", debitSearchField));
        inputPanel.add(createLabeledField("", hstDebitField));
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(createLabeledField("Credit Account:", creditSearchField));
        inputPanel.add(createLabeledField("", hstCreditField));
        inputPanel.add(Box.createVerticalStrut(20));
        inputPanel.add(Box.createVerticalStrut(10));

        // add buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton addButton = new JButton("Record Transaction");
        addButton.setFont(new Font("Arial", Font.BOLD, 12));
        styleButton(addButton, BUTTON_SUCCESS);
        addButton.addActionListener(e -> addTransaction());
        
        JButton clearButton = new JButton("Clear Form");
        styleNeutralButton(clearButton);
        clearButton.addActionListener(e -> clearForm());
        
        buttonPanel.add(addButton);
        buttonPanel.add(clearButton);
        
        inputPanel.add(buttonPanel);
    }


    // updates selected labels for consistency
    private void updateSelectedLabels() {
        updateHstFields();
    }

    // creates labeled field used by this screen
    private JPanel createLabeledField(String label, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    // checks whether an account is an HST account
    private boolean isHstAccount(LedgerAccount account) {
        if (account == null) return false;
        String n = account.getName();
        return "HST Payable".equalsIgnoreCase(n) || "HST Recoverable".equalsIgnoreCase(n);
    }

    // handles account text behavior for transactionpage.
    private String accountText(LedgerAccount account) {
        return account != null ? (account.getId() + " - " + account.getName()) : "";
    }

    // updates HST fields for consistency.
    private void updateHstFields() {
        if (hstPurchaseCheckbox == null) return;
        boolean remittance = remittanceCheckbox.isSelected();
        boolean show = hstPurchaseCheckbox.isSelected() || hstSaleCheckbox.isSelected() || remittance;

        debitSearchField.setEditable(!remittance);
        creditSearchField.setEditable(!remittance);
        amountField.setEditable(!remittance);

        hstDebitField.setVisible(false);
        hstCreditField.setVisible(false);
        if (show) {
            LedgerAccount hstRec = ledger.getAccountByName("HST Recoverable");
            LedgerAccount hstPay = ledger.getAccountByName("HST Payable");

            if (remittance) {
                // preview where cash goes based on current balances
                ledger.updateFromJournal(journal);
                LedgerAccount cash = ledger.getAccountByName("Cash");
                double payableBal = hstPay != null ? Math.max(0, hstPay.getBalance()) : 0;
                double receivableBal = hstRec != null ? Math.max(0, hstRec.getBalance()) : 0;
                double diff = payableBal - receivableBal;

                if (hstPay != null) {
                    selectedDebitAccount = hstPay;
                }
                if (hstRec != null) {
                    selectedCreditAccount = hstRec;
                }

                // base remittance entry as debit HST Payable, credit HST Recoverable
                debitSearchField.setText(accountText(hstPay));
                creditSearchField.setText(accountText(hstRec));

                // extra helper line shows only balancing cash side, if needed
                hstDebitField.setText("");
                hstCreditField.setText("");
                if (cash != null && Math.abs(diff) > 0.0001) {
                    if (diff > 0) {
                        // cash appears on credit side if tax is owed
                        hstCreditField.setText(accountText(cash));
                    } else {
                        // cash appears on debit side if tax can be redeemed
                        hstDebitField.setText(accountText(cash));
                    }
                }
                hstDebitField.setVisible(true);
                hstCreditField.setVisible(true);
            } else if (hstPurchaseCheckbox.isSelected()) {
                if (hstRec != null) {
                    hstDebitField.setText(accountText(hstRec));
                } else {
                    hstDebitField.setText("(no HST Recoverable)");
                }
                hstDebitField.setVisible(true);
            } else if (hstSaleCheckbox.isSelected()) {
                if (hstPay != null) {
                    hstCreditField.setText(accountText(hstPay));
                } else {
                    hstCreditField.setText("(no HST Payable)");
                }
                hstCreditField.setVisible(true);
            }
        } else {
            debitSearchField.setEditable(true);
            creditSearchField.setEditable(true);
            amountField.setEditable(true);

            // remove tax-account autofill from main fields when remittance is toggled off
            if (!hstPurchaseCheckbox.isSelected() && !hstSaleCheckbox.isSelected()) {
                if (isHstAccount(selectedDebitAccount)) {
                    selectedDebitAccount = null;
                    debitSearchField.setText("");
                }
                if (isHstAccount(selectedCreditAccount)) {
                    selectedCreditAccount = null;
                    creditSearchField.setText("");
                }
            }
            hstDebitField.setText("");
            hstCreditField.setText("");
        }
        inputPanel.revalidate();
        inputPanel.repaint();
    }

    // attach popup with filtered account list to a text field
    private void setupAccountPopup(JTextField field, boolean isDebit) {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> resultsList = new JList<>(listModel);
        resultsList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // prevent the list from taking focus
        resultsList.setFocusable(false);
        JScrollPane scroll = new JScrollPane(resultsList);
        scroll.setPreferredSize(new Dimension(300, 120));

        JPopupMenu popup = new JPopupMenu();
        popup.setFocusable(false);
        popup.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        popup.add(scroll);

        // use document listener for immediate updates
        javax.swing.event.DocumentListener docListener = new javax.swing.event.DocumentListener() {
            // updates state for consistency
            private void update() {
                String prefix = field.getText().trim();
                listModel.clear();
                List<LedgerAccount> matches;
                if (prefix.isEmpty()) {
                    matches = ledger.getAllAccountsSorted();
                } else {
                    matches = ledger.findAccountsByPrefix(prefix);
                }
                for (LedgerAccount acc : matches) {
                    if (isHstAccount(acc)) {
                        continue;
                    }
                    if (isDebit && selectedCreditAccount != null && selectedCreditAccount.getId() == acc.getId())
                        continue;
                    if (!isDebit && selectedDebitAccount != null && selectedDebitAccount.getId() == acc.getId())
                        continue;
                    listModel.addElement(String.format("%d | %-20s | %-8s",acc.getId(), acc.getName(), acc.getType()));
                }
                if (!listModel.isEmpty()) {
                    popup.show(field, 0, field.getHeight());
                } else {
                    popup.setVisible(false);
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        };
        field.getDocument().addDocumentListener(docListener);

        resultsList.addMouseListener(new MouseAdapter() {
            @Override
            // handles mouse clicked behavior on transaction page
            public void mouseClicked(MouseEvent e) {
                int idx = resultsList.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    String sel = listModel.get(idx);
                    int id = Integer.parseInt(sel.split("\\|")[0].trim());
                    LedgerAccount acc = ledger.getAccountById(id);
                    if (acc != null) {
                        if (isDebit) selectedDebitAccount = acc;
                        else selectedCreditAccount = acc;
                        field.setText(acc.getId() + " - " + acc.getName());
                        updateHstFields();
                    }
                    popup.setVisible(false);
                }
            }
        });

        field.addFocusListener(new FocusAdapter() {
            @Override
            // handles focus lost behavior
            public void focusLost(FocusEvent e) {
                Component opp = e.getOppositeComponent();
                if (opp != null && SwingUtilities.isDescendingFrom(opp, popup)) {
                    return;
                }
                popup.setVisible(false);
            }
            @Override
            // handles focus gained behavior
            public void focusGained(FocusEvent e) {
                docListener.insertUpdate(null);
            }
        });
    }

    // adds transaction to the current state
    private void addTransaction() {
        try {
            String description = descriptionField.getText().trim();

            // remittance is an auto-filled transaction type
            if (remittanceCheckbox.isSelected()) {
                ledger.updateFromJournal(journal);
                LedgerAccount hstPay = ledger.getAccountByName("HST Payable");
                LedgerAccount hstRec = ledger.getAccountByName("HST Recoverable");
                LedgerAccount cash = ledger.getAccountByName("Cash");

                if (hstPay == null || hstRec == null || cash == null) {
                    JOptionPane.showMessageDialog(this, "HST Payable, HST Recoverable, and Cash accounts are required for remittance.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double payableBal = Math.max(0, hstPay.getBalance());
                double receivableBal = Math.max(0, hstRec.getBalance());
                if (payableBal == 0 && receivableBal == 0) {
                    JOptionPane.showMessageDialog(this, "No HST balance to remit.", "Info", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                int gid = Transaction.allocateGroupId();
                String remDesc = description.isEmpty() ? "HST remittance" : description;

                // common offset between payable and recoverable
                double common = Math.min(payableBal, receivableBal);
                if (common > 0) {
                    journal.addTransaction(new Transaction(selectedDate,remDesc,hstPay.getId(),hstRec.getId(),common,gid));
                }

                // balance with cash on whichever side is needed for the remainder
                double diff = payableBal - receivableBal;
                if (Math.abs(diff) > 0.0001) {
                    if (diff > 0) {
                        journal.addTransaction(new Transaction(selectedDate, remDesc, hstPay.getId(), cash.getId(), diff, gid));
                    } else {
                        journal.addTransaction(new Transaction(selectedDate, remDesc, cash.getId(), hstRec.getId(), -diff, gid));
                    }
                }

                JOptionPane.showMessageDialog(this, "HST remittance recorded.", "Success", JOptionPane.INFORMATION_MESSAGE);

                clearForm();
                refreshJournal();
                ledger.updateFromJournal(journal);
                if (ledgerPage != null) {
                    ledgerPage.refreshLedger();
                }
                return;
            }

            double amount = Double.parseDouble(amountField.getText());

            if (selectedDebitAccount == null || selectedCreditAccount == null) {
                JOptionPane.showMessageDialog(this, "Please select both debit and credit accounts using the search!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (selectedDebitAccount.getId() == selectedCreditAccount.getId()) {
                JOptionPane.showMessageDialog(this, "Debit and Credit accounts cannot be the same!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (description.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a description!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // determine group ID if any HST checkbox is selected
            int gid = 0;
            if (hstPurchaseCheckbox.isSelected() || hstSaleCheckbox.isSelected()) {
                gid = Transaction.allocateGroupId();
            }
            Transaction transaction = new Transaction(selectedDate, description, selectedDebitAccount.getId(), selectedCreditAccount.getId(), amount, gid);

            journal.addTransaction(transaction);
            if (rightScroll != null) {
                rightScroll.setVisible(true);
                inputPanel.setVisible(true);
            }

            double taxAmount = 0;
            LedgerAccount hstRec = ledger.getAccountByName("HST Recoverable");
            LedgerAccount hstPay = ledger.getAccountByName("HST Payable");
            if (hstPurchaseCheckbox.isSelected()) {
                taxAmount = amount * 0.13;
                // debit HST Recoverable, credit same credit account for purchase
                if (hstRec != null) {
                    journal.addTransaction(new Transaction(selectedDate, "HST on purchase", hstRec.getId(), selectedCreditAccount.getId(), taxAmount, gid));
                }
            } else if (hstSaleCheckbox.isSelected()) {
                taxAmount = amount * 0.13;
                // debit same debit account, credit HST Payable for sale
                if (hstPay != null) {
                    journal.addTransaction(new Transaction(selectedDate, "HST on sale", selectedDebitAccount.getId(), hstPay.getId(), taxAmount, gid));
                }
            }

            //  confirmation message
            StringBuilder msg = new StringBuilder("Transaction recorded!\n\n");
            msg.append("Date: ").append(selectedDate).append("\n");
            msg.append("Description: ").append(description).append("\n");
            msg.append("Debit: ").append(selectedDebitAccount.getName())
               .append(" (ID: ").append(selectedDebitAccount.getId()).append(")\n");
            msg.append("Credit: ").append(selectedCreditAccount.getName())
               .append(" (ID: ").append(selectedCreditAccount.getId()).append(")\n");
            msg.append(String.format("Base amount: $%.2f\n", amount));
            if (hstPurchaseCheckbox.isSelected() || hstSaleCheckbox.isSelected()) {
                msg.append(String.format("HST (13%%): $%.2f\n", taxAmount));
                msg.append(String.format("Total affected: $%.2f\n", amount + taxAmount));
            }

            JOptionPane.showMessageDialog(this, msg.toString(), "Success", JOptionPane.INFORMATION_MESSAGE);

            clearForm();
            refreshJournal();
            // update ledger balances and refresh ledger page
            ledger.updateFromJournal(journal);
            if (ledgerPage != null) {
                ledgerPage.refreshLedger();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid amount!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // clears current input or stored data
    private void clearForm() {
        descriptionField.setText("");
        amountField.setText("");
        if (hstPurchaseCheckbox != null) hstPurchaseCheckbox.setSelected(false);
        if (hstSaleCheckbox != null) hstSaleCheckbox.setSelected(false);
        if (remittanceCheckbox != null) remittanceCheckbox.setSelected(false);
        if (debitSearchField != null) debitSearchField.setText("");
        if (creditSearchField != null) creditSearchField.setText("");
        selectedDate = LocalDate.now();
        dateButton.setText(formatter.format(selectedDate));
        selectedDebitAccount = null;
        selectedCreditAccount = null;
        updateHstFields();
    }

    // refreshes journal based on current data
    private void refreshJournal() {
        List<Transaction> transactions = journal.getAllTransactions();
        if (tableModel != null) {
            tableModel.setTransactions(transactions);
            // ensure row heights accommodate wrapped text
            adjustRowHeights();
        }
    }

    // refreshes journal display based on current data
    public void refreshJournalDisplay() {
        refreshJournal();
    }

    // class for a renderer to wrap text in table cells
    private static class TextAreaRenderer extends JTextArea implements TableCellRenderer {
        private final TransactionTableModel model;

        // constructor for a text area renderer
        public TextAreaRenderer(TransactionTableModel model) {
            this.model = model;
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
        }
        @Override
        // returns table cell renderer component
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            setSize(table.getColumnModel().getColumn(column).getWidth(), Short.MAX_VALUE);
            int height = getPreferredSize().height;
            if (table.getRowHeight(row) != height) {
                table.setRowHeight(row, height);
            }

            int selectedRow = table.getSelectedRow();
            boolean groupSelected = false;
            if (selectedRow >= 0) {
                int selectedKey = model.getSelectionKeyForRow(selectedRow);
                int rowKey = model.getSelectionKeyForRow(row);
                groupSelected = (selectedKey != Integer.MIN_VALUE && selectedKey == rowKey);
            }

            if (isSelected || groupSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            return this;
        }
    }

    // creates top panel used by this screen
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("JOURNAL");
        title.setFont(new Font("Georgia", Font.BOLD, 24));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        JButton helpBtn = new JButton("Help");
        styleButton(helpBtn, BUTTON_INFO);
        helpBtn.addActionListener(e -> {
            if (tutorialScroll != null) {
                tutorialScroll.setVisible(!tutorialScroll.isVisible());
                revalidate();
            }
        });
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.add(helpBtn);
        panel.add(left, BorderLayout.WEST);
        panel.add(title, BorderLayout.CENTER);
        return panel;
    }

    // creates tutorial panel used by this screen
    private JComponent createTutorialPanel() {
        JTextArea tutorial = new JTextArea();
        tutorial.setEditable(false);
        tutorial.setLineWrap(true);
        tutorial.setWrapStyleWord(true);
        tutorial.setBackground(PANEL_NEUTRAL);
        tutorial.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tutorial.setText(
            "Debits and Credits:\n" +
            "- Assets increase with debits, decrease with credits.\n" +
            "- Liabilities increase with credits, decrease with debits.\n" +
            "- Revenue accounts are credited to record income.\n" +
            "- Expense accounts are debited to record costs.\n\n" +
            "When recording a transaction:\n" +
            "- Enter the debit account (what receives value).\n" +
            "- Enter the credit account (what gives value).\n" +
            "- Enter the amount (value transferred).\n\n" +
            "Examples:\n" +
            "- Purchase supplies for cash: debit Supplies (Expense), credit Cash (Asset).\n" +
            "- Receive revenue: debit Cash (Asset), credit Service Revenue (Revenue).\n\n" +
            "Check the HST box to automatically add 13% tax using appropriate HST accounts.\n");
        JScrollPane scroll = new JScrollPane(tutorial);
        scroll.setPreferredSize(new Dimension(0, 120));
        tutorialScroll = scroll;
        tutorialScroll.setVisible(false); // hidden until help requested
        return tutorialScroll;
    }

    // adjusts row heights for text
    private void adjustRowHeights() {
        for (int row = 0; row < journalTable.getRowCount(); row++) {
            int maxHeight = journalTable.getRowHeight();
            for (int col = 0; col < journalTable.getColumnCount(); col++) {
                TableCellRenderer renderer = journalTable.getCellRenderer(row, col);
                Component comp = renderer.getTableCellRendererComponent(journalTable,
                        journalTable.getValueAt(row, col), false, false, row, col);
                maxHeight = Math.max(maxHeight, comp.getPreferredSize().height);
            }
            if (journalTable.getRowHeight(row) != maxHeight) {
                journalTable.setRowHeight(row, maxHeight);
            }
        }
    }

    // shows calendar.
    private void showCalendar() {
        CalendarPopup popup = new CalendarPopup();
        Dimension pref = popup.getPreferredSize();
        int popupWidth = Math.max(280, dateButton.getWidth());
        popup.setPreferredSize(new Dimension(popupWidth, pref.height));
        popup.setPopupSize(popupWidth, pref.height);
        popup.show(dateButton, 0, dateButton.getHeight());
    }

    // class for a popup calendar
    private class CalendarPopup extends JPopupMenu {
        private YearMonth currentYearMonth;
        private LocalDate selDate;
        private JPanel calendarPanel;
        private JLabel monthLabel;

        // constructor for a popup calendar
        CalendarPopup() {
            currentYearMonth = YearMonth.now();
            selDate = null;
            setLayout(new BorderLayout());
            initUI();
            refreshCalendar();
        }

        // initializes UI for popup calendar
        private void initUI() {
            JPanel headerPanel = new JPanel(new BorderLayout());
            JButton prevButton = new JButton("<");
            JButton nextButton = new JButton(">");
            styleNeutralButton(prevButton);
            styleNeutralButton(nextButton);

            monthLabel = new JLabel("", SwingConstants.CENTER);
            monthLabel.setFont(new Font("Arial", Font.BOLD, 12));

            prevButton.addActionListener(e -> {
                currentYearMonth = currentYearMonth.minusMonths(1);
                refreshCalendar();
            });
            nextButton.addActionListener(e -> {
                currentYearMonth = currentYearMonth.plusMonths(1);
                refreshCalendar();
            });

            headerPanel.add(prevButton, BorderLayout.WEST);
            headerPanel.add(monthLabel, BorderLayout.CENTER);
            headerPanel.add(nextButton, BorderLayout.EAST);

            calendarPanel = new JPanel(new GridLayout(0, 7, 4, 4));
            add(headerPanel, BorderLayout.NORTH);
            add(calendarPanel, BorderLayout.CENTER);
        }

        // refreshes calendar based on current data
        private void refreshCalendar() {
            calendarPanel.removeAll();
            monthLabel.setText(currentYearMonth.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()) + " " + currentYearMonth.getYear());
            for (DayOfWeek day : DayOfWeek.values()) {
                JLabel lbl = new JLabel(day.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()), SwingConstants.CENTER);
                lbl.setFont(new Font("Arial", Font.BOLD, 10));
                calendarPanel.add(lbl);
            }
            LocalDate first = currentYearMonth.atDay(1);
            int startDay = first.getDayOfWeek().getValue() % 7;
            for (int i = 1; i < startDay; i++) {
                calendarPanel.add(new JLabel(""));
            }
            int days = currentYearMonth.lengthOfMonth();
            for (int d = 1; d <= days; d++) {
                LocalDate date = currentYearMonth.atDay(d);
                JButton btn = new JButton(String.valueOf(d));
                btn.setPreferredSize(new Dimension(30, 30));
                btn.setFont(new Font("Arial", Font.BOLD, 10));
                btn.setMargin(new Insets(0, 0, 0, 0));
                styleButton(btn, BUTTON_CALENDAR_DAY);
                if (date.equals(LocalDate.now())) {
                    styleButton(btn, BUTTON_CALENDAR_TODAY);
                }
                btn.addActionListener(e -> {
                    selDate = date;
                    selectedDate = selDate;
                    dateButton.setText(formatter.format(selectedDate));
                    setVisible(false);
                });
                calendarPanel.add(btn);
            }
            calendarPanel.revalidate();
            calendarPanel.repaint();
        }
    }

    // handles style button behavior
    private void styleButton(JButton button, Color background) {
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
    }

    private void styleNeutralButton(JButton button) {
        button.setBackground(BUTTON_NEUTRAL);
        button.setForeground(new Color(70, 70, 70));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
    }
}
