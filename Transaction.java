import java.time.DateTimeException;
import java.time.LocalDate;

public class Transaction {

    // variable declaration
    private LocalDate date;
    private String description;
    private String debitAccount;
    private String creditAccount;
    private double amount;

    // constructor using LocalDate
    public Transaction(LocalDate date, String description, String debitAccount, String creditAccount, double amount){
        setDate(date);
        setDescription(description);
        setDebitAccount(debitAccount);
        setCreditAccount(creditAccount);
        setAmount(amount);
    }

    // overloaded constructor using numeric year, month, and day
    public Transaction(int year, int month, int day, String description, String debitAccount, String creditAccount, double amount){
        this(LocalDate.of(year, month, day), description, debitAccount, creditAccount, amount);
    }
    
    // accessor and modifier methods
    public LocalDate getDate() {
        return date;
    }

    public int getYear() {
        return date.getYear();
    }

    public int getMonth() {
        return date.getMonthValue();
    }

    public int getDay() {
        return date.getDayOfMonth();
    }

    public void setDate(LocalDate date) {
        this.date = date; // LocalDate is always valid
    }

    public void setDate(int year, int month, int day) {
        // ensure year, month, and day are valid
        try {
            setDate(LocalDate.of(year, month, day));
        } catch (DateTimeException e) {
            System.out.println("Invalid date inputted.");
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDebitAccount() {
        return debitAccount;
    }

    public void setDebitAccount(String debitAccount) {
        // ensure debit account is not equal to credit account
        if (debitAccount != null && !debitAccount.equals(this.creditAccount)) {
            this.debitAccount = debitAccount;
        }
    }

    public String getCreditAccount() {
        return creditAccount;
    }

    public void setCreditAccount(String creditAccount) {
        // ensure credit account is not equal to debit account
        if (creditAccount != null && !creditAccount.equals(this.debitAccount)) {
            this.creditAccount = creditAccount;
        }
    }

    public double getAmount() {
        return amount;
    } 

    public void setAmount(double amount) {
        // ensure the amount is positive
        if (amount > 0) {
            this.amount = amount;
        }
    }

    // string representation
    public String toString() {
        return date + " | " + description + " | DR: " + debitAccount + " | CR: " + creditAccount + " | $" + amount;
    }
}