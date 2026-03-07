import java.time.LocalDate;

public class Transaction {
    private static int nextGroupId = 1;

    public static int allocateGroupId() {
        return nextGroupId++;
    }

    public static void ensureNextGroupIdAtLeast(int candidateNext) {
        if (candidateNext > nextGroupId) {
            nextGroupId = candidateNext;
        }
    }
    private LocalDate date;
    private String description;
    private int debitAccount;
    private int creditAccount;
    private double amount;
    private int groupId; // 0 = no group, >0 = transactions in same group

    // pointers to previous and next transaction
    private Transaction next;
    private Transaction prev;

    // constructor using LocalDate (no group)
    public Transaction(LocalDate date, String description, int debitAccount, int creditAccount, double amount) {
        this(date, description, debitAccount, creditAccount, amount, 0);
    }

    // constructor with explicit group id
    public Transaction(LocalDate date, String description, int debitAccount, int creditAccount, double amount, int groupId) {
        this.date = date;
        this.description = description;
        this.debitAccount = debitAccount;
        this.creditAccount = creditAccount;
        this.amount = amount;
        this.groupId = groupId;
        this.next = null;
        this.prev = null;
    }

    // overloaded constructor using year, month, day
    public Transaction(int year, int month, int day, String description, int debitAccount, int creditAccount, double amount) {
        this(LocalDate.of(year, month, day), description, debitAccount, creditAccount, amount);
    }

    // getter and setter methods
    public LocalDate getDate() {return date;}
    public void setDate(LocalDate date) {this.date = date;}

    public int getYear() {return date.getYear();}
    public int getMonth() {return date.getMonthValue();}
    public int getDay() {return date.getDayOfMonth();}

    public String getDescription() {return description;}
    public void setDescription(String description) {this.description = description;}

    public int getDebitAccount() {return debitAccount;}
    public void setDebitAccount(int debitAccount) {
        if (Ledger.accountsById.containsKey(debitAccount)) {
            this.debitAccount = debitAccount;
        } else {
            System.out.println("Invalid debit account");
        }
    }

    public int getCreditAccount() {return creditAccount;}
    public void setCreditAccount(int creditAccount) {
        if (Ledger.accountsById.containsKey(creditAccount)) {
            this.creditAccount = creditAccount;
        } else {
            System.out.println("Invalid credit account");
        }
    }

    public double getAmount() {return amount;}
    public void setAmount(double amount) {
        if (amount >= 0){
            this.amount = amount;
        } else {
            System.out.println("Invalid amount.");
        }
    }

    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }

    public boolean isGrouped() { return groupId != 0; }


    // linked list pointer getters and setters
    public Transaction getNext() {return next;}
    public void setNext(Transaction next) {this.next = next;}

    public Transaction getPrev() {return prev;}
    public void setPrev(Transaction prev) {this.prev = prev;}

    // check if this transaction has a previous or following transaction
    public boolean hasNext() {return next != null;}
    public boolean hasPrev() {return prev != null;}
    
    // compares two transactions by date
    public boolean isAfter(Transaction other) {return this.date.isAfter(other.date);}
    public boolean isBefore(Transaction other) {return this.date.isBefore(other.date);}

    // string representation for display
    @Override
    public String toString() {
        return date + " | " + description + " | DR: " + debitAccount + " | CR: " + creditAccount + " | $" + amount;
    }

    public String toDebugString() {
        return String.format("[Transaction: date=%s, desc=%s, dr=%d, cr=%d, amt=%.2f]", 
                           date, description, debitAccount, creditAccount, amount);
    }
}