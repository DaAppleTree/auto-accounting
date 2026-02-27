import java.util.ArrayList;

public class Journal {
    // ArrayList for storing transactions
    private ArrayList<Transaction> transactions;

    // constructor
    public Journal() {
        transactions = new ArrayList<>();
    }
    
    // method to add transactions
    public void addTransaction(Transaction transaction) {
        if (transaction == null) {
            System.out.println("Transaction does not exist.");
        } else {
            transactions.add(transaction);
        }
    }

    // accessor method for all transactions
    public ArrayList<Transaction> getAllTransactions() {
        return transactions;
    }

    // method to clear all transactions
    public void clearTransactions() {
        transactions.clear();
    }

    // prints all transactions
    public void printJournal() {
        System.out.println("Journal Entries");
        for (Transaction t : transactions) {
            System.out.println(t);
        }
    }
}