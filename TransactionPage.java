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

public class TransactionPage extends JPanel {
    private Journal journal;
    private Ledger ledger;
    private LedgerPage ledgerPage; // reference to update ledger UI
    // private JTextArea journalArea; // replaced by JTable below
    private JTable journalTable;
    private TransactionTableModel tableModel;
    private JPanel inputPanel;
    private JScrollPane rightScroll;
    // toggleButton removed; form always visible
    // private JButton toggleButton;
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
    private JList<String> debitResultsList;
    private JList<String> creditResultsList;
    private DefaultListModel<String> debitListModel;
    private DefaultListModel<String> creditListModel;
    private JComponent tutorialScroll; // holds tutorial pane
    private LocalDate selectedDate = LocalDate.now();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // Selected accounts
    private LedgerAccount selectedDebitAccount;
    private LedgerAccount selectedCreditAccount;

    public TransactionPage(Journal journal, Ledger ledger, LedgerPage ledgerPage) {
        this.journal = journal;
        this.ledger = ledger;
        this.ledgerPage = ledgerPage;
        
        setLayout(new BorderLayout(10,10));
        setBorder(new EmptyBorder(10,10,10,10));
        // header section includes title and tutorial (hidden initially)
        JPanel header = new JPanel(new BorderLayout());
        header.add(createTopPanel(), BorderLayout.NORTH);
        header.add(createTutorialPanel(), BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);
        // tutorial panel accessible later via field

        // CENTER - journal table
        TransactionTableModel tableModel = new TransactionTableModel(ledger);
        JTable journalTable = new JTable(tableModel);
        journalTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        journalTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        TextAreaRenderer groupRenderer = new TextAreaRenderer(tableModel);
        journalTable.setDefaultRenderer(String.class, groupRenderer);
        journalTable.setDefaultRenderer(Object.class, groupRenderer);

        // column sizing: Date | Particulars | Account # | Debit | Credit
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

        // bottom panel for delete
        // bottom wrapper for delete button (hidden until selection)
        JButton deleteRowBtn = new JButton("Delete Selected");
        styleButton(deleteRowBtn, new Color(198, 40, 40));
        // always visible; enable/disable toggled by selection listener
        deleteRowBtn.setVisible(true);
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
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(deleteRowBtn, BorderLayout.SOUTH);
        add(wrapper, BorderLayout.SOUTH);
        // always show delete button, and when one row is clicked select the full
        // transaction block (all rows with the same selection key).
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

        // store reference for later updating
        this.journalTable = journalTable;
        this.tableModel = tableModel;

        // RIGHT SIDE - Input Panel with Binary Search
        JPanel rightPanel = new JPanel(new BorderLayout());
        // match ledger page ratio by fixing width to 350
        rightPanel.setPreferredSize(new Dimension(350, 600));
        
        // no toggle button; form always visible
        createInputPanel();
        rightScroll = new JScrollPane(inputPanel);
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

    private void createInputPanel() {
        inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        // title handled by scroll pane border
        inputPanel.setBorder(new EmptyBorder(10,10,10,10));

        // Date selection
        dateButton = new JButton(formatter.format(selectedDate));
        styleButton(dateButton, new Color(2, 119, 189));
        dateButton.addActionListener(e -> showCalendar());
        
        // Description field
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
        hstPurchaseCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        hstSaleCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        remittanceCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
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
        
        // Debit & credit account fields with dropdown autofill
        debitSearchField = new JTextField(15);
        debitSearchField.setMaximumSize(new Dimension(300, 30));
        setupAccountPopup(debitSearchField, true);
        creditSearchField = new JTextField(15);
        creditSearchField.setMaximumSize(new Dimension(300, 30));
        setupAccountPopup(creditSearchField, false);
        // HST account fields (auto-populated, uneditable)
        hstDebitField = new JTextField(15);
        hstDebitField.setMaximumSize(new Dimension(300,30));
        hstDebitField.setEditable(false);
        hstDebitField.setVisible(false);
        hstDebitField.setBackground(new Color(245, 245, 245));
        hstDebitField.setToolTipText(null);
        hstCreditField = new JTextField(15);
        hstCreditField.setMaximumSize(new Dimension(300,30));
        hstCreditField.setEditable(false);
        hstCreditField.setVisible(false);
        hstCreditField.setBackground(new Color(245, 245, 245));
        hstCreditField.setToolTipText(null);

        // Add components
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

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton addButton = new JButton("Record Transaction");
        addButton.setFont(new Font("Arial", Font.BOLD, 12));
        styleButton(addButton, new Color(0, 150, 0));
        addButton.addActionListener(e -> addTransaction());
        
        JButton clearButton = new JButton("Clear Form");
        styleButton(clearButton, new Color(96, 125, 139));
        clearButton.addActionListener(e -> clearForm());
        
        buttonPanel.add(addButton);
        buttonPanel.add(clearButton);
        
        inputPanel.add(buttonPanel);
    }


    private void updateSelectedLabels() {
        // Account text fields already display selected accounts; keep this hook
        // only to refresh dependent HST/remittance UI.
        updateHstFields();
    }

    private JPanel createLabeledField(String label, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private boolean isHstAccount(LedgerAccount account) {
        if (account == null) return false;
        String n = account.getName();
        return "HST Payable".equalsIgnoreCase(n)
            || "HST Receivable".equalsIgnoreCase(n)
            || "HST Recoverable".equalsIgnoreCase(n);
    }

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
            if (hstRec == null) {
                hstRec = ledger.getAccountByName("HST Receivable");
            }
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

                // base remittance entry is always:
                //   Debit HST Payable, Credit HST Recoverable/Receivable
                debitSearchField.setText(hstPay != null ? (hstPay.getId() + " - " + hstPay.getName()) : "");
                creditSearchField.setText(hstRec != null ? (hstRec.getId() + " - " + hstRec.getName()) : "");

                // extra helper line shows only balancing cash side, if needed
                hstDebitField.setText("");
                hstCreditField.setText("");
                if (cash != null && Math.abs(diff) > 0.0001) {
                    if (diff > 0) {
                        // owe tax: cash appears on credit side
                        hstCreditField.setText(cash.getId() + " - " + cash.getName());
                    } else {
                        // refund due: cash appears on debit side
                        hstDebitField.setText(cash.getId() + " - " + cash.getName());
                    }
                }
                hstDebitField.setVisible(true);
                hstCreditField.setVisible(true);
            } else if (hstPurchaseCheckbox.isSelected()) {
                if (hstRec != null) {
                    hstDebitField.setText(hstRec.getId() + " - " + hstRec.getName());
                } else {
                    hstDebitField.setText("(no HST Recoverable)");
                }
                hstDebitField.setVisible(true);
            } else if (hstSaleCheckbox.isSelected()) {
                if (hstPay != null) {
                    hstCreditField.setText(hstPay.getId() + " - " + hstPay.getName());
                } else {
                    hstCreditField.setText("(no HST Payable)");
                }
                hstCreditField.setVisible(true);
            }
        } else {
            debitSearchField.setEditable(true);
            creditSearchField.setEditable(true);
            amountField.setEditable(true);

            // remittance toggled off: remove tax-account autofill from main fields
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
        // prevent the list from taking focus; keep focus on the field so we don't
        // immediately hide the popup when the list is clicked.
        resultsList.setFocusable(false);
        JScrollPane scroll = new JScrollPane(resultsList);
        scroll.setPreferredSize(new Dimension(300, 120));

        JPopupMenu popup = new JPopupMenu();
        popup.setFocusable(false);
        popup.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        popup.add(scroll);

        // use document listener for immediate updates
        javax.swing.event.DocumentListener docListener = new javax.swing.event.DocumentListener() {
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
                    String nm = acc.getName();
                    if ("HST Payable".equalsIgnoreCase(nm) ||
                        "HST Receivable".equalsIgnoreCase(nm) ||
                        "HST Recoverable".equalsIgnoreCase(nm)) {
                        continue;
                    }
                    if (isDebit && selectedCreditAccount != null && selectedCreditAccount.getId() == acc.getId())
                        continue;
                    if (!isDebit && selectedDebitAccount != null && selectedDebitAccount.getId() == acc.getId())
                        continue;
                    // only show id, name and type – omit balance/amount
                    listModel.addElement(String.format("%d | %-20s | %-8s", 
                            acc.getId(), acc.getName(), acc.getType()));
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
                        updateSelectedLabels();
                    }
                    popup.setVisible(false);
                }
            }
        });

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // if focus is moving into the popup itself (e.g. user clicked a
                // result), don't hide it immediately.  keep the list visible so the
                // mouse click can be processed.
                Component opp = e.getOppositeComponent();
                if (opp != null && SwingUtilities.isDescendingFrom(opp, popup)) {
                    return; // ignore the loss
                }
                popup.setVisible(false);
            }
            @Override
            public void focusGained(FocusEvent e) {
                // show full list immediately on focus
                docListener.insertUpdate(null);
            }
        });
    }

    private void addTransaction() {
        try {
            String description = descriptionField.getText().trim();

            // remittance is a dedicated auto-filled transaction type
            if (remittanceCheckbox.isSelected()) {
                ledger.updateFromJournal(journal);
                LedgerAccount hstPay = ledger.getAccountByName("HST Payable");
                LedgerAccount hstRec = ledger.getAccountByName("HST Recoverable");
                if (hstRec == null) {
                    hstRec = ledger.getAccountByName("HST Receivable");
                }
                LedgerAccount cash = ledger.getAccountByName("Cash");

                if (hstPay == null || hstRec == null || cash == null) {
                    JOptionPane.showMessageDialog(this,
                        "HST Payable, HST Recoverable, and Cash accounts are required for remittance.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double payableBal = Math.max(0, hstPay.getBalance());
                double receivableBal = Math.max(0, hstRec.getBalance());
                if (payableBal == 0 && receivableBal == 0) {
                    JOptionPane.showMessageDialog(this,
                        "No HST balance to remit.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                int gid = Transaction.allocateGroupId();
                String remDesc = description.isEmpty() ? "HST remittance" : description;

                // common offset between payable and recoverable
                double common = Math.min(payableBal, receivableBal);
                if (common > 0) {
                    journal.addTransaction(new Transaction(
                        selectedDate,
                        remDesc,
                        hstPay.getId(),
                        hstRec.getId(),
                        common,
                        gid
                    ));
                }

                // balance with cash on whichever side is needed for the remainder
                double diff = payableBal - receivableBal;
                if (Math.abs(diff) > 0.0001) {
                    if (diff > 0) {
                        journal.addTransaction(new Transaction(
                            selectedDate,
                            remDesc,
                            hstPay.getId(),
                            cash.getId(),
                            diff,
                            gid
                        ));
                    } else {
                        journal.addTransaction(new Transaction(
                            selectedDate,
                            remDesc,
                            cash.getId(),
                            hstRec.getId(),
                            -diff,
                            gid
                        ));
                    }
                }

                JOptionPane.showMessageDialog(this,
                    "HST remittance recorded.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

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
                JOptionPane.showMessageDialog(this,
                    "Please select both debit and credit accounts using the search!",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (selectedDebitAccount.getId() == selectedCreditAccount.getId()) {
                JOptionPane.showMessageDialog(this,
                    "Debit and Credit accounts cannot be the same!",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (description.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Please enter a description!",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // determine group id if any HST checkbox is selected
            int gid = 0;
            if (hstPurchaseCheckbox.isSelected() || hstSaleCheckbox.isSelected()) {
                gid = Transaction.allocateGroupId();
            }
            Transaction transaction = new Transaction(
                selectedDate, description,
                selectedDebitAccount.getId(), selectedCreditAccount.getId(),
                amount,
                gid
            );

            journal.addTransaction(transaction);
            // in case layout hiccup hid the input section earlier, ensure it remains visible
            if (rightScroll != null) {
                rightScroll.setVisible(true);
                inputPanel.setVisible(true);
            }

            double taxAmount = 0;
            boolean taxed = false;
            LedgerAccount hstRec = ledger.getAccountByName("HST Recoverable");
            if (hstRec == null) {
                hstRec = ledger.getAccountByName("HST Receivable");
            }
            LedgerAccount hstPay = ledger.getAccountByName("HST Payable");
            if (hstPurchaseCheckbox.isSelected()) {
                taxed = true;
                taxAmount = amount * 0.13;
                // purchase: debit HST Recoverable, credit same credit account
                if (hstRec != null) {
                    journal.addTransaction(new Transaction(
                        selectedDate,
                        "HST on purchase",
                        hstRec.getId(),
                        selectedCreditAccount.getId(),
                        taxAmount,
                        gid
                    ));
                }
            } else if (hstSaleCheckbox.isSelected()) {
                taxed = true;
                taxAmount = amount * 0.13;
                // sale: debit same debit account, credit HST Payable
                if (hstPay != null) {
                    journal.addTransaction(new Transaction(
                        selectedDate,
                        "HST on sale",
                        selectedDebitAccount.getId(),
                        hstPay.getId(),
                        taxAmount,
                        gid
                    ));
                }
            }
            // build confirmation message with HST breakdown if needed
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
            JOptionPane.showMessageDialog(this,
                "Please enter a valid amount!",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        descriptionField.setText("");
        amountField.setText("");
        if (hstPurchaseCheckbox != null) hstPurchaseCheckbox.setSelected(false);
        if (hstSaleCheckbox != null) hstSaleCheckbox.setSelected(false);
        if (remittanceCheckbox != null) remittanceCheckbox.setSelected(false);
        if (debitSearchField != null) debitSearchField.setText("");
        if (creditSearchField != null) creditSearchField.setText("");
        if (debitListModel != null) debitListModel.clear();
        if (creditListModel != null) creditListModel.clear();
        selectedDate = LocalDate.now();
        dateButton.setText(formatter.format(selectedDate));
        selectedDebitAccount = null;
        selectedCreditAccount = null;
        updateSelectedLabels();
    }

    private void refreshJournal() {
        List<Transaction> transactions = journal.getAllTransactions();
        if (tableModel != null) {
            tableModel.setTransactions(transactions);
            // ensure row heights accommodate wrapped text
            adjustRowHeights();
        }
    }

    private String truncate(String str, int length) {
        if (str.length() <= length) return str;
        return str.substring(0, length - 3) + "...";
    }

    // renderer to wrap text in table cells
    private static class TextAreaRenderer extends JTextArea implements TableCellRenderer {
        private final TransactionTableModel model;

        public TextAreaRenderer(TransactionTableModel model) {
            this.model = model;
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
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

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("JOURNAL / TRANSACTIONS");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        JButton helpBtn = new JButton("Help");
        styleButton(helpBtn, new Color(21, 101, 192));
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

    private JComponent createTutorialPanel() {
        JTextArea tutorial = new JTextArea();
        tutorial.setEditable(false);
        tutorial.setLineWrap(true);
        tutorial.setWrapStyleWord(true);
        tutorial.setBackground(new Color(245,245,245));
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
            "- Amount represents the value transferred.\n\n" +
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

    private void showCalendar() {
        CalendarPopup popup = new CalendarPopup();
        // force the popup to be no wider than the button that triggered it
        int width = dateButton.getWidth();
        if (width > 0) {
            Dimension pref = popup.getPreferredSize();
            popup.setPreferredSize(new Dimension(width, pref.height));
            popup.setPopupSize(width, pref.height);
        }
        popup.show(dateButton, 0, dateButton.getHeight());
    }

    // lightweight popup calendar
    private class CalendarPopup extends JPopupMenu {
        private YearMonth currentYearMonth;
        private LocalDate selDate;
        private JPanel calendarPanel;
        private JLabel monthLabel;

        CalendarPopup() {
            currentYearMonth = YearMonth.now();
            selDate = null;
            setLayout(new BorderLayout());
            initUI();
            refreshCalendar();
        }

        private void initUI() {
            JPanel headerPanel = new JPanel(new BorderLayout());
            JButton prevButton = new JButton("<");
            JButton nextButton = new JButton(">");
            styleButton(prevButton, new Color(96, 125, 139));
            styleButton(nextButton, new Color(96, 125, 139));

            monthLabel = new JLabel("", SwingConstants.CENTER);
            monthLabel.setFont(new Font("Arial", Font.BOLD, 14));

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

            calendarPanel = new JPanel(new GridLayout(0, 7));
            add(headerPanel, BorderLayout.NORTH);
            add(calendarPanel, BorderLayout.CENTER);
        }

        private void refreshCalendar() {
            calendarPanel.removeAll();
            monthLabel.setText(currentYearMonth.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()) + " " + currentYearMonth.getYear());
            for (DayOfWeek day : DayOfWeek.values()) {
                JLabel lbl = new JLabel(day.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()), SwingConstants.CENTER);
                lbl.setFont(new Font("Arial", Font.BOLD, 12));
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
                // square buttons and smaller font to fit two-digit numbers
                btn.setPreferredSize(new Dimension(30, 30));
                btn.setFont(new Font("Arial", Font.PLAIN, 10));
                styleButton(btn, new Color(84, 110, 122));
                if (date.equals(LocalDate.now())) {
                    styleButton(btn, new Color(46, 125, 50));
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

    private void styleButton(JButton button, Color background) {
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
    }
}