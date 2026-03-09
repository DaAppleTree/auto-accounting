import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.table.AbstractTableModel;

// class for creating the tables on the transaction page
public class TransactionTableModel extends AbstractTableModel {
    private final String[] columns = {"Date", "Particulars", "Account #", "Debit", "Credit"};
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final List<String[]> rows;
    private final List<Integer> journalIndexes;
    private final List<Integer> selectionKeys;
    private final Ledger ledger;

    // constructor for a transaction table
    public TransactionTableModel(Ledger ledger) {
        this.ledger = ledger;
        this.rows = new ArrayList<>();
        this.journalIndexes = new ArrayList<>();
        this.selectionKeys = new ArrayList<>();
    }

    private void addRow(String date, String particulars, String accountNumber, String debit, String credit, int journalIndex, int selectionKey) {
        rows.add(new String[]{date, particulars, accountNumber, debit, credit});
        journalIndexes.add(journalIndex);
        selectionKeys.add(selectionKey);
    }

    // updates transactions for this component
    public void setTransactions(List<Transaction> list) {
        rows.clear();
        journalIndexes.clear();
        selectionKeys.clear();

        // turn grouped transactions into one journal entry
        LinkedHashMap<String, String> entryDates = new LinkedHashMap<>();
        LinkedHashMap<String, String> entryDescriptions = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> entryJournalIndexes = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> entrySelectionKeys = new LinkedHashMap<>();
        LinkedHashMap<String, LinkedHashMap<Integer, Double>> entryDebits = new LinkedHashMap<>();
        LinkedHashMap<String, LinkedHashMap<Integer, Double>> entryCredits = new LinkedHashMap<>();

        for (int i = 0; i < list.size(); i++) {
            Transaction t = list.get(i);
            int groupId = t.getGroupId();
            String key = groupId != 0 ? "G:" + groupId : "S:" + i;

            if (!entryDates.containsKey(key)) {
                int selectionKey = groupId != 0 ? groupId : -(i + 1);
                entryDates.put(key, fmt.format(t.getDate()));
                entryDescriptions.put(key, t.getDescription());
                entryJournalIndexes.put(key, i);
                entrySelectionKeys.put(key, selectionKey);
                entryDebits.put(key, new LinkedHashMap<>());
                entryCredits.put(key, new LinkedHashMap<>());
            }

            String currentDescription = entryDescriptions.get(key);
            if (currentDescription == null || currentDescription.trim().isEmpty() || currentDescription.toLowerCase().contains("hst")) {
                entryDescriptions.put(key, t.getDescription());
            }

            LinkedHashMap<Integer, Double> debits = entryDebits.get(key);
            LinkedHashMap<Integer, Double> credits = entryCredits.get(key);
            debits.put(t.getDebitAccount(), debits.getOrDefault(t.getDebitAccount(), 0.0) + t.getAmount());
            credits.put(t.getCreditAccount(), credits.getOrDefault(t.getCreditAccount(), 0.0) + t.getAmount());
        }

        for (String key : entryDates.keySet()) {
            String date = entryDates.get(key);
            String description = entryDescriptions.get(key);
            int journalIndex = entryJournalIndexes.get(key);
            int selectionKey = entrySelectionKeys.get(key);
            LinkedHashMap<Integer, Double> debits = entryDebits.get(key);
            LinkedHashMap<Integer, Double> credits = entryCredits.get(key);

            boolean firstRow = true;

            // add debits first
            for (Map.Entry<Integer, Double> debitLine : debits.entrySet()) {
                int accountId = debitLine.getKey();
                LedgerAccount acc = ledger.getAccountById(accountId);
                String accountName = acc != null ? acc.getName() : ("Account " + accountId);
                addRow(firstRow ? date : "", accountName, String.valueOf(accountId), String.format("$%.2f", debitLine.getValue()), "", journalIndex, selectionKey);
                firstRow = false;
            }

            // add credits next
            for (Map.Entry<Integer, Double> creditLine : credits.entrySet()) {
                int accountId = creditLine.getKey();
                LedgerAccount acc = ledger.getAccountById(accountId);
                String accountName = acc != null ? acc.getName() : ("Account " + accountId);
                addRow("", "    " + accountName, String.valueOf(accountId), "", String.format("$%.2f", creditLine.getValue()), journalIndex, selectionKey);
            }

            // description line last
            addRow("", description == null ? "" : description, "", "", "", journalIndex, selectionKey);
        }

        fireTableDataChanged();
    }

    // returns journal index for a row
    public int getJournalIndexForRow(int row) {
        if (row < 0 || row >= rows.size()) return -1;
        return journalIndexes.get(row);
    }

    // returns selection key for a row
    public int getSelectionKeyForRow(int row) {
        if (row < 0 || row >= rows.size()) return Integer.MIN_VALUE;
        return selectionKeys.get(row);
    }

    @Override
    // returns row count
    public int getRowCount() {
        return rows.size();
    }

    @Override
    // returns column count
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    // returns column name
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    // returns value at a given row and column
    public Object getValueAt(int rowIndex, int columnIndex) {
        String[] row = rows.get(rowIndex);
        if (columnIndex < 0 || columnIndex >= row.length) {
            return "";
        }
        return row[columnIndex];
    }

    @Override
    // checks whether a cell is editable
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}
