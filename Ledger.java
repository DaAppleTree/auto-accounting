import java.util.*;

public class Ledger {
    private static final String ACCOUNT_FILE = "accounts.csv";
    public static java.util.ArrayList<LedgerAccount> sortedAccounts;
    public static java.util.HashMap<String, LedgerAccount> accountsByName;
    public static java.util.HashMap<Integer, LedgerAccount> accountsById;
    private java.util.List<LedgerAccount> rootAccounts;

    public Ledger() {
        if (sortedAccounts == null) {
            sortedAccounts = new ArrayList<>();
            accountsByName = new HashMap<>();
            accountsById = new HashMap<>();
        }
        rootAccounts = new ArrayList<>();
        // ensure root categories exist
        createRootCategory("Asset");
        createRootCategory("Liability");
        createRootCategory("Equity");
        createRootCategory("Revenue");
        createRootCategory("Expense");
        loadFromFile();
        // ensure key HST accounts are present regardless of loaded file contents
        ensureHstAccounts();
    }

    private LedgerAccount createRootCategory(String type) {
        if (accountsByName.containsKey(type)) {
            return accountsByName.get(type);
        }
        LedgerAccount cat = new LedgerAccount(type, type);
        rootAccounts.add(cat);
        accountsByName.put(type, cat);
        return cat;
    }

    // add the HST accounts
    private void ensureHstAccounts() {
        if (!accountsByName.containsKey("HST Receivable")) {
            LedgerAccount assetCat = getOrCreateAbstractAccount("Asset", "Asset", null);
            addAccount(new LedgerAccount(701, "HST Receivable", "Asset", assetCat));
        }
        if (!accountsByName.containsKey("HST Payable")) {
            LedgerAccount liabilityCat = getOrCreateAbstractAccount("Liability", "Liability", null);
            addAccount(new LedgerAccount(702, "HST Payable", "Liability", liabilityCat));
        }
    }

    // constructor to create an abstract (no-ID) account with given name and type, under optional parent.
    public LedgerAccount getOrCreateAbstractAccount(String name, String type, LedgerAccount parent) {
        LedgerAccount existing = accountsByName.get(name);
        if (existing != null) {
            return existing;
        }
        LedgerAccount acc = new LedgerAccount(-1, name, type, parent);
        addAccount(acc);
        return acc;
    }

    public void addAccount(LedgerAccount account) {
        if (account == null) {
            System.out.println("Invalid account.");
            return;
        }

        // handle abstract (no ID) accounts
        if (account.getId() <= 0) {
            if (accountsByName.containsKey(account.getName())) {
                System.out.println("Account name \"" + account.getName() + "\" already exists.");
                return;
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
            return;
        }

        // leaf account with ID
        if (accountsByName.containsKey(account.getName())) {
            System.out.println("Account name \"" + account.getName() + "\" already exists.");
            return;
        } else if (accountsById.containsKey(account.getId())) {
            System.out.println("Account ID \"" + account.getId() + "\" already taken.");
            return;
        }

        int insertPos = findPosition(account.getId());
        if (insertPos < sortedAccounts.size()) {
            sortedAccounts.add(insertPos, account);
        } else {
            sortedAccounts.add(account);
        }

        accountsByName.put(account.getName(), account);
        accountsById.put(account.getId(), account);

        // also if this account has a parent, register under it
        if (account.getParent() != null) {
            account.getParent().addSubaccount(account);
        } else {
            // attach unparented leaf accounts to their type root so tree traversal
            // can recursively aggregate balances by major category.
            LedgerAccount typeRoot = accountsByName.get(account.getType());
            if (typeRoot != null && typeRoot.getId() <= 0) {
                typeRoot.addSubaccount(account);
            }
        }
        saveToFile();
    }

    private int findPosition(int targetId) {
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

    public List<LedgerAccount> findAccountsByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return new ArrayList<>();
        }
        
        // first try numeric search by ID prefix
        try {
            int minId = Integer.parseInt(prefix) * 100;
            int maxId = minId + 99;
            
            int multiplier = (int) Math.pow(10, 3 - prefix.length());
            minId = Integer.parseInt(prefix) * multiplier;
            maxId = minId + multiplier - 1;
            
            int startIndex = findFirstIndex(minId);
            int endIndex = findLastIndex(maxId);
            
            if (startIndex <= endIndex && startIndex >= 0 && endIndex < sortedAccounts.size()) {
                return sortedAccounts.subList(startIndex, endIndex + 1);
            }
        } catch (NumberFormatException e) {
            // not numeric; fall through to name search
        }
        
        // if not numeric or numeric search returned nothing, try searching for the name
        List<LedgerAccount> matches = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (LedgerAccount acc : sortedAccounts) {
            if (acc.getName().toLowerCase().startsWith(lower)) {
                matches.add(acc);
            }
        }
        return matches;
    }

    private int findFirstIndex(int targetId) {
        int left = 0;
        int right = sortedAccounts.size() - 1;
        int result = -1;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            int midId = sortedAccounts.get(mid).getId();
            
            if (midId >= targetId) {
                result = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }
        
        return result;
    }

    private int findLastIndex(int targetId) {
        int left = 0;
        int right = sortedAccounts.size() - 1;
        int result = -1;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            int midId = sortedAccounts.get(mid).getId();
            
            if (midId <= targetId) {
                result = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        
        return result;
    }

    public LedgerAccount getAccountByName(String name) {
        return accountsByName.get(name);
    }

    public LedgerAccount getAccountById(int id) {
        return accountsById.get(id);
    }

    public LedgerAccount findAccountById(int id) {
        return accountsById.get(id);
    }

    public Set<String> getAccountNames() {
        return accountsByName.keySet();
    }

    public LedgerAccount getAccount(String name) {
        return accountsByName.get(name);
    }

    public boolean accountExists(int id) {
        return accountsById.containsKey(id);
    }

    public boolean accountExists(String name) {
        return accountsByName.containsKey(name);
    }

    public Set<Integer> getAllAccountIds() {
        return accountsById.keySet();
    }

    public Set<String> getAllAccountNames() {
        return accountsByName.keySet();
    }

    public ArrayList<LedgerAccount> getAllAccounts() {
        // return only leaf accounts with IDs
        ArrayList<LedgerAccount> list = new ArrayList<>();
        for (LedgerAccount acc : accountsById.values()) {
            list.add(acc);
        }
        return list;
    }

    public java.util.List<LedgerAccount> getRootAccounts() {
        return rootAccounts;
    }

    public ArrayList<LedgerAccount> getAllAccountsSorted() {
        return new ArrayList<>(sortedAccounts);
    }

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

    // recursive traversal helper for tree totals
    private double sumBalancesRecursive(LedgerAccount node) {
        if (node == null) {
            return 0;
        }
        double total = 0;
        if (node.getId() > 0) {
            total += node.getBalance();
        }
        for (LedgerAccount child : node.getSubaccounts()) {
            total += sumBalancesRecursive(child);
        }
        return total;
    }

    // total by any node name (root type, subtype, or leaf)
    public double getRecursiveTotalByName(String accountName) {
        LedgerAccount node = accountsByName.get(accountName);
        if (node == null) {
            return 0;
        }
        return sumBalancesRecursive(node);
    }

    // convenience totals for main types
    public double getTypeTotal(String type) {
        return getRecursiveTotalByName(type);
    }

    /**
     * Represents a balance at a particular date, used for graphing history.
     */
    public static class BalancePoint {
        public final java.time.LocalDate date;
        public final double balance;
        public BalancePoint(java.time.LocalDate date, double balance) {
            this.date = date;
            this.balance = balance;
        }
    }

     // compute the running balance history for the account with the given ID, based on all transactions in the journal. 
    public List<BalancePoint> getBalanceHistory(int accountId, Journal journal) {
        LedgerAccount base = accountsById.get(accountId);
        if (base == null) {
            return new ArrayList<>();
        }
        List<Transaction> txns = new ArrayList<>(journal.getAllTransactions());
        txns.sort((a,b) -> a.getDate().compareTo(b.getDate()));
        // use a temporary account to accumulate balance according to normal side
        LedgerAccount temp = new LedgerAccount(base.getId(), base.getName(), base.getType(), base.getParent());
        List<BalancePoint> history = new ArrayList<>();
        for (Transaction t : txns) {
            if (t.getDebitAccount() == accountId) {
                temp.postDebit(t.getAmount());
            }
            if (t.getCreditAccount() == accountId) {
                temp.postCredit(t.getAmount());
            }
            history.add(new BalancePoint(t.getDate(), temp.getBalance()));
        }
        return history;
    }

    public HashMap<String, Double> getTrialBalance() {
        HashMap<String, Double> trialBalance = new HashMap<>();
        for (LedgerAccount account : accountsById.values()) {
            trialBalance.put(account.getName(), account.getBalance());
        }
        return trialBalance;
    }

    public void printAccounts() {
        System.out.println("\n=== LEDGER ACCOUNTS ===");
        System.out.println("========================");
        
        if (sortedAccounts.isEmpty()) {
            System.out.println("No accounts in ledger");
            return;
        }
        
        for (LedgerAccount account : sortedAccounts) {
            System.out.println(account);
        }
        
        System.out.println("========================");
        System.out.println("Total accounts: " + sortedAccounts.size());
    }

    public boolean removeAccount(int id) {
        LedgerAccount account = accountsById.get(id);
        if (account != null) {
            accountsById.remove(id);
            accountsByName.remove(account.getName());
            sortedAccounts.remove(account);
            System.out.println("Account removed: " + id);
            saveToFile();
            return true;
        }
        return false;
    }

    public boolean removeAccount(String name) {
        LedgerAccount account = accountsByName.get(name);
        if (account != null) {
            accountsByName.remove(name);
            accountsById.remove(account.getId());
            sortedAccounts.remove(account);
            System.out.println("Account removed: " + name);
            saveToFile();
            return true;
        }
        return false;
    }

    public void clear() {
        accountsById.clear();
        accountsByName.clear();
        sortedAccounts.clear();
        System.out.println("All accounts cleared");
        saveToFile();
    }

    public int getSize() {
        return accountsById.size();
    }

    // reading and writing from the files
    private void saveToFile() {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(ACCOUNT_FILE))) {
            for (LedgerAccount acc : accountsByName.values()) {
                String parentName = acc.getParent() != null ? acc.getParent().getName() : "";
                pw.printf("%d,%s,%s,%s\n", acc.getId(), acc.getName(), acc.getType(), parentName);
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to save accounts: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        java.io.File f = new java.io.File(ACCOUNT_FILE);
        if (!f.exists()) return;
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f))) {
            String line;
            java.util.List<String[]> rows = new java.util.ArrayList<>();
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 4) rows.add(parts);
            }
            java.util.Map<String, LedgerAccount> temp = new java.util.HashMap<>();
            for (String[] parts : rows) {
                int id = Integer.parseInt(parts[0]);
                String name = parts[1];
                String type = parts[2];
                LedgerAccount acc;
                if (id > 0) acc = new LedgerAccount(id, name, type);
                else acc = new LedgerAccount(name, type);
                temp.put(name, acc);
            }
            for (String[] parts : rows) {
                String name = parts[1];
                String parentName = parts[3];
                LedgerAccount acc = temp.get(name);
                if (parentName != null && !parentName.isEmpty()) {
                    LedgerAccount parent = temp.get(parentName);
                    if (parent != null) {
                        acc.setParent(parent);
                        parent.addSubaccount(acc);
                    }
                }
                addAccount(acc);
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load accounts: " + e.getMessage());
        }
    }
}