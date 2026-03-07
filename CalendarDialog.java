import java.awt.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.Locale;
import javax.swing.*;

public class CalendarDialog extends JDialog {
    private LocalDate selectedDate;
    private YearMonth currentYearMonth;
    private JPanel calendarPanel;
    private JLabel monthLabel;

    public CalendarDialog(JFrame parent) {
        super(parent, "Select Date", true);
        selectedDate = LocalDate.now();
        currentYearMonth = YearMonth.now();
        setSize(400, 400);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        initUI();
        refreshCalendar();
    }

    private void initUI() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        JButton prevButton = new JButton("<");
        JButton nextButton = new JButton(">");
        
        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("Arial", Font.BOLD, 16));
        
        prevButton.addActionListener(e -> {
            currentYearMonth = currentYearMonth.minusMonths(1);
            refreshCalendar();
        });
        
        nextButton.addActionListener(e -> {
            currentYearMonth = currentYearMonth.plusMonths(1);
            refreshCalendar();
        });

        headerPanel.add(prevButton, BorderLayout.WEST);
        headerPanel.add(monthLabel, BorderLayout.CENTER);
        headerPanel.add(nextButton, BorderLayout.EAST);

        calendarPanel = new JPanel(new GridLayout(0, 7));
        add(headerPanel, BorderLayout.NORTH);
        add(calendarPanel, BorderLayout.CENTER);
    }

    private void refreshCalendar() {
        calendarPanel.removeAll();
        
        monthLabel.setText(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) 
                          + " " + currentYearMonth.getYear());

        for (DayOfWeek day : DayOfWeek.values()) {
            JLabel lbl = new JLabel(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()), 
                                   SwingConstants.CENTER);
            lbl.setFont(new Font("Arial", Font.BOLD, 12));
            calendarPanel.add(lbl);
        }

        LocalDate first = currentYearMonth.atDay(1);
        int startDay = first.getDayOfWeek().getValue() % 7; // Adjust for Sunday start
        
        for (int i = 1; i < startDay; i++) {
            calendarPanel.add(new JLabel(""));
        }

        int days = currentYearMonth.lengthOfMonth();
        for (int d = 1; d <= days; d++) {
            LocalDate date = currentYearMonth.atDay(d);
            JButton btn = new JButton(String.valueOf(d));
            
            if (date.equals(LocalDate.now())) {
                btn.setBackground(Color.CYAN);
            }
            
            btn.addActionListener(e -> {
                selectedDate = date;
                dispose();
            });
            calendarPanel.add(btn);
        }

        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }
}