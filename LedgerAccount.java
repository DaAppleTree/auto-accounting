
public class LedgerAccount {
    private int id; // <=0 for abstract/accounts without ID
    private String name;
    private String type;
    private double debitTotal;
    private double creditTotal;
    private LedgerAccount parent;        
    private java.util.List<LedgerAccount> subaccounts;

    // constructor for leaf accounts with ID
    public LedgerAccount(int id, String name, String type) {
        this(id, name, type, null);
    }

    // constructor allowing parent (for subcategories)
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
        this(0, name, type, null);
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public double getDebitTotal() { return debitTotal; }
    public double getCreditTotal() { return creditTotal; }

    public LedgerAccount getParent() { return parent; }
    public void setParent(LedgerAccount parent) { this.parent = parent; }

    public java.util.List<LedgerAccount> getSubaccounts() {
        return subaccounts;
    }

    public void addSubaccount(LedgerAccount child) {
        if (child != null && !subaccounts.contains(child)) {
            subaccounts.add(child);
            child.parent = this;
        }
    }

    public void postDebit(double debit) {
        if (debit > 0) {
            this.debitTotal += debit;
        }
    }

    public void postCredit(double credit) {
        if (credit > 0) {
            this.creditTotal += credit;
        }
    }

    public void reset() {
        this.debitTotal = 0;
        this.creditTotal = 0;
    }

    public double getBalance() {
        if (getNormalSide().equals("debit")) {
            return this.debitTotal - this.creditTotal;
        } else {
            return this.creditTotal - this.debitTotal;
        }
    }

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

    public String toString() {
        return String.format("ID: %d | Name: %s | Type: %s | DR: %.2f | CR: %.2f | Balance: %.2f (%s)",
                           id, name, type, debitTotal, creditTotal, getBalance(), getNormalSide());
    }
}