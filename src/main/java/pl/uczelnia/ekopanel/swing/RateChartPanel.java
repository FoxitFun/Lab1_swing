package pl.uczelnia.ekopanel.swing;

import pl.uczelnia.ekopanel.model.Indicator;
import pl.uczelnia.ekopanel.model.Observation;
import pl.uczelnia.ekopanel.model.Region;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class RateChartPanel extends JPanel {
    private static final Color LINE_LIGHT = Color.decode("#0F766E");
    private static final Color LINE_DARK = Color.decode("#5EEAD4");

    private Region region;
    private Indicator indicator;
    private List<Observation> rows = List.of();

    public RateChartPanel() {
        setPreferredSize(new Dimension(600, 360));
    }

    public void setSeries(Region region, Indicator indicator, List<Observation> rows) {
        this.region = region;
        this.indicator = indicator;
        List<Observation> sorted = new ArrayList<>();
        for (Observation row : rows) {
            if (row.value() != null) sorted.add(row);
        }
        sorted.sort(Comparator.comparingInt(Observation::year));
        this.rows = sorted;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color fg = UIManager.getColor("Label.foreground");
            if (fg == null) fg = Color.DARK_GRAY;
            Color grid = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 40);
            Color lineColor = ThemeManager.isDark() ? LINE_DARK : LINE_LIGHT;

            int w = getWidth();
            int h = getHeight();
            int left = 80, right = 30, top = 50, bottom = 50;
            int plotW = w - left - right;
            int plotH = h - top - bottom;

            // Tytuł
            g2.setColor(fg);
            g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
            String title = indicator == null ? "Brak wskaźnika"
                    : indicator.name() + " [" + indicator.unit() + "]"
                    + (region == null ? "" : " — " + region.name());
            g2.drawString(title, left, 28);

            if (plotW <= 10 || plotH <= 10) return;

            // Osie
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawLine(left, top, left, top + plotH);
            g2.drawLine(left, top + plotH, left + plotW, top + plotH);

            if (rows.isEmpty()) {
                g2.setFont(getFont());
                g2.drawString("Brak danych dla wybranego regionu i wskaźnika.", left + 10, top + plotH / 2);
                return;
            }

            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE, sum = 0;
            int minYear = Integer.MAX_VALUE, maxYear = Integer.MIN_VALUE;
            for (Observation row : rows) {
                double v = row.value().doubleValue();
                min = Math.min(min, v);
                max = Math.max(max, v);
                sum += v;
                minYear = Math.min(minYear, row.year());
                maxYear = Math.max(maxYear, row.year());
            }
            double mean = sum / rows.size();
            double pad = (max - min) * 0.1;
            if (pad == 0) pad = Math.max(Math.abs(max) * 0.1, 1);
            double yMin = Math.max(0, min - pad);
            double yMax = max + pad;

            NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL"));
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);

            // Siatka i etykiety osi Y
            g2.setFont(getFont().deriveFont(11f));
            FontMetrics fm = g2.getFontMetrics();
            int ticks = 5;
            for (int i = 0; i <= ticks; i++) {
                double v = yMin + (yMax - yMin) * i / ticks;
                int y = toY(v, yMin, yMax, top, plotH);
                g2.setColor(grid);
                g2.drawLine(left + 1, y, left + plotW, y);
                g2.setColor(fg);
                String label = nf.format(v);
                g2.drawString(label, left - 8 - fm.stringWidth(label), y + fm.getAscent() / 2 - 1);
            }

            // Linia średniej
            int meanY = toY(mean, yMin, yMax, top, plotH);
            g2.setColor(new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 140));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                    new float[]{6f, 4f}, 0f));
            g2.drawLine(left + 1, meanY, left + plotW, meanY);
            g2.drawString("x̄ = " + nf.format(mean), left + plotW - 90, meanY - 4);

            // Łamana
            Path2D path = new Path2D.Double();
            List<Point> points = new ArrayList<>();
            for (Observation row : rows) {
                int x = toX(row.year(), minYear, maxYear, left, plotW);
                int y = toY(row.value().doubleValue(), yMin, yMax, top, plotH);
                if (points.isEmpty()) path.moveTo(x, y);
                else path.lineTo(x, y);
                points.add(new Point(x, y));
            }
            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(path);

            for (int i = 0; i < points.size(); i++) {
                Point p = points.get(i);
                Observation row = rows.get(i);
                g2.setColor(lineColor);
                g2.fillOval(p.x - 4, p.y - 4, 8, 8);
                g2.setColor(fg);
                String year = String.valueOf(row.year());
                g2.drawString(year, p.x - fm.stringWidth(year) / 2, top + plotH + fm.getHeight() + 4);
                String value = nf.format(row.value());
                g2.drawString(value, p.x - fm.stringWidth(value) / 2, p.y - 8);
            }
        } finally {
            g2.dispose();
        }
    }

    private static int toX(int year, int minYear, int maxYear, int left, int plotW) {
        if (maxYear == minYear) return left + plotW / 2;
        int margin = 30;
        return left + margin + (int) Math.round((double) (year - minYear) / (maxYear - minYear) * (plotW - 2 * margin));
    }

    private static int toY(double value, double yMin, double yMax, int top, int plotH) {
        return top + plotH - (int) Math.round((value - yMin) / (yMax - yMin) * plotH);
    }
}
