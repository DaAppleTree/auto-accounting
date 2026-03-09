import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.List;
import javax.swing.JPanel;

// class for creating a graph panel analyzing account balance over time
public class GraphPanel extends JPanel {
    private final List<Ledger.BalancePoint> history;
    private long minEpoch;
    private long maxEpoch;
    private double minBal;
    private double maxBal;

    // constructor for graph panel
    public GraphPanel(List<Ledger.BalancePoint> history) {
        this.history = history;
        computeBounds();
    }

    // computes bounds for graph panel
    private void computeBounds() {
        if (history.isEmpty()) return;
        minEpoch = Long.MAX_VALUE;
        maxEpoch = Long.MIN_VALUE;
        minBal = Double.MAX_VALUE;
        maxBal = Double.MIN_VALUE;
        for (Ledger.BalancePoint bp : history) {
            long e = bp.date.toEpochDay();
            minEpoch = Math.min(minEpoch, e);
            maxEpoch = Math.max(maxEpoch, e);
            minBal = Math.min(minBal, bp.balance);
            maxBal = Math.max(maxBal, bp.balance);
        }
        double pad = (maxBal - minBal) * 0.1;
        if (pad == 0) pad = 1;
        minBal -= pad;
        maxBal += pad;
        if (minEpoch == maxEpoch) {
            minEpoch -= 1;
            maxEpoch += 1;
        }
    }

    @Override
    // paints the graph panel
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (history.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g;

        // nicer rendering
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        setBackground(Color.WHITE);

        // remember original font for later
        Font origFont = g2.getFont();
        int w = getWidth();
        int h = getHeight();
        int margin = 40;

        // draw axes
        g2.setColor(Color.BLACK);
        g2.drawLine(margin, h - margin, w - margin, h - margin);
        g2.drawLine(margin, margin, margin, h - margin);

        // x-axis label at right end of axis
        String xLabel = "Date";
        int xW = g2.getFontMetrics().stringWidth(xLabel);
        g2.drawString(xLabel, w - margin - xW / 2, h - 10);

        // y-axis label rotated and moved left of tick labels
        String yLabel = "Balance";
        AffineTransform orig = g2.getTransform();

        // translate to left of margin and center vertically
        int labelX = margin / 4;
        int labelY = h / 2;
        g2.translate(labelX, labelY);
        g2.rotate(-Math.PI / 2);
        g2.drawString(yLabel, 0, 0);
        g2.setTransform(orig);

        // draw ticks on y-axis
        int yTicks = 5;
        g2.setColor(Color.DARK_GRAY);
        Font tickFont = origFont.deriveFont(10f);
        g2.setFont(tickFont);
        for (int i = 0; i <= yTicks; i++) {
            double val = minBal + (maxBal - minBal) * i / yTicks;
            int y = h - margin - (int) ((val - minBal) / (maxBal - minBal) * (h - 2 * margin));
            g2.drawLine(margin - 5, y, margin, y);
            String label = String.format("%.2f", val);
            g2.drawString(label, 5, y + 5);
        }

        // ensure max value drawn at very top as a numeric label as well
        String topVal = String.format("%.2f", maxBal);
        g2.drawString(topVal, 5, margin - 5);

        // draw ticks on x-axis
        int desiredLabels = 4;
        int count = history.size();
        int step = Math.max(1, count / (desiredLabels - 1));
        for (int idx = 0; idx < count; idx += step) {
            Ledger.BalancePoint bp = history.get(idx);
            double frac = (bp.date.toEpochDay() - minEpoch) / (double) (maxEpoch - minEpoch);
            int x = margin + (int) (frac * (w - 2 * margin));
            g2.drawLine(x, h - margin, x, h - margin + 5);
            String label = bp.date.toString();
            int strW = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, x - strW / 2, h - margin + 20);
        }

        // intermediate ticks with no labels
        for (int idx = 0; idx < count; idx++) {
            if (idx % step != 0) {
                Ledger.BalancePoint bp = history.get(idx);
                double frac = (bp.date.toEpochDay() - minEpoch) / (double) (maxEpoch - minEpoch);
                int x = margin + (int) (frac * (w - 2 * margin));
                g2.drawLine(x, h - margin, x, h - margin + 3);
            }
        }
        g2.setFont(origFont);

        int pxPrev = 0, pyPrev = 0;
        boolean first = true;
        g2.setColor(Color.BLUE);
        for (Ledger.BalancePoint bp : history) {
            double fracX = (bp.date.toEpochDay() - minEpoch) / (double) (maxEpoch - minEpoch);
            double fracY = (bp.balance - minBal) / (maxBal - minBal);
            int px = margin + (int) (fracX * (w - 2 * margin));
            int py = h - margin - (int) (fracY * (h - 2 * margin));
            if (first) {
                first = false;
            } else {
                g2.drawLine(pxPrev, pyPrev, px, py);
            }
            g2.fillOval(px - 3, py - 3, 6, 6);
            pxPrev = px;
            pyPrev = py;
        }
    }
}
