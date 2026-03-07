import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

public class LedgerTableModel extends AbstractTableModel {
    private List<LedgerAccount> data;
    private String[] columns = {"ID","Name","Type","Debit Total","Credit Total","Balance"};

    public LedgerTableModel() {
        data = new ArrayList<>();
    }

    public void setAccounts(List<LedgerAccount> list) {
        data = new ArrayList<>(list);
        fireTableDataChanged();
    }

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
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int col) {
        return columns[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        LedgerAccount a = data.get(row);
        boolean displayOnly = a.getId() <= 0;
        switch (col) {
            case 0:
                return displayOnly ? "" : a.getId();
            case 1:
                return a.getName();
            case 2:
                return displayOnly ? "" : a.getType();
            case 3:
                return displayOnly ? "" : String.format("$%.2f", a.getDebitTotal());
            case 4:
                return displayOnly ? "" : String.format("$%.2f", a.getCreditTotal());
            case 5:
                return displayOnly ? "" : formatCurrency(a.getBalance());
        }
        return null;
    }

    private String formatCurrency(double value) {
        if (value < 0) {
            return String.format("-$%.2f", Math.abs(value));
        }
        return String.format("$%.2f", value);
    }
}