import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class ReportsPage extends JPanel {
    private Journal journal;
    private Ledger ledger;
    private JTextArea reportArea;
    private List<FinancialRecord> records;

    public ReportsPage(Journal journal, Ledger ledger) {
        this.journal = journal;
        this.ledger = ledger;
        this.records = new ArrayList<>();
        // polymorphism: store different record types together
        records.add(new IncomeStatement(ledger));
        records.add(new BalanceSheet(ledger));
        
        setLayout(new BorderLayout());
        
        reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JButton printJournalBtn = new JButton("Print Journal");
        printJournalBtn.addActionListener(e -> printJournal());
        styleButton(printJournalBtn, new Color(96, 125, 139));

        JButton incomeStmtBtn = new JButton("Generate Income Statement");
        incomeStmtBtn.addActionListener(e -> showRecordByTitle("INCOME STATEMENT"));
        styleButton(incomeStmtBtn, new Color(46, 125, 50));

        JButton balanceSheetBtn = new JButton("Generate Balance Sheet");
        balanceSheetBtn.addActionListener(e -> showRecordByTitle("BALANCE SHEET"));
        styleButton(balanceSheetBtn, new Color(21, 101, 192));

        JButton allRecordsBtn = new JButton("Generate All Records");
        allRecordsBtn.addActionListener(e -> generateAllRecords());
        styleButton(allRecordsBtn, new Color(123, 31, 162));
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(printJournalBtn);
        buttonPanel.add(incomeStmtBtn);
        buttonPanel.add(balanceSheetBtn);
        buttonPanel.add(allRecordsBtn);
        
        add(new JScrollPane(reportArea), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void styleButton(JButton button, Color background) {
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
    }

    private void showRecordByTitle(String title) {
        ledger.updateFromJournal(journal);
        for (FinancialRecord record : records) {
            if (record.getTitle().equals(title)) {
                reportArea.setText(record.generate());
                return;
            }
        }
        reportArea.setText("Record type not found: " + title);
    }

    private void generateAllRecords() {
        ledger.updateFromJournal(journal);
        StringBuilder sb = new StringBuilder();
        for (FinancialRecord record : records) {
            sb.append(record.generate()).append("\n\n");
        }
        reportArea.setText(sb.toString());
    }

    private void printJournal() {
        reportArea.setText("JOURNAL ENTRIES\n");
        reportArea.append("==================\n\n");
        
        for (Transaction t : journal.getAllTransactions()) {
            reportArea.append(t.toString() + "\n");
        }
    }
}