// represents a single account in the ledger (e.g. Cash, Accounts Receivable)
public class LedgerAccount {
    private int id; // -1 for abstract accounts without ID
    private String name;
    private String type;
    private double debitTotal;
    private double creditTotal;
    private LedgerAccount parent;        
    private java.util.List<LedgerAccount> subaccounts;

    // constructor for leaf accounts
    public LedgerAccount(int id, String name, String type) {
        this(id, name, type, null);
    }

    // constructor for accounts with parent
    public LedgerAccount(int id, String name, String type, LedgerAccount parent) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.debitTotal = 0;
        this.creditTotal = 0;
        this.parent = parent;
        this.subaccounts = new java.util.ArrayList<>();
        if (parent != null) {
            parent.addSubaccount(this);
        }
    }

    // constructor for abstract nodes without ID
    public LedgerAccount(String name, String type) {
        this(-1, name, type, null);
    }

    // accessor and modifier methods
    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public double getDebitTotal() { return debitTotal; }
    public double getCreditTotal() { return creditTotal; }

    public LedgerAccount getParent() { return parent; }
    public void setParent(LedgerAccount parent) { this.parent = parent; }

    // returns subaccounts
    public java.util.List<LedgerAccount> getSubaccounts() {
        return subaccounts;
    }

    // adds subaccount to the current state
    public void addSubaccount(LedgerAccount child) {
        if (child != null && child != this && !subaccounts.contains(child)) {
            subaccounts.add(child);
            child.parent = this;
        }
    }

    // removes subaccount from the current state
    public void removeSubaccount(LedgerAccount child) {
        if (child != null) {
            subaccounts.remove(child);
            if (child.parent == this) {
                child.parent = null;
            }
        }
    }

    // posts debit amounts
    public void postDebit(double debit) {
        if (debit > 0) {
            this.debitTotal += debit;
        }
    }

    // posts credit amounts
    public void postCredit(double credit) {
        if (credit > 0) {
            this.creditTotal += credit;
        }
    }

    // resets debit and credit amounts
    public void reset() {
        this.debitTotal = 0;
        this.creditTotal = 0;
    }

    // returns balance
    public double getBalance() {
        if (getNormalSide().equals("debit")) {
            return this.debitTotal - this.creditTotal;
        } else {
            return this.creditTotal - this.debitTotal;
        }
    }

    // returns the normal side for balances
    public String getNormalSide() {
        switch (type.toLowerCase()) {
            case "asset":
            case "expense":
                return "debit";
            case "liability":
            case "equity":
            case "revenue":
                return "credit";
            default:
                return "debit";
        }
    }
}
