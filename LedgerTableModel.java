import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

// class for creating the tables on the ledger page
public class LedgerTableModel extends AbstractTableModel {
    private final ArrayList<LedgerAccount> data;
    private static final String[] COLUMNS = {"ID","Name","Type","Debit Total","Credit Total","Balance"};

    // constructor for the ledger table
    public LedgerTableModel() {
        data = new ArrayList<>();
    }

    // updates accounts for this component
    public void setAccounts(ArrayList<LedgerAccount> list) {
        data.clear();
        data.addAll(list);
        fireTableDataChanged();
    }

    // returns ledger account at a given row
    public LedgerAccount getAccountAt(int row) {
        if (row < 0 || row >= data.size()) {
            return null;
        }
        LedgerAccount a = data.get(row);
        if (a == null || a.getId() <= 0) {
            return null;
        }
        return a;
    }

    @Override
    // returns row count
    public int getRowCount() {
        return data.size();
    }

    @Override
    // returns column count
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    // returns column name
    public String getColumnName(int col) {
        return COLUMNS[col];
    }

    @Override
    // returns value for a given column
    public Object getValueAt(int row, int col) {
        LedgerAccount a = data.get(row);
        boolean displayOnly = a.getId() <= 0;
        switch (col) {
            case 0:
                if (displayOnly) return "";
                return a.getId();
            case 1:
                return a.getName();
            case 2:
                if (displayOnly) return "";
                return a.getType();
            case 3:
                if (displayOnly) return "";
                return String.format("$%.2f", a.getDebitTotal());
            case 4:
                if (displayOnly) return "";
                return String.format("$%.2f", a.getCreditTotal());
            case 5:
                if (displayOnly) return "";
                return formatCurrency(a.getBalance());
        }
        return null;
    }

    // formats currency for display
    private String formatCurrency(double value) {
        if (value < 0) {
            return String.format("-$%.2f", Math.abs(value));
        }
        return String.format("$%.2f", value);
    }
}

