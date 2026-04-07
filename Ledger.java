import java.util.*;
import javax.swing.JOptionPane;

// class for creating a ledger
public class Ledger {
    private static final String ACCOUNT_FILE = "accounts.csv";
    public static java.util.ArrayList<LedgerAccount> sortedAccounts;
    public static java.util.HashMap<String, LedgerAccount> accountsByName;
    public static java.util.HashMap<Integer, LedgerAccount> accountsById;
    private final java.util.ArrayList<LedgerAccount> rootAccounts;

    // constructor for ledger
    public Ledger() {
        if (sortedAccounts == null) {
            sortedAccounts = new ArrayList<>();
            accountsByName = new HashMap<>();
            accountsById = new HashMap<>();
        }
        rootAccounts = new ArrayList<>();
        loadFromFile();
        rebuildTree();
    }

    // finds or creates a top-level root account for the given type (e.g. "Asset")
    public LedgerAccount getOrCreateRoot(String type) {
        if (accountsByName.containsKey(type)) {
            return accountsByName.get(type);
        }
        LedgerAccount root = new LedgerAccount(type, type);
        rootAccounts.add(root);
        accountsByName.put(type, root);
        return root;
    }

    // finds or creates a category account (e.g. "Current Asset") under the given parent
    public LedgerAccount getOrCreateSubtype(String name, String type, LedgerAccount parent) {
        LedgerAccount existing = accountsByName.get(name);
        if (existing != null) {
            if (existing == parent || parent == null) {
                return existing;
            }
            moveUnderParent(existing, parent);
            return existing;
        }
        LedgerAccount acc = new LedgerAccount(-1, name, type, parent);
        addAccount(acc);
        return acc;
    }

    // returns the correct parent node for an account, creating the subtype if needed
    private LedgerAccount getSubtypeParent(LedgerAccount account) {
        if (account == null || account.getId() <= 0) return null;
        LedgerAccount typeRoot = accountsByName.get(account.getType());
        if (typeRoot == null) return null;

        String type = account.getType();
        String parentName = "";
        if (account.getParent() != null) {
            parentName = account.getParent().getName();
        }
        String subtype = null;

        if (type.equals("Asset")) {
            if (parentName.equalsIgnoreCase("Fixed Asset")) {
                subtype = "Fixed Asset";
            } else {
                subtype = "Current Asset";
            }
        } else if (type.equals("Liability")) {
            if (parentName.equalsIgnoreCase("Long-Term Liability")) {
                subtype = "Long-Term Liability";
            } else {
                subtype = "Current Liability";
            }
        } else if (type.equals("Expense")) {
            if (parentName.equalsIgnoreCase("Other Expense")) {
                subtype = "Other Expense";
            } else {
                subtype = "Operating Expense";
            }
        }

        if (subtype == null) {
            return typeRoot;
        }
        return getOrCreateSubtype(subtype, type, typeRoot);
    }

    // moves an account under a new parent category when needed
    private void moveUnderParent(LedgerAccount account, LedgerAccount parent) {
        if (account == null || parent == null || account == parent) {
            return;
        }
        LedgerAccount oldParent = account.getParent();
        if (oldParent != null && oldParent != parent) {
            oldParent.removeSubaccount(account);
        }
        if (account.getParent() != parent) {
            parent.addSubaccount(account);
        }
    }

    // rebuilds the account tree, placing each account under the right subtype
    private void rebuildTree() {
        LedgerAccount asset = getOrCreateRoot("Asset");
        LedgerAccount liability = getOrCreateRoot("Liability");
        LedgerAccount equity = getOrCreateRoot("Equity");
        LedgerAccount revenue = getOrCreateRoot("Revenue");
        LedgerAccount expense = getOrCreateRoot("Expense");
        getOrCreateSubtype("Current Asset", "Asset", asset);
        getOrCreateSubtype("Fixed Asset", "Asset", asset);
        getOrCreateSubtype("Current Liability", "Liability", liability);
        getOrCreateSubtype("Long-Term Liability", "Liability", liability);
        getOrCreateSubtype("Operating Expense", "Expense", expense);
        getOrCreateSubtype("Other Expense", "Expense", expense);

        for (LedgerAccount account : accountsById.values()) {
            moveUnderParent(account, getSubtypeParent(account));
        }

        if (!accountsByName.containsKey("HST Recoverable")) {
            addAccount(new LedgerAccount(701, "HST Recoverable", "Asset", asset));
        }
        if (!accountsByName.containsKey("HST Payable")) {
            addAccount(new LedgerAccount(702, "HST Payable", "Liability", liability));
        }
    }

    // adds an account to the ledger and saves the change
    public boolean addAccount(LedgerAccount account) {
        if (account == null) {
            return false;
        }

        // handle abstract accounts with no IDs
        if (account.getId() <= 0) {
            if (accountsByName.containsKey(account.getName())) {
                return false;
            }

            // attach to parent or root
            LedgerAccount parent = account.getParent();
            if (parent != null) {
                parent.addSubaccount(account);
            } else {
                rootAccounts.add(account);
            }
            accountsByName.put(account.getName(), account);
            saveToFile();
            return true;
        }

        // leaf account with ID
        if (accountsByName.containsKey(account.getName())) {
            return false;
        } else if (accountsById.containsKey(account.getId())) {
            return false;
        }

        int insertPos = insertIndex(account.getId());
        sortedAccounts.add(insertPos, account);

        accountsByName.put(account.getName(), account);
        accountsById.put(account.getId(), account);

        moveUnderParent(account, getSubtypeParent(account));
        saveToFile();
        return true;
    }

    // binary search: returns the index where targetId should be inserted
    private int insertIndex(int targetId) {
        int left = 0;
        int right = sortedAccounts.size() - 1;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            int midId = sortedAccounts.get(mid).getId();
            
            if (midId == targetId) {
                return mid;
            } else if (midId < targetId) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return left;
    }

    // searches accounts by ID prefix (numeric) or name prefix (text)
    public ArrayList<LedgerAccount> searchAccounts(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return new ArrayList<>();
        }
        
        // first try numeric search by ID prefix
        try {
            int prefixNum = Integer.parseInt(prefix);
            int scale = (int) Math.pow(10, Math.max(0, 3 - prefix.length()));
            int minId = prefixNum * scale;
            int maxId = minId + scale - 1;

            ArrayList<LedgerAccount> numericMatches = new ArrayList<>();
            int start = insertIndex(minId);
            for (int i = start; i < sortedAccounts.size(); i++) {
                LedgerAccount acc = sortedAccounts.get(i);
                if (acc.getId() > maxId) {
                    break;
                }
                if (String.valueOf(acc.getId()).startsWith(prefix)) {
                    numericMatches.add(acc);
                }
            }
            if (!numericMatches.isEmpty()) {
                return numericMatches;
            }
        } catch (NumberFormatException e) {}
        
        // if not numeric or numeric search returned nothing, try searching for the name
        ArrayList<LedgerAccount> matches = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (LedgerAccount acc : sortedAccounts) {
            if (acc.getName().toLowerCase().startsWith(lower)) {
                matches.add(acc);
            }
        }
        return matches;
    }

    // returns account by name
    public LedgerAccount getAccountByName(String name) {
        return accountsByName.get(name);
    }

    // returns account by ID
    public LedgerAccount getAccountById(int id) {
        return accountsById.get(id);
    }

    // returns all account names
    public Set<String> getAccountNames() {
        return accountsByName.keySet();
    }

    // returns all accounts (leaf accounts with IDs only)
    public ArrayList<LedgerAccount> getAllAccounts() {
        return new ArrayList<>(accountsById.values());
    }

    // returns all accounts sorted by ID
    public ArrayList<LedgerAccount> getAllAccountsSorted() {
        return new ArrayList<>(sortedAccounts);
    }

    // recalculates account balances from the journal
    public void updateFromJournal(Journal journal) {
        for (LedgerAccount a : accountsById.values()) {
            a.reset();
        }
        
        for (Transaction t : journal.getAllTransactions()) {
            LedgerAccount debitAccount = accountsById.get(t.getDebitAccount());
            LedgerAccount creditAccount = accountsById.get(t.getCreditAccount());
            
            if (debitAccount != null) {
                debitAccount.postDebit(t.getAmount());
            }
            if (creditAccount != null) {
                creditAccount.postCredit(t.getAmount());
            }
        }
    }

    // adds up balances across an account and all its subaccounts (no cycles)
    private double sumBalances(LedgerAccount node, Set<LedgerAccount> visiting) {
        if (node == null || visiting.contains(node)) {
            return 0;
        }
        visiting.add(node);
        double total = 0;
        if (node.getId() > 0) {
            total += node.getBalance();
        }
        for (LedgerAccount child : node.getSubaccounts()) {
            total += sumBalances(child, visiting);
        }
        visiting.remove(node);
        return total;
    }

    // returns the total balance for an account group by name (includes subaccounts)
    public double getTotalByName(String accountName) {
        LedgerAccount node = accountsByName.get(accountName);
        if (node == null) {
            return 0;
        }
        return sumBalances(node, new HashSet<>());
    }

    // class for a balance at a particular date, used for graphing history
    public static class BalancePoint {
        public final java.time.LocalDate date;
        public final double balance;

        // constructor for a balance point
        public BalancePoint(java.time.LocalDate date, double balance) {
            this.date = date;
            this.balance = balance;
        }
    }

    // returns the running balance history for an account, sorted by date
    public ArrayList<BalancePoint> getBalanceHistory(int accountId, Journal journal) {
        LedgerAccount base = accountsById.get(accountId);
        if (base == null) {
            return new ArrayList<>();
        }
        
        // tracks the running balance as each matching transaction is processed
        LedgerAccount runningAccount = new LedgerAccount(base.getId(), base.getName(), base.getType());
        ArrayList<BalancePoint> history = new ArrayList<>();
        for (Transaction t : journal.getAllTransactions()) {
            if (t.getDebitAccount() == accountId) {
                runningAccount.postDebit(t.getAmount());
            }
            if (t.getCreditAccount() == accountId) {
                runningAccount.postCredit(t.getAmount());
            }
            history.add(new BalancePoint(t.getDate(), runningAccount.getBalance()));
        }
        return history;
    }

    // returns trial balance used by other components
    public HashMap<String, Double> getTrialBalance() {
        HashMap<String, Double> trialBalance = new HashMap<>();
        for (LedgerAccount account : accountsById.values()) {
            trialBalance.put(account.getName(), account.getBalance());
        }
        return trialBalance;
    }

    // removes an account from the ledger and saves the change
    public void removeAccount(int id) {
        LedgerAccount account = accountsById.get(id);
        if (account != null) {
            LedgerAccount parent = account.getParent();
            if (parent != null) {
                parent.removeSubaccount(account);
            }
            accountsById.remove(id);
            accountsByName.remove(account.getName());
            sortedAccounts.remove(account);
            saveToFile();
        }
    }

    // removes all saved accounts from the ledger
    public void clear() {
        accountsById.clear();
        accountsByName.clear();
        sortedAccounts.clear();
        saveToFile();
    }

    // saves all accounts to the CSV file
    private void saveToFile() {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(ACCOUNT_FILE))) {
            for (LedgerAccount acc : accountsByName.values()) {
                String parentName = "";
                if (acc.getParent() != null) {
                    parentName = acc.getParent().getName();
                }
                pw.printf("%d,%s,%s,%s\n", acc.getId(), acc.getName(), acc.getType(), parentName);
            }
        } catch (java.io.IOException e) {
            // error if file is unable to be saved
            JOptionPane.showMessageDialog(null, "Failed to save accounts file.", "Save Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // loads saved accounts from the CSV file
    private void loadFromFile() {
        java.io.File f = new java.io.File(ACCOUNT_FILE);
        if (!f.exists()) return;
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f))) {
            String line;
            java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 4) rows.add(parts);
            }
            java.util.Map<String, LedgerAccount> loadedAccounts = new java.util.HashMap<>();
            for (String[] parts : rows) {
                int id = Integer.parseInt(parts[0]);
                String name = parts[1];
                String type = parts[2];
                LedgerAccount acc;
                if (id > 0) acc = new LedgerAccount(id, name, type);
                else acc = new LedgerAccount(-1, name, type);
                loadedAccounts.put(name, acc);
            }
            for (String[] parts : rows) {
                String name = parts[1];
                String parentName = parts[3];
                LedgerAccount acc = loadedAccounts.get(name);
                if (parentName != null && !parentName.isEmpty()) {
                    LedgerAccount parent = loadedAccounts.get(parentName);
                    if (parent != null) {
                        acc.setParent(parent);
                        parent.addSubaccount(acc);
                    }
                }
                addAccount(acc);
            }
        } catch (java.io.IOException e) {
            // error if file is unable to be loaded
            JOptionPane.showMessageDialog(null, "Failed to load accounts file.", "Load Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}

