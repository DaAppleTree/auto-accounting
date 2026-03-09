import java.time.LocalDate;

// class for creating a transaction
public class Transaction {
    private static int nextGroupId = 1;

    // handles group ID behavior for transaction
    public static int allocateGroupId() {
        return nextGroupId++;
    }

    // finds the next group ID for a transaction
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
    private int groupId;

    // pointer to next transaction
    private Transaction next;

    // constructor using LocalDate
    public Transaction(LocalDate date, String description, int debitAccount, int creditAccount, double amount) {
        this(date, description, debitAccount, creditAccount, amount, 0);
    }

    // constructor using group ID
    public Transaction(LocalDate date, String description, int debitAccount, int creditAccount, double amount, int groupId) {
        this.date = date;
        this.description = description;
        this.debitAccount = debitAccount;
        this.creditAccount = creditAccount;
        this.amount = amount;
        this.groupId = groupId;
        this.next = null;
    }

    // accessor and modifier methods
    public LocalDate getDate() {return date;}
    public String getDescription() {return description;}
    public void setDescription(String description) {this.description = description;}
    public int getDebitAccount() {return debitAccount;}
    public int getCreditAccount() {return creditAccount;}
    public double getAmount() {return amount;}
    public int getGroupId() { return groupId; }

    // linked list pointers
    public Transaction getNext() {return next;}
    public void setNext(Transaction next) {this.next = next;}

    // check if this transaction has a following transaction
    public boolean hasNext() {return next != null;}
    
    // compares two transactions by date
    public boolean isBefore(Transaction other) {return this.date.isBefore(other.date);}

    @Override
    // string representation
    public String toString() {
        return date + " | " + description + " | DR: " + debitAccount + " | CR: " + creditAccount + " | $" + amount;
    }
}
