import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import javax.swing.JOptionPane;

// class for creating a journal
public class Journal {
    private final TransactionList allTransactions;

    private static final String TRANSACTION_FILE = "transactions.csv";

    // constructor for an empty journal
    public Journal() {
        allTransactions = new TransactionList();
        loadFromFile();
    }

    // adds a transaction to the journal and saves it
    public void addTransaction(Transaction transaction) {
        if (transaction == null) {
            return;
        }
        allTransactions.addByDate(transaction);
        saveToFile();
    }

    // gets transaction at specific index
    public Transaction getTransaction(int index) {
        if (index < 0 || index >= size()) {
            return null;
        }

        Transaction current = allTransactions.getRoot();
        int i = 0;
        while (current != null) {
            if (i == index) {
                return current;
            }
            current = current.getNext();
            i++;
        }

        return null;
    }

    // deletes a transaction by its index; if it belongs to a group, deletes all entries in that group
    public boolean deleteTransaction(int index) {
        boolean removed = false;
        if (index >= 0 && index < size()) {
            Transaction t = getTransaction(index);
            if (t != null) {
                // remove the selected transaction
                allTransactions.remove(t);
                removed = true;

                // if the transaction is part of a group, also remove other group members
                int gid = t.getGroupId();
                if (gid != 0) {
                    Transaction current = allTransactions.getRoot();
                    while (current != null) {
                        Transaction next = current.getNext(); // save because we may remove
                        if (current.getGroupId() == gid) {
                            // remove this entry
                            allTransactions.remove(current);
                        }
                        current = next;
                    }
                }
            }
        }
        if (removed) saveToFile();
        return removed;
    }

    // returns true if the given transaction involves the account
    private boolean usesAccount(Transaction t, int accountId) {
        return t != null && (t.getDebitAccount() == accountId || t.getCreditAccount() == accountId);
    }

    // counts individual transaction lines that involve the given account
    public int countLinesForAccount(int accountId) {
        if (accountId <= 0) {
            return 0;
        }
        ArrayList<Transaction> txs = getAllTransactions();
        Set<Integer> groupedIds = new HashSet<>();
        int nonGroupedCount = 0;

        for (Transaction t : txs) {
            if (!usesAccount(t, accountId)) {
                continue;
            }
            if (t.getGroupId() != 0) {
                groupedIds.add(t.getGroupId());
            } else {
                nonGroupedCount++;
            }
        }

        int groupedLineCount = 0;
        if (!groupedIds.isEmpty()) {
            for (Transaction t : txs) {
                if (t.getGroupId() != 0 && groupedIds.contains(t.getGroupId())) {
                    groupedLineCount++;
                }
            }
        }
        return nonGroupedCount + groupedLineCount;
    }

    // counts logical journal entries (groups count as one) involving the given account
    public int countEntriesForAccount(int accountId) {
        if (accountId <= 0) {
            return 0;
        }
        ArrayList<Transaction> txs = getAllTransactions();
        Set<Integer> groupedIds = new HashSet<>();
        int nonGroupedEntries = 0;

        for (Transaction t : txs) {
            if (!usesAccount(t, accountId)) {
                continue;
            }
            if (t.getGroupId() != 0) {
                groupedIds.add(t.getGroupId());
            } else {
                nonGroupedEntries++;
            }
        }
        return nonGroupedEntries + groupedIds.size();
    }

    // deletes all transactions that involve the given account; returns how many were removed
    public int deleteEntriesForAccount(int accountId) {
        if (accountId <= 0) {
            return 0;
        }

        Set<Integer> groupedIds = new HashSet<>();
        Transaction current = allTransactions.getRoot();
        while (current != null) {
            if (usesAccount(current, accountId) && current.getGroupId() != 0) {
                groupedIds.add(current.getGroupId());
            }
            current = current.getNext();
        }

        int removedCount = 0;
        current = allTransactions.getRoot();
        while (current != null) {
            Transaction next = current.getNext();
            boolean shouldRemove;
            if (current.getGroupId() != 0) {
                shouldRemove = groupedIds.contains(current.getGroupId());
            } else {
                shouldRemove = usesAccount(current, accountId);
            }

            if (shouldRemove) {
                allTransactions.remove(current);
                removedCount++;
            }

            current = next;
        }

        if (removedCount > 0) {
            saveToFile();
        }
        return removedCount;
    }

    // returns all transactions
    public ArrayList<Transaction> getAllTransactions() {
        return allTransactions.toList();
    }

    // encodes an account-to-amount map as a bracket string, e.g. [101:50.00;201:50.00]
    private String encodeAmounts(LinkedHashMap<Integer, Double> accountMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (java.util.Map.Entry<Integer, Double> e : accountMap.entrySet()) {
            if (!first) {
                sb.append(";");
            }
            sb.append(e.getKey()).append(":").append(String.format(java.util.Locale.US, "%.2f", e.getValue()));
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    // parses a bracket string like [101:50.00;201:50.00] into an account-to-amount map
    private LinkedHashMap<Integer, Double> parseAmounts(String block) {
        LinkedHashMap<Integer, Double> out = new LinkedHashMap<>();
        if (block == null) return out;
        String trimmed = block.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return out;
        }
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) {
            return out;
        }
        String[] pairs = inner.split(";");
        for (String pair : pairs) {
            String p = pair.trim();
            if (p.isEmpty()) continue;
            String[] kv = p.split(":", 2);
            if (kv.length != 2) continue;
            try {
                int accountId = Integer.parseInt(kv[0].trim());
                double amount = Double.parseDouble(kv[1].trim());
                if (amount <= 0) continue;
                out.put(accountId, out.getOrDefault(accountId, 0.0) + amount);
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    // expands a debit/credit map pair into individual Transaction objects and adds them
    private void addEntry(LocalDate date, String desc, LinkedHashMap<Integer, Double> debits, LinkedHashMap<Integer, Double> credits, int gid) {
        if (debits.isEmpty() || credits.isEmpty()) {
            return;
        }

        ArrayList<java.util.Map.Entry<Integer, Double>> dList = new ArrayList<>(debits.entrySet());
        ArrayList<java.util.Map.Entry<Integer, Double>> cList = new ArrayList<>(credits.entrySet());

        int d = 0;
        int c = 0;
        double dRemain = dList.get(0).getValue();
        double cRemain = cList.get(0).getValue();

        while (d < dList.size() && c < cList.size()) {
            double amount = Math.min(dRemain, cRemain);
            if (amount > 0.0000001) {
                addWithoutSave(new Transaction(date, desc, dList.get(d).getKey(), cList.get(c).getKey(), amount, gid));
            }

            dRemain -= amount;
            cRemain -= amount;

            if (dRemain <= 0.0000001) {
                d++;
                if (d < dList.size()) {
                    dRemain = dList.get(d).getValue();
                }
            }
            if (cRemain <= 0.0000001) {
                c++;
                if (c < cList.size()) {
                    cRemain = cList.get(c).getValue();
                }
            }
        }
    }

    // adds a transaction without triggering a save (used during file loading)
    private void addWithoutSave(Transaction tx) {
        allTransactions.addByDate(tx);
    }

    // loads saved transactions from the CSV file
    private void loadFromFile() {
        java.io.File f = new java.io.File(TRANSACTION_FILE);
        if (!f.exists()) return;
        int malformedRows = 0;
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f))) {
            String line;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                // keep empty trailing fields if present
                String[] parts = trimmed.split(",", -1);
                try {
                    // reads the file in the format <date>,<desc>,[debitId:amount;...],[creditId:amount;...],<gid>
                    if (parts.length >= 5 && parts[2].trim().startsWith("[") && parts[3].trim().startsWith("[")) {
                        LocalDate date = LocalDate.parse(parts[0].trim());
                        String desc = parts[1].trim();
                        LinkedHashMap<Integer, Double> debits = parseAmounts(parts[2].trim());
                        LinkedHashMap<Integer, Double> credits = parseAmounts(parts[3].trim());
                        int gid = 0;
                        if (!parts[4].trim().isEmpty()) {
                            gid = Integer.parseInt(parts[4].trim());
                            if (gid > 0) {
                                Transaction.setMinGroupId(gid + 1);
                            }
                        }
                        addEntry(date, desc, debits, credits, gid);
                        continue;
                    }

                    LocalDate date = LocalDate.parse(parts[0].trim());
                    String desc = parts[1].trim();
                    int dr = Integer.parseInt(parts[2].trim());
                    int cr = Integer.parseInt(parts[3].trim());
                    double amt = Double.parseDouble(parts[4].trim());
                    int gid = 0;
                    if (parts.length >= 6 && !parts[5].trim().isEmpty()) {
                        gid = Integer.parseInt(parts[5].trim());
                        if (gid > 0) {
                            Transaction.setMinGroupId(gid + 1);
                        }
                    }
                    addWithoutSave(new Transaction(date, desc, dr, cr, amt, gid));
                } catch (RuntimeException parseError) {
                    malformedRows++;
                }
            }
        } catch (java.io.IOException e) {
            // error for if file is unable to be loaded
            JOptionPane.showMessageDialog(null, "Failed to load transactions from file:\n" + e.getMessage(), "Load Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // error for if rows in the file are malformed
        if (malformedRows > 0) {
            JOptionPane.showMessageDialog(null, "Loaded transactions, but skipped " + malformedRows + " malformed row(s).", "Partial Load Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    // saves grouped journal entries to the CSV file
    public void saveToFile() {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(TRANSACTION_FILE))) {
            // saves to the file using the format <date>,<desc>,[debitId:amount;...],[creditId:amount;...],<gid>
            ArrayList<Transaction> txs = getAllTransactions();
            LinkedHashMap<String, ArrayList<Transaction>> grouped = new LinkedHashMap<>();
            for (int i = 0; i < txs.size(); i++) {
                Transaction t = txs.get(i);
                String key;
                if (t.getGroupId() != 0) {
                    key = "G:" + t.getGroupId();
                } else {
                    key = "S:" + i;
                }
                if (!grouped.containsKey(key)) {
                    grouped.put(key, new ArrayList<>());
                }
                grouped.get(key).add(t);
            }

            for (ArrayList<Transaction> entryLines : grouped.values()) {
                if (entryLines.isEmpty()) {
                    continue;
                }
                Transaction first = entryLines.get(0);
                String desc = first.getDescription().replace(",", " ").replace("\n", " ").trim();
                int gid = first.getGroupId();

                LinkedHashMap<Integer, Double> debits = new LinkedHashMap<>();
                LinkedHashMap<Integer, Double> credits = new LinkedHashMap<>();
                for (Transaction line : entryLines) {
                    debits.put(line.getDebitAccount(), debits.getOrDefault(line.getDebitAccount(), 0.0) + line.getAmount());
                    credits.put(line.getCreditAccount(), credits.getOrDefault(line.getCreditAccount(), 0.0) + line.getAmount());
                }

                pw.printf("%s,%s,%s,%s,%d\n", first.getDate(), desc, encodeAmounts(debits), encodeAmounts(credits), gid);
            }
        } catch (java.io.IOException e) {
            // error for if the file is unable to be saved
            JOptionPane.showMessageDialog(null, "Failed to save transactions file.", "Save Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // checks whether transaction list is empty
    public boolean isEmpty() {
        return allTransactions.isEmpty();
    }

    // checks for size of transaction list
    public int size() {
        return allTransactions.getSize();
    }

    // clears all transactions
    public void clear() {
        allTransactions.clear();
        saveToFile();
    }
}

