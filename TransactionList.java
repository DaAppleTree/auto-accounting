// class for creating a transaction linked list
public class TransactionList{
    private Transaction root;
    private int size;

    // constructor for a transaction list
    public TransactionList() {
        this.root = null;
        this.size = 0;
    }

    public Transaction getRoot() {return root;}
    
    public int getSize() {return size;}

    // checks if the linked list is empty
    public boolean isEmpty() {return root == null;}

    // checks if a given transaction exists
    public boolean isExist(Transaction newTransaction) {
        Transaction transaction = root;
        while (transaction != null) {
            if (transaction.equals(newTransaction)) {
                return true;
            }
            transaction = transaction.getNext();
        }
        return false;
    }

    // adds a transaction based on date, from oldest to newest
    public void addByDate(Transaction newTransaction) {
        if (isEmpty()) {
            root = newTransaction;
            this.size++;
        } else if (!isExist(newTransaction)) {
            if (newTransaction.isBefore(root)) {
                newTransaction.setNext(root);
                root = newTransaction;
            } else {
                Transaction transaction = root;
                while (transaction.hasNext() && !newTransaction.isBefore(transaction.getNext())) {
                    transaction = transaction.getNext();
                }
                newTransaction.setNext(transaction.getNext());
                transaction.setNext(newTransaction);
            }
            this.size++;
        }
    }
    
    // removes a transaction from the linked list
    public void remove(Transaction targetTransaction) {
        if (!isEmpty() && isExist(targetTransaction)){
            if (root.equals(targetTransaction)) {
                root = root.getNext();
            } else {
                Transaction transaction = root;
                while (transaction.hasNext() && !transaction.getNext().equals(targetTransaction)){
                    transaction = transaction.getNext();
                }
                transaction.setNext(transaction.getNext().getNext());
            }
            this.size--;
        }
    }

    // converts linked list to a list
    public java.util.ArrayList<Transaction> toList() {
        java.util.ArrayList<Transaction> list = new java.util.ArrayList<>();
        Transaction current = root;
        while (current != null) {
            list.add(current);
            current = current.getNext();
        }
        return list;
    }

    // clears current input or stored data
    public void clear() {
        root = null;
        size = 0;
    }
}

