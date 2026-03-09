import java.util.*;
import javax.swing.JOptionPane;

// class for creating a ledger
public class Ledger {
    private static final String ACCOUNT_FILE = "accounts.csv";
    public static java.util.ArrayList<LedgerAccount> sortedAccounts;
    public static java.util.HashMap<String, LedgerAccount> accountsByName;
    public static java.util.HashMap<Integer, LedgerAccount> accountsById;
    private final java.util.List<LedgerAccount> rootAccounts;

    // constructor for ledger
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
        normalizeTreeForRecursion();
    }

    // ensures each subtype account has been created
    private LedgerAccount ensureSubtypeNode(String nodeName, String type, LedgerAccount typeRoot) {
        LedgerAccount existing = accountsByName.get(nodeName);
        if (existing != null) {
            if (existing == typeRoot) {
                return existing;
            }
            if (existing.getParent() != typeRoot) {
                LedgerAccount oldParent = existing.getParent();
                if (oldParent != null) {
                    oldParent.removeSubaccount(existing);
                }
                typeRoot.addSubaccount(existing);
            }
            return existing;
        }
        return getOrCreateAbstractAccount(nodeName, type, typeRoot);
    }

    // finds the subtype accounts for each parent account
    private String inferSubtypeName(LedgerAccount account) {
        if (account == null) {
            return null;
        }
        String type = account.getType();
        String parentName = account.getParent() != null ? account.getParent().getName() : "";

        if ("Asset".equals(type)) {
            return "Fixed Asset".equalsIgnoreCase(parentName) ? "Fixed Asset" : "Current Asset";
        }
        if ("Liability".equals(type)) {
            return "Long-Term Liability".equalsIgnoreCase(parentName) ? "Long-Term Liability" : "Current Liability";
        }
        if ("Expense".equals(type)) {
            return parentName.toLowerCase().contains("other") ? "Other Expense" : "Operating Expense";
        }
        if ("Revenue".equals(type)) {
            return "Revenue";
        }
        if ("Equity".equals(type)) {
            return "Equity";
        }
        return null;
    }

    // creates the subtype account for each parent account
    private LedgerAccount resolveSubtypeParent(LedgerAccount account) {
        if (account == null || account.getId() <= 0) {
            return null;
        }
        LedgerAccount typeRoot = accountsByName.get(account.getType());
        if (typeRoot == null) {
            return null;
        }
        String subtypeName = inferSubtypeName(account);
        if (subtypeName == null || subtypeName.equals(typeRoot.getName())) {
            return typeRoot;
        }
        return ensureSubtypeNode(subtypeName, account.getType(), typeRoot);
    }

    // handles normalize tree for recursion behavior for ledger
    private void normalizeTreeForRecursion() {
        LedgerAccount assetRoot = getOrCreateAbstractAccount("Asset", "Asset", null);
        ensureSubtypeNode("Current Asset", "Asset", assetRoot);
        ensureSubtypeNode("Fixed Asset", "Asset", assetRoot);

        LedgerAccount liabilityRoot = getOrCreateAbstractAccount("Liability", "Liability", null);
        ensureSubtypeNode("Current Liability", "Liability", liabilityRoot);
        ensureSubtypeNode("Long-Term Liability", "Liability", liabilityRoot);

        LedgerAccount expenseRoot = getOrCreateAbstractAccount("Expense", "Expense", null);
        ensureSubtypeNode("Operating Expense", "Expense", expenseRoot);
        ensureSubtypeNode("Other Expense", "Expense", expenseRoot);

        LedgerAccount equityRoot = getOrCreateAbstractAccount("Equity", "Equity", null);
        ensureSubtypeNode("Equity", "Equity", equityRoot);

        LedgerAccount revenueRoot = getOrCreateAbstractAccount("Revenue", "Revenue", null);
        ensureSubtypeNode("Revenue", "Revenue", revenueRoot);

        for (LedgerAccount account : accountsById.values()) {
            LedgerAccount targetParent = resolveSubtypeParent(account);
            if (targetParent == null) {
                continue;
            }
            LedgerAccount oldParent = account.getParent();
            if (oldParent != null && oldParent != targetParent) {
                oldParent.removeSubaccount(account);
            }
            targetParent.addSubaccount(account);
        }
    }

    // creates root category used by this screen
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
        if (!accountsByName.containsKey("HST Recoverable")) {
            LedgerAccount assetCat = getOrCreateAbstractAccount("Asset", "Asset", null);
            addAccount(new LedgerAccount(701, "HST Recoverable", "Asset", assetCat));
        }
        if (!accountsByName.containsKey("HST Payable")) {
            LedgerAccount liabilityCat = getOrCreateAbstractAccount("Liability", "Liability", null);
            addAccount(new LedgerAccount(702, "HST Payable", "Liability", liabilityCat));
        }
    }

    // constructor to create an abstract account with no ID
    public LedgerAccount getOrCreateAbstractAccount(String name, String type, LedgerAccount parent) {
        LedgerAccount existing = accountsByName.get(name);
        if (existing != null) {
            return existing;
        }
        LedgerAccount acc = new LedgerAccount(-1, name, type, parent);
        addAccount(acc);
        return acc;
    }

    // adds account to the current state
    public void addAccount(LedgerAccount account) {
        if (account == null) {
            return;
        }

        // handle abstract accounts with no IDs
        if (account.getId() <= 0) {
            if (accountsByName.containsKey(account.getName())) {
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
            return;
        } else if (accountsById.containsKey(account.getId())) {
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
        LedgerAccount targetParent = resolveSubtypeParent(account);
        if (targetParent != null) {
            LedgerAccount oldParent = account.getParent();
            if (oldParent != null && oldParent != targetParent) {
                oldParent.removeSubaccount(account);
            }
            targetParent.addSubaccount(account);
        }
        saveToFile();
    }

    // finds position from current collections
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

    // finds accounts by prefix
    public List<LedgerAccount> findAccountsByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return new ArrayList<>();
        }
        
        // first try numeric search by ID prefix
        try {
            int multiplier = (int) Math.pow(10, 3 - prefix.length());
            int minId = Integer.parseInt(prefix) * multiplier;
            int maxId = minId + multiplier - 1;
            
            int startIndex = findFirstIndex(minId);
            int endIndex = findLastIndex(maxId);
            
            if (startIndex <= endIndex && startIndex >= 0 && endIndex < sortedAccounts.size()) {
                return sortedAccounts.subList(startIndex, endIndex + 1);
            }
        } catch (NumberFormatException e) {}
        
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

    // finds first index from current collections
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

    // finds last index from current collections
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

    // returns all accounts
    public ArrayList<LedgerAccount> getAllAccounts() {

        // return only leaf accounts with IDs
        ArrayList<LedgerAccount> list = new ArrayList<>();
        for (LedgerAccount acc : accountsById.values()) {
            list.add(acc);
        }
        return list;
    }

    // returns all accounts sorted by ID
    public ArrayList<LedgerAccount> getAllAccountsSorted() {
        return new ArrayList<>(sortedAccounts);
    }

    // updates from journal for consistency
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

    // recursive traversal helper for tree nodes
    private double sumBalancesRecursive(LedgerAccount node, Set<LedgerAccount> visiting) {
        if (node == null || visiting.contains(node)) {
            return 0;
        }
        visiting.add(node);
        double total = 0;
        if (node.getId() > 0) {
            total += node.getBalance();
        }
        for (LedgerAccount child : node.getSubaccounts()) {
            total += sumBalancesRecursive(child, visiting);
        }
        visiting.remove(node);
        return total;
    }

    // account total by any node name
    public double getRecursiveTotalByName(String accountName) {
        LedgerAccount node = accountsByName.get(accountName);
        if (node == null) {
            return 0;
        }
        return sumBalancesRecursive(node, new HashSet<>());
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

     // compute the running balance history for the account with the given ID, based on all transactions in the journal
    public List<BalancePoint> getBalanceHistory(int accountId, Journal journal) {
        LedgerAccount base = accountsById.get(accountId);
        if (base == null) {
            return new ArrayList<>();
        }
        List<Transaction> txns = new ArrayList<>(journal.getAllTransactions());
        txns.sort((a,b) -> a.getDate().compareTo(b.getDate()));

        // use a temporary account to accumulate balance according to normal side
        LedgerAccount temp = new LedgerAccount(base.getId(), base.getName(), base.getType());
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

    // returns trial balance used by other components
    public HashMap<String, Double> getTrialBalance() {
        HashMap<String, Double> trialBalance = new HashMap<>();
        for (LedgerAccount account : accountsById.values()) {
            trialBalance.put(account.getName(), account.getBalance());
        }
        return trialBalance;
    }

    // removes account from the current state
    public boolean removeAccount(int id) {
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
            return true;
        }
        return false;
    }

    // clears current input or stored data.
    public void clear() {
        accountsById.clear();
        accountsByName.clear();
        sortedAccounts.clear();
        saveToFile();
    }

    // saves to file in storage
    private void saveToFile() {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(ACCOUNT_FILE))) {
            for (LedgerAccount acc : accountsByName.values()) {
                String parentName = acc.getParent() != null ? acc.getParent().getName() : "";
                pw.printf("%d,%s,%s,%s\n", acc.getId(), acc.getName(), acc.getType(), parentName);
            }
        } catch (java.io.IOException e) {
            // error if file is unable to be saved
            JOptionPane.showMessageDialog(null, "Failed to save accounts file.", "Save Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // loads from file from storage
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
                else acc = new LedgerAccount(-1, name, type);
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
            // error if file is unable to be loaded
            JOptionPane.showMessageDialog(null, "Failed to load accounts file.", "Load Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
