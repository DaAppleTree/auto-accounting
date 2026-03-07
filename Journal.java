import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class Journal {
    private TransactionList allTransactions;
    private HashMap<YearMonth, TransactionList> monthlyTransactions;

    // Constructor - creates empty journal
    private static final String TRANSACTION_FILE = "transactions.csv";

    public Journal() {
        allTransactions = new TransactionList();
        monthlyTransactions = new HashMap<>();
        loadFromFile();
    }

    public void addTransaction(Transaction transaction) {
        if (transaction == null) {
            return;
        }

        allTransactions.addByDate(transaction);

        YearMonth month = YearMonth.from(transaction.getDate());
        TransactionList monthList = monthlyTransactions.get(month);

        if (monthList == null) {
            monthList = new TransactionList();
            monthlyTransactions.put(month, monthList);
        }

        monthList.addByDate(transaction);
        saveToFile();
    }

    /**
     * Gets transaction at specific index (oldest to newest)
     */
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

    public boolean deleteTransactionByIndex(int index) {
        boolean removed = false;
        if (index >= 0 && index < size()) {
            Transaction t = getTransaction(index);
            if (t != null) {
                // remove the selected transaction
                allTransactions.remove(t);
                YearMonth month = YearMonth.from(t.getDate());
                TransactionList mlist = monthlyTransactions.get(month);
                if (mlist != null) {
                    mlist.remove(t);
                    if (mlist.isEmpty()) {
                        monthlyTransactions.remove(month);
                    }
                }
                removed = true;
                // if t is part of a group, also remove siblings
                int gid = t.getGroupId();
                if (gid != 0) {
                    Transaction current = allTransactions.getRoot();
                    while (current != null) {
                        Transaction next = current.getNext(); // save because we may remove
                        if (current.getGroupId() == gid) {
                            // remove this entry
                            allTransactions.remove(current);
                            YearMonth m2 = YearMonth.from(current.getDate());
                            TransactionList mlist2 = monthlyTransactions.get(m2);
                            if (mlist2 != null) {
                                mlist2.remove(current);
                                if (mlist2.isEmpty()) {
                                    monthlyTransactions.remove(m2);
                                }
                            }
                        }
                        current = next;
                    }
                }
            }
        }
        if (removed) saveToFile();
        return removed;
    }

    // ========== DATE-BASED QUERIES ==========

    /**
     * Gets all transactions on a specific date
     */
    public Journal getTransactionsByDate(LocalDate date) {
        Journal result = new Journal();

        for (Transaction current : getAllTransactions()) {
            if (current.getDate().equals(date)) {
                Transaction copy = new Transaction(
                    current.getDate(),
                    current.getDescription(),
                    current.getDebitAccount(),
                    current.getCreditAccount(),
                    current.getAmount()
                );
                result.addTransaction(copy);
            }
        }

        return result;
    }

    /**
     * Gets transactions within a date range
     */
    public Journal getTransactionsInRange(LocalDate start, LocalDate end) {
        Journal result = new Journal();

        for (Transaction current : getAllTransactions()) {
            LocalDate date = current.getDate();
            if (!date.isBefore(start) && !date.isAfter(end)) {
                Transaction copy = new Transaction(
                    current.getDate(),
                    current.getDescription(),
                    current.getDebitAccount(),
                    current.getCreditAccount(),
                    current.getAmount()
                );
                result.addTransaction(copy);
            }
        }

        return result;
    }

    public List<Transaction> getAllTransactions() {
        return allTransactions.toList();
    }

    public List<YearMonth> getMonthsNewestFirst() {
        ArrayList<YearMonth> months = new ArrayList<>(monthlyTransactions.keySet());
        Collections.sort(months, Collections.reverseOrder());
        return months;
    }

    public List<Transaction> getTransactionsForMonth(YearMonth month) {
        TransactionList list = monthlyTransactions.get(month);
        if (list == null) {
            return new ArrayList<>();
        }
        return list.toList();
    }

    // ========== GETTERS ==========

    // persistence helpers
    public void saveToFile() {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(TRANSACTION_FILE))) {
            // Write one logical entry per line using bracket format:
            // <date>,<description>,[debitId:amount;...],[creditId:amount;...],<groupId>
            List<Transaction> txs = getAllTransactions();
            LinkedHashMap<String, List<Transaction>> grouped = new LinkedHashMap<>();
            for (int i = 0; i < txs.size(); i++) {
                Transaction t = txs.get(i);
                String key = t.getGroupId() != 0 ? "G:" + t.getGroupId() : "S:" + i;
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
            }

            for (List<Transaction> entryLines : grouped.values()) {
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

                pw.printf("%s,%s,%s,%s,%d\n",
                    first.getDate(),
                    desc,
                    encodeAccountAmounts(debits),
                    encodeAccountAmounts(credits),
                    gid);
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to save transactions: " + e.getMessage());
        }
    }

    private String encodeAccountAmounts(LinkedHashMap<Integer, Double> accountMap) {
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

    private LinkedHashMap<Integer, Double> parseAccountAmounts(String block) {
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

    private void addEntryFromBracketFormat(LocalDate date, String desc,
                                           LinkedHashMap<Integer, Double> debits,
                                           LinkedHashMap<Integer, Double> credits,
                                           int gid) {
        if (debits.isEmpty() || credits.isEmpty()) {
            return;
        }

        List<java.util.Map.Entry<Integer, Double>> dList = new ArrayList<>(debits.entrySet());
        List<java.util.Map.Entry<Integer, Double>> cList = new ArrayList<>(credits.entrySet());

        int d = 0;
        int c = 0;
        double dRemain = dList.get(0).getValue();
        double cRemain = cList.get(0).getValue();

        while (d < dList.size() && c < cList.size()) {
            double amount = Math.min(dRemain, cRemain);
            if (amount > 0.0000001) {
                addLoadedTransaction(new Transaction(
                    date,
                    desc,
                    dList.get(d).getKey(),
                    cList.get(c).getKey(),
                    amount,
                    gid
                ));
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

    private void addLoadedTransaction(Transaction tx) {
        allTransactions.addByDate(tx);
        YearMonth month = YearMonth.from(tx.getDate());
        TransactionList monthList = monthlyTransactions.get(month);
        if (monthList == null) {
            monthList = new TransactionList();
            monthlyTransactions.put(month, monthList);
        }
        monthList.addByDate(tx);
    }

    private void loadFromFile() {
        java.io.File f = new java.io.File(TRANSACTION_FILE);
        if (!f.exists()) return;
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
                    // bracket format (primary):
                    // <date>,<desc>,[debitId:amount;...],[creditId:amount;...],<gid>
                    if (parts.length >= 5 && parts[2].trim().startsWith("[") && parts[3].trim().startsWith("[")) {
                        LocalDate date = LocalDate.parse(parts[0].trim());
                        String desc = parts[1].trim();
                        LinkedHashMap<Integer, Double> debits = parseAccountAmounts(parts[2].trim());
                        LinkedHashMap<Integer, Double> credits = parseAccountAmounts(parts[3].trim());
                        int gid = 0;
                        if (!parts[4].trim().isEmpty()) {
                            gid = Integer.parseInt(parts[4].trim());
                            if (gid > 0) {
                                Transaction.ensureNextGroupIdAtLeast(gid + 1);
                            }
                        }
                        addEntryFromBracketFormat(date, desc, debits, credits, gid);
                        continue;
                    }

                    // legacy format fallback:
                    // <date>,<desc>,<dr>,<cr>,<amount>[,<gid>]
                    if (parts.length < 5) {
                        System.err.println("Skipping invalid transaction row " + lineNumber + ": " + trimmed);
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
                            Transaction.ensureNextGroupIdAtLeast(gid + 1);
                        }
                    }
                    addLoadedTransaction(new Transaction(date, desc, dr, cr, amt, gid));
                } catch (RuntimeException parseError) {
                    System.err.println("Skipping malformed transaction row " + lineNumber + ": " + trimmed);
                }
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load transactions: " + e.getMessage());
        }
    }

    public Transaction getMostRecent() {
        Transaction current = allTransactions.getRoot();
        if (current == null) {
            return null;
        }

        while (current.getNext() != null) {
            current = current.getNext();
        }

        return current;
    }

    public Transaction getOldest() {
        return allTransactions.getRoot();
    }

    public boolean isEmpty() {
        return allTransactions.isEmpty();
    }

    public int size() {
        return allTransactions.getSize();
    }

    public Transaction getHead() {
        return getOldest();
    }

    public Transaction getTail() {
        return getMostRecent();
    }

    // ========== ITERATION HELPERS ==========

    /**
     * Returns all transactions as an array
     */
    public Transaction[] toArray() {
        List<Transaction> list = getAllTransactions();
        return list.toArray(new Transaction[0]);
    }

    // ========== DISPLAY METHODS ==========

    /**
     * Prints journal from oldest to newest
     */
    public void printJournal() {
        System.out.println("JOURNAL ENTRIES (Oldest to Newest)");
        System.out.println("==================================");

        if (isEmpty()) {
            System.out.println("No transactions recorded.");
            return;
        }

        int count = 1;
        for (Transaction transaction : getAllTransactions()) {
            System.out.println(count + ". " + transaction.toString());
            count++;
        }

        System.out.println("==================================");
        System.out.println("Total transactions: " + size());
    }

    /**
     * Prints journal from newest to oldest
     */
    public void printJournalReverse() {
        System.out.println("JOURNAL ENTRIES (Newest to Oldest)");
        System.out.println("==================================");

        if (isEmpty()) {
            System.out.println("No transactions recorded.");
            return;
        }

        List<Transaction> list = getAllTransactions();
        int count = 1;
        for (int i = list.size() - 1; i >= 0; i--) {
            System.out.println(count + ". " + list.get(i).toString());
            count++;
        }

        System.out.println("==================================");
        System.out.println("Total transactions: " + size());
    }

    /**
     * Prints detailed journal with pointer information
     */
    public void printDebug() {
        System.out.println("JOURNAL DEBUG (Oldest to Newest)");
        System.out.println("================================");

        if (isEmpty()) {
            System.out.println("Empty journal");
            return;
        }

        System.out.println("Oldest: " + getOldest().getDate());
        System.out.println("Most Recent: " + getMostRecent().getDate());
        System.out.println("Size: " + size());
        System.out.println();

        Transaction current = allTransactions.getRoot();
        while (current != null) {
            System.out.println(current.toDebugString());
            current = current.getNext();
        }
    }

    // ========== CLEAR METHOD ==========

    /**
     * Removes all transactions
     */
    public void clear() {
        allTransactions.clear();
        monthlyTransactions.clear();
        System.out.println("Journal cleared");
        saveToFile();
    }
}
