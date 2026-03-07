import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.table.AbstractTableModel;

public class TransactionTableModel extends AbstractTableModel {
    private static class JournalRow {
        String date;
        String particulars;
        String accountNumber;
        String debit;
        String credit;
        int journalIndex;
        int selectionKey;

        JournalRow(String date, String particulars, String accountNumber,
                   String debit, String credit, int journalIndex, int selectionKey) {
            this.date = date;
            this.particulars = particulars;
            this.accountNumber = accountNumber;
            this.debit = debit;
            this.credit = credit;
            this.journalIndex = journalIndex;
            this.selectionKey = selectionKey;
        }
    }

    private static class JournalEntry {
        String date;
        String description;
        int journalIndex;
        int selectionKey;
        LinkedHashMap<Integer, Double> debits = new LinkedHashMap<>();
        LinkedHashMap<Integer, Double> credits = new LinkedHashMap<>();

        JournalEntry(String date, String description, int journalIndex, int selectionKey) {
            this.date = date;
            this.description = description;
            this.journalIndex = journalIndex;
            this.selectionKey = selectionKey;
        }

        void addDebit(int accountId, double amount) {
            debits.put(accountId, debits.getOrDefault(accountId, 0.0) + amount);
        }

        void addCredit(int accountId, double amount) {
            credits.put(accountId, credits.getOrDefault(accountId, 0.0) + amount);
        }
    }

    private final String[] columns = {"Date", "Particulars", "Account #", "Debit", "Credit"};
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final List<JournalRow> rows;
    private final Ledger ledger;

    public TransactionTableModel(Ledger ledger) {
        this.ledger = ledger;
        this.rows = new ArrayList<>();
    }

    public void setTransactions(List<Transaction> list) {
        rows.clear();

        // Build logical journal entries: each grouped transaction (groupId != 0)
        // becomes one entry; non-grouped transactions become standalone entries.
        LinkedHashMap<String, JournalEntry> entries = new LinkedHashMap<>();

        for (int i = 0; i < list.size(); i++) {
            Transaction t = list.get(i);
            int groupId = t.getGroupId();
            String key = groupId != 0 ? "G:" + groupId : "S:" + i;

            JournalEntry entry = entries.get(key);
            if (entry == null) {
                int selectionKey = groupId != 0 ? groupId : -(i + 1);
                entry = new JournalEntry(fmt.format(t.getDate()), t.getDescription(), i, selectionKey);
                entries.put(key, entry);
            }

            if (entry.description == null || entry.description.trim().isEmpty() || entry.description.toLowerCase().contains("hst")) {
                entry.description = t.getDescription();
            }

            entry.addDebit(t.getDebitAccount(), t.getAmount());
            entry.addCredit(t.getCreditAccount(), t.getAmount());
        }

        for (JournalEntry entry : entries.values()) {
            boolean firstRow = true;

            // debits first
            for (Map.Entry<Integer, Double> debitLine : entry.debits.entrySet()) {
                int accountId = debitLine.getKey();
                LedgerAccount acc = ledger.getAccountById(accountId);
                String accountName = acc != null ? acc.getName() : ("Account " + accountId);
                rows.add(new JournalRow(
                    firstRow ? entry.date : "",
                    accountName,
                    String.valueOf(accountId),
                    String.format("$%.2f", debitLine.getValue()),
                    "",
                    entry.journalIndex,
                    entry.selectionKey
                ));
                firstRow = false;
            }

            // credits next (indented)
            for (Map.Entry<Integer, Double> creditLine : entry.credits.entrySet()) {
                int accountId = creditLine.getKey();
                LedgerAccount acc = ledger.getAccountById(accountId);
                String accountName = acc != null ? acc.getName() : ("Account " + accountId);
                rows.add(new JournalRow(
                    "",
                    "    " + accountName,
                    String.valueOf(accountId),
                    "",
                    String.format("$%.2f", creditLine.getValue()),
                    entry.journalIndex,
                    entry.selectionKey
                ));
            }

            // description line last
            rows.add(new JournalRow(
                "",
                entry.description == null ? "" : entry.description,
                "",
                "",
                "",
                entry.journalIndex,
                entry.selectionKey
            ));
        }

        fireTableDataChanged();
    }

    public int getJournalIndexForRow(int row) {
        if (row < 0 || row >= rows.size()) return -1;
        return rows.get(row).journalIndex;
    }

    public int getSelectionKeyForRow(int row) {
        if (row < 0 || row >= rows.size()) return Integer.MIN_VALUE;
        return rows.get(row).selectionKey;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        JournalRow row = rows.get(rowIndex);
        switch (columnIndex) {
            case 0: return row.date;
            case 1: return row.particulars;
            case 2: return row.accountNumber;
            case 3: return row.debit;
            case 4: return row.credit;
            default: return "";
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}
