public class LedgerAccount {
    
    // variable declaration
    private int id;
    private String name;
    private String type;
    private double debitTotal;
    private double creditTotal;

    // constructor
    public LedgerAccount(int id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.debitTotal = 0;
        this.creditTotal = 0;
    }

    // accessor and modifier methods
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // methods to posting debits and credits 
    public void postDebit(double debit) {
        // ensure debit is postive
        if (debit > 0) {
            this.debitTotal += debit;
        }
    }

    public void postCredit(double credit) {
        // ensure credit is postive
        if (credit > 0) {
            this.creditTotal += credit;
        }
    }

    public void reset() {
        this.debitTotal = 0;
        this.creditTotal = 0;
    }

    // returns the current balance based on account type
    public double getBalance() {
        if (getNormalSide().equals("debit")) {
            return this.debitTotal - this.creditTotal;
        } else {
            return this.creditTotal - this.debitTotal;
        }
    }

    // determines normal side for this account type
    public String getNormalSide() {
        switch (type.toLowerCase()) {
            // in accounting, assets and expenses have debit normal
            case "asset":
            case "expense":
                return "debit";

            // in accounting, liabilities, equity, and revenues have credit normal
            case "liability":
            case "equity":
            case "revenue":
                return "credit";
            default:
                return "debit";
        }
    }

    // string representation
    public String toString() {
        return "ID: " + id + " | Name: " + name + " | Type: " + type + " | DR: " + debitTotal + " | CR: " + creditTotal + " | Balance: " + getBalance() + " (" + getNormalSide() + ")";
    }
}