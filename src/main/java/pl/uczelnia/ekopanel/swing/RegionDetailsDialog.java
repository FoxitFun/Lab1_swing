package pl.uczelnia.ekopanel.swing;

import pl.uczelnia.ekopanel.model.Indicator;
import pl.uczelnia.ekopanel.model.Observation;
import pl.uczelnia.ekopanel.model.Region;
import pl.uczelnia.ekopanel.model.Statistics;
import pl.uczelnia.ekopanel.service.StatisticsService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RegionDetailsDialog extends JDialog {
    private static final int LAST_YEAR = 2024;

    public RegionDetailsDialog(Frame owner, Region region, List<Indicator> indicators,
                               List<Observation> regionObservations) {
        super(owner, "Szczegóły regionu: " + region.name(), true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // NORTH: kod TERYT/NTS, nazwa, makroregion
        JPanel header = new JPanel(new GridLayout(3, 1, 2, 2));
        JLabel nameLabel = new JLabel(region.name());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 16f));
        header.add(nameLabel);
        header.add(new JLabel("Kod TERYT/NTS: " + region.code()));
        header.add(new JLabel("Makroregion: " + region.macroregion()));
        content.add(header, BorderLayout.NORTH);

        // CENTER: tabela podsumowująca wskaźniki
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("pl-PL"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        String[] columns = {"Kod wskaźnika", "Nazwa", "Jednostka", "Liczba lat",
                "Średnia", "Min", "Max", "Ostatnia wartość (" + LAST_YEAR + ")"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        StatisticsService statisticsService = new StatisticsService();
        for (Indicator indicator : indicators) {
            List<Observation> rows = new ArrayList<>();
            BigDecimal last = null;
            for (Observation o : regionObservations) {
                if (o.indicatorCode().equals(indicator.code())) {
                    rows.add(o);
                    if (o.year() == LAST_YEAR) last = o.value();
                }
            }
            Statistics stats = statisticsService.calculate(rows);
            model.addRow(new Object[]{
                    indicator.code(), indicator.name(), indicator.unit(), rows.size(),
                    fmt(nf, stats.mean()), fmt(nf, stats.min()), fmt(nf, stats.max()), fmt(nf, last)
            });
        }
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        // SOUTH: Zamknij
        JButton closeButton = new JButton("Zamknij");
        closeButton.addActionListener(e -> dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(closeButton);
        content.add(south, BorderLayout.SOUTH);

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(closeButton);

        setContentPane(content);
        setSize(640, 340);
        setLocationRelativeTo(owner);
    }

    private static String fmt(NumberFormat nf, BigDecimal value) {
        return value == null ? "—" : nf.format(value);
    }
}
