import java.util.HashMap;
public class Ledger {

    // HashMap to store account names (keys) and ledger accounts (values)
    private HashMap<String, LedgerAccount> accounts;

    // constructor
    public Ledger() {
        accounts = new HashMap<>();
    }

    public void addAccount(LedgerAccount account) {

        // check for null account
        if (account == null) {
            System.out.println("Invalid account.");
            return;
        }
        
        // check for duplicate account name
        if (accounts.containsKey(account.getName())) {
            System.out.println("Account name \"" + account.getName() + "\" already exists.");
            return;
        }

        // check for duplicate account ID
        for (LedgerAccount a : accounts.values()){
            if (a.getId() == account.getId()) {
                System.out.println("Account ID \"" + account.getId() + "\" already taken.");
                return;
            }
        }

        // adds account to ledger
        accounts.put(account.getName(), account);
    }

    // accessor method for the ledger accounts
    public LedgerAccount getAccount(String name) {
        return accounts.get(name);
    }

    // posts the debits and credits from each transaction to the corresponding ledger account
    public void updateFromJournal(Journal journal) {
        // reset all accounts
        for (LedgerAccount a : accounts.values()){
            a.reset();
        }

        // repost all journal entries
        for (Transaction t : journal.getAllTransactions()) {
            LedgerAccount debitAccount = accounts.get(t.getDebitAccount());
            LedgerAccount creditAccount = accounts.get(t.getCreditAccount());
            if (debitAccount != null) {
                debitAccount.postDebit(t.getAmount());
            }
            if (creditAccount != null) {
                creditAccount.postCredit(t.getAmount());
            }
        }
    }

    // generates trial balance for all accounts in the ledger
    public HashMap<String, Double> getTrialBalance() {
        HashMap<String, Double> trialBalance = new HashMap<>();
        for (String name: accounts.keySet()) {
            trialBalance.put(name, accounts.get(name).getBalance());
        }
        return trialBalance;
    }

    // prints all the ledger accounts
    public void printAccounts() {
        System.out.println("Ledger Accounts");
        for (LedgerAccount account : accounts.values()) {
            System.out.println(account);
        }
    }

}