import java.time.LocalDate;
public class Main {
    public static void main(String[] args) {
        // create ledger and journal
        Ledger ledger = new Ledger();
        Journal journal = new Journal();

        // create ledger accounts
        LedgerAccount cash = new LedgerAccount(101, "Cash", "Asset");
        LedgerAccount revenue = new LedgerAccount(401, "Revenue", "Revenue");
        LedgerAccount supplies = new LedgerAccount(105, "Supplies", "Asset");

        // adding accounts to ledger
        ledger.addAccount(cash);
        ledger.addAccount(revenue);
        ledger.addAccount(supplies);

        // testing duplicate name
        ledger.addAccount(new LedgerAccount(999, "Cash", "Asset"));

        // testing duplicate ID
        ledger.addAccount(new LedgerAccount(101, "Inventory", "Asset"));

        // printing ledger
        ledger.printAccounts();

        // adding transactions to journal
        journal.addTransaction(new Transaction(LocalDate.of(2025, 2, 26), "#1", "Cash", "Revenue", 500));
        journal.addTransaction(new Transaction(2025, 2, 28, "#2", "Supplies", "Cash", 200));
        
        // printing journal
        journal.printJournal();

        // update and print ledger
        ledger.updateFromJournal(journal);
        ledger.printAccounts();
    }
}