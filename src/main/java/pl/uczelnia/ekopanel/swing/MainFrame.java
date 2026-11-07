package pl.uczelnia.ekopanel.swing;

import pl.uczelnia.ekopanel.io.GusCsvPackageReader;
import pl.uczelnia.ekopanel.model.Indicator;
import pl.uczelnia.ekopanel.model.Observation;
import pl.uczelnia.ekopanel.model.Region;
import pl.uczelnia.ekopanel.model.Statistics;
import pl.uczelnia.ekopanel.service.ChangeCalculator;
import pl.uczelnia.ekopanel.service.Numbers;
import pl.uczelnia.ekopanel.service.ObservationService;
import pl.uczelnia.ekopanel.service.StatisticsService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class MainFrame extends JFrame {
    private static final String CARD_DATA = "DATA";
    private static final String CARD_CHART = "CHART";
    private static final String CARD_CALCULATOR = "CALCULATOR";
    private static final String ALL_INDICATORS = "Wszystkie wskaźniki";
    private static final Locale PL = Locale.forLanguageTag("pl-PL");

    private final ObservationService service;
    private final StatisticsService statisticsService = new StatisticsService();
    private final ChangeCalculator changeCalculator = new ChangeCalculator();

    private final JLabel statusLabel = new JLabel("Gotowe.");
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    // Karty KPI
    private final JPanel kpiPanel = new JPanel(new GridLayout(1, 4, 8, 8));
    private final KpiCardPanel countCard = new KpiCardPanel("Liczba obserwacji");
    private final KpiCardPanel meanCard = new KpiCardPanel("Średnia (x̄)");
    private final KpiCardPanel medianCard = new KpiCardPanel("Mediana (Me)");
    private final KpiCardPanel sdCard = new KpiCardPanel("Odchylenie (s / d)");

    // Formularz
    private final JTextField yearField = new JTextField("2025");
    private final JComboBox<Region> regionCombo = new JComboBox<>();
    private final JComboBox<Indicator> indicatorCombo = new JComboBox<>();
    private final JTextField valueField = new JTextField("5,80");
    private final JCheckBox includedBox = new JCheckBox("Uwzględnij w analizie", true);
    private final JButton newButton = new JButton("Nowa");
    private final JButton saveButton = new JButton("Zapisz");
    private final JButton deleteButton = new JButton("Usuń");
    private Long editedId = null;

    // Tabela i filtry
    private final ObservationTableModel tableModel = new ObservationTableModel();
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<ObservationTableModel> sorter = new TableRowSorter<>(tableModel);
    private final JComboBox<Object> filterIndicatorCombo = new JComboBox<>();
    private final JTextField filterField = new JTextField(16);
    private final JButton importButton = new JButton("Importuj pakiet CSV");
    private JSplitPane splitPane;
    private int currentOrientation = -1;
    private boolean compactKpi = false;

    // Wykres
    private final JComboBox<Region> chartRegionCombo = new JComboBox<>();
    private final JComboBox<Indicator> chartIndicatorCombo = new JComboBox<>();
    private final RateChartPanel chartPanel = new RateChartPanel();

    // Kalkulator
    private final JTextField startField = new JTextField("4520,00", 10);
    private final JTextField endField = new JTextField("6980,00", 10);
    private final JLabel calcResultLabel = new JLabel(" ");

    // Menu
    private JMenuItem importMenuItem;
    private JMenuItem saveMenuItem;

    public MainFrame(ObservationService service) {
        this.service = service;

        setTitle("EkoPanel — GUS BDL");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 720);
        setMinimumSize(new Dimension(480, 360));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        root.add(buildNorth(), BorderLayout.NORTH);

        cardPanel.add(buildDataView(), CARD_DATA);
        cardPanel.add(buildChartView(), CARD_CHART);
        cardPanel.add(buildCalculatorView(), CARD_CALCULATOR);
        root.add(cardPanel, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);

        setContentPane(root);
        setJMenuBar(buildMenuBar());
        table.setComponentPopupMenu(buildPopupMenu());

        reloadDictionaries();
        refreshTable();

        getContentPane().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateResponsiveLayout();
            }
        });
        SwingUtilities.invokeLater(this::updateResponsiveLayout);
    }

    // ------------------------------------------------------------------ Budowa UI

    private JComponent buildNorth() {
        JPanel navPanel = new JPanel(new GridLayout(1, 4, 8, 8));
        JButton dataButton = new JButton("Dane");
        JButton chartButton = new JButton("Wykres");
        JButton calcButton = new JButton("Kalkulator");
        JButton regionDetailsButton = new JButton("Szczegóły regionu…");
        dataButton.addActionListener(e -> showCard(CARD_DATA));
        chartButton.addActionListener(e -> showCard(CARD_CHART));
        calcButton.addActionListener(e -> showCard(CARD_CALCULATOR));
        regionDetailsButton.addActionListener(e -> openRegionDetails());
        navPanel.add(dataButton);
        navPanel.add(chartButton);
        navPanel.add(calcButton);
        navPanel.add(regionDetailsButton);

        kpiPanel.add(countCard);
        kpiPanel.add(meanCard);
        kpiPanel.add(medianCard);
        kpiPanel.add(sdCard);

        JPanel north = new JPanel(new BorderLayout(8, 8));
        north.add(navPanel, BorderLayout.NORTH);
        north.add(kpiPanel, BorderLayout.CENTER);
        return north;
    }

    private JComponent buildDataView() {
        JPanel dataView = new JPanel(new BorderLayout(12, 12));

        // --- Formularz GridBagLayout
        JPanel formFields = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        addFormField(formFields, gbc, 0, new JLabel("Rok:"), yearField);
        addFormField(formFields, gbc, 1, new JLabel("Województwo:"), regionCombo);
        addFormField(formFields, gbc, 2, new JLabel("Wskaźnik GUS:"), indicatorCombo);
        addFormField(formFields, gbc, 3, new JLabel("Wartość:"), valueField);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        formFields.add(includedBox, gbc);

        JPanel formButtons = new JPanel(new GridLayout(1, 3, 8, 8));
        formButtons.add(newButton);
        formButtons.add(saveButton);
        formButtons.add(deleteButton);
        formButtons.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

        newButton.addActionListener(e -> newObservation());
        saveButton.addActionListener(e -> saveObservation());
        deleteButton.addActionListener(e -> deleteObservation());
        deleteButton.setEnabled(false);

        ScrollableFormPanel formPanel = new ScrollableFormPanel(new BorderLayout(8, 8));
        formPanel.add(formFields, BorderLayout.NORTH);
        JPanel buttonsWrapper = new JPanel(new BorderLayout());
        buttonsWrapper.add(formButtons, BorderLayout.NORTH);
        formPanel.add(buttonsWrapper, BorderLayout.CENTER);

        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        formScroll.setMinimumSize(new Dimension(200, 120));

        // --- Tabela
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(false);
        table.getColumnModel().getColumn(ObservationTableModel.COL_VALUE)
                .setCellRenderer(new DecimalRenderer());

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends ObservationTableModel, ? extends Integer> entry) {
                return matchesFilter(entry.getModel().getRow(entry.getIdentifier()));
            }
        });
        sorter.addRowSorterListener(e -> refreshKpiCards());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            onTableSelection();
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selectRowUnderPointer(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                selectRowUnderPointer(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)
                        && table.rowAtPoint(e.getPoint()) >= 0) {
                    openRegionDetails();
                }
            }
        });

        // --- Pasek filtrów
        filterIndicatorCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Object shown = value instanceof Indicator ind ? ind.code() + " — " + ind.name() : value;
                return super.getListCellRendererComponent(list, shown, index, isSelected, cellHasFocus);
            }
        });
        filterIndicatorCombo.setPrototypeDisplayValue("UNEMP_RATE — Stopa bezrobocia");
        filterIndicatorCombo.addActionListener(e -> applyFilter());
        filterField.setMinimumSize(new Dimension(90, filterField.getPreferredSize().height));
        filterField.putClientProperty("JTextField.placeholderText", "Szukaj województwa lub kodu");
        filterField.setToolTipText("Szukaj województwa lub kodu");
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        importButton.addActionListener(e -> importPackage());

        JPanel filterBar = new JPanel(new GridBagLayout());
        GridBagConstraints f = new GridBagConstraints();
        f.insets = new Insets(0, 0, 0, 8);
        f.fill = GridBagConstraints.HORIZONTAL;
        f.gridx = 0;
        f.weightx = 0.5;
        filterBar.add(filterIndicatorCombo, f);
        f.gridx = 1;
        f.weightx = 0.5;
        filterBar.add(filterField, f);
        f.gridx = 2;
        f.weightx = 0;
        f.insets = new Insets(0, 0, 0, 0);
        filterBar.add(importButton, f);

        JPanel tablePanel = new JPanel(new BorderLayout(8, 8));
        tablePanel.add(filterBar, BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        tablePanel.setMinimumSize(new Dimension(200, 120));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formScroll, tablePanel);
        splitPane.setResizeWeight(0.32);
        splitPane.setContinuousLayout(true);
        dataView.add(splitPane, BorderLayout.CENTER);
        return dataView;
    }

    private JComponent buildChartView() {
        JPanel chartView = new JPanel(new BorderLayout(8, 8));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.add(new JLabel("Województwo:"));
        bar.add(chartRegionCombo);
        bar.add(new JLabel("Wskaźnik:"));
        bar.add(chartIndicatorCombo);
        chartRegionCombo.addActionListener(e -> refreshChart());
        chartIndicatorCombo.addActionListener(e -> refreshChart());
        chartView.add(bar, BorderLayout.NORTH);
        chartView.add(chartPanel, BorderLayout.CENTER);
        return chartView;
    }

    private JComponent buildCalculatorView() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Wartość początkowa (x₀):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        form.add(startField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("Wartość końcowa (x₁):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        form.add(endField, gbc);

        JButton computeButton = new JButton("Oblicz");
        computeButton.addActionListener(e -> computeChange());
        JButton fromChartButton = new JButton("Pobierz 2020 i 2024 z wykresu");
        fromChartButton.addActionListener(e -> fillCalculatorFromChart());
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 8));
        buttons.add(computeButton);
        buttons.add(fromChartButton);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        form.add(buttons, gbc);

        calcResultLabel.setFont(calcResultLabel.getFont().deriveFont(Font.BOLD, 14f));
        gbc.gridy = 3;
        form.add(calcResultLabel, gbc);

        JLabel formulas = new JLabel("<html>Δx = x₁ − x₀ &nbsp;·&nbsp; r = (x₁ − x₀) / x₀ · 100% "
                + "&nbsp;·&nbsp; i₁/₀ = x₁ / x₀ · 100%</html>");
        gbc.gridy = 4;
        form.add(formulas, gbc);

        JPanel calculatorView = new JPanel(new BorderLayout());
        calculatorView.add(form, BorderLayout.NORTH);
        return calculatorView;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Plik");
        fileMenu.setMnemonic(KeyEvent.VK_P);
        importMenuItem = menuItem("Importuj pakiet GUS CSV…", KeyEvent.VK_O, e -> importPackage());
        fileMenu.add(importMenuItem);
        fileMenu.add(menuItem("Nowa obserwacja", KeyEvent.VK_N, e -> newObservation()));
        saveMenuItem = menuItem("Zapisz obserwację", KeyEvent.VK_S, e -> saveObservation());
        fileMenu.add(saveMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Zakończ", KeyEvent.VK_Q,
                e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING))));

        JMenu viewMenu = new JMenu("Widok");
        viewMenu.setMnemonic(KeyEvent.VK_W);
        viewMenu.add(menuItem("Dane i tabela", KeyEvent.VK_1, e -> showCard(CARD_DATA)));
        viewMenu.add(menuItem("Wykres serii czasowej", KeyEvent.VK_2, e -> showCard(CARD_CHART)));
        viewMenu.add(menuItem("Kalkulator dynamiki", KeyEvent.VK_3, e -> showCard(CARD_CALCULATOR)));
        viewMenu.addSeparator();
        viewMenu.add(menuItem("Szczegóły wybranego regionu…", KeyEvent.VK_I, e -> openRegionDetails()));

        JMenu themeMenu = new JMenu("Motyw");
        themeMenu.setMnemonic(KeyEvent.VK_M);
        ButtonGroup themeGroup = new ButtonGroup();
        addThemeItem(themeMenu, themeGroup, "Systemowy", ThemeMode.SYSTEM);
        addThemeItem(themeMenu, themeGroup, "Jasny", ThemeMode.LIGHT);
        addThemeItem(themeMenu, themeGroup, "Ciemny", ThemeMode.DARK);

        JMenu helpMenu = new JMenu("Pomoc");
        helpMenu.setMnemonic(KeyEvent.VK_O);
        JMenuItem aboutItem = new JMenuItem("O programie EkoPanel");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(themeMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private JPopupMenu buildPopupMenu() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem edit = new JMenuItem("Edytuj w formularzu");
        edit.addActionListener(e -> {
            onTableSelection();
            showCard(CARD_DATA);
            valueField.requestFocusInWindow();
        });
        JMenuItem details = new JMenuItem("Pokaż szczegóły regionu…");
        details.addActionListener(e -> openRegionDetails());
        JMenuItem chart = new JMenuItem("Pokaż ten region i wskaźnik na wykresie");
        chart.addActionListener(e -> showSelectedOnChart());
        JMenuItem delete = new JMenuItem("Usuń obserwację");
        delete.addActionListener(e -> deleteObservation());

        popup.add(edit);
        popup.add(details);
        popup.add(chart);
        popup.addSeparator();
        popup.add(delete);
        popup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                boolean selected = selectedObservation() != null;
                edit.setEnabled(selected);
                chart.setEnabled(selected);
                delete.setEnabled(selected);
            }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { }
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) { }
        });
        return popup;
    }

    private JMenuItem menuItem(String text, int keyCode, ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        item.setAccelerator(KeyStroke.getKeyStroke(keyCode, InputEvent.CTRL_DOWN_MASK));
        item.addActionListener(action);
        return item;
    }

    private void addThemeItem(JMenu menu, ButtonGroup group, String text, ThemeMode mode) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(text, ThemeManager.getMode() == mode);
        item.addActionListener(e -> {
            ThemeManager.apply(mode);
            chartPanel.repaint();
            showStatus("Motyw: " + text + ".");
        });
        group.add(item);
        menu.add(item);
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, JLabel label, JComponent field) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, gbc);
    }

    // ------------------------------------------------------------------ Odświeżanie

    public void showStatus(String message) {
        statusLabel.setText(message);
    }

    private void showCard(String key) {
        cardLayout.show(cardPanel, key);
        if (CARD_CHART.equals(key)) {
            refreshChart();
        }
    }

    /** Wypełnia wszystkie listy rozwijane słownikami z serwisu, zachowując wybór po kodzie. */
    private void reloadDictionaries() {
        List<Region> regions = service.findAllRegions();
        List<Indicator> indicators = service.findAllIndicators();

        fillCombo(regionCombo, regions, Region::code, "PL82");
        fillCombo(indicatorCombo, indicators, Indicator::code, "UNEMP_RATE");
        fillCombo(chartRegionCombo, regions, Region::code, "PL82");
        fillCombo(chartIndicatorCombo, indicators, Indicator::code, "UNEMP_RATE");

        Object previous = filterIndicatorCombo.getSelectedItem();
        String previousCode = previous instanceof Indicator ind ? ind.code()
                : previous == null ? "UNEMP_RATE" : null;
        DefaultComboBoxModel<Object> filterModel = new DefaultComboBoxModel<>();
        filterModel.addElement(ALL_INDICATORS);
        Object toSelect = ALL_INDICATORS;
        for (Indicator indicator : indicators) {
            filterModel.addElement(indicator);
            if (indicator.code().equals(previousCode)) {
                toSelect = indicator;
            }
        }
        filterModel.setSelectedItem(toSelect);
        filterIndicatorCombo.setModel(filterModel);

        tableModel.setDictionaries(regions, indicators);
    }

    private static <T> void fillCombo(JComboBox<T> combo, List<T> items, Function<T, String> code,
                                      String defaultCode) {
        Object previous = combo.getSelectedItem();
        @SuppressWarnings("unchecked")
        String wanted = previous == null ? defaultCode : code.apply((T) previous);
        DefaultComboBoxModel<T> model = new DefaultComboBoxModel<>();
        T toSelect = items.isEmpty() ? null : items.getFirst();
        for (T item : items) {
            model.addElement(item);
            if (code.apply(item).equals(wanted)) {
                toSelect = item;
            }
        }
        model.setSelectedItem(toSelect);
        combo.setModel(model);
    }

    private void refreshTable() {
        tableModel.setObservations(service.findAllObservations());
        refreshKpiCards();
        refreshChart();
    }

    private void applyFilter() {
        sorter.sort();
    }

    private boolean matchesFilter(Observation row) {
        Object selected = filterIndicatorCombo.getSelectedItem();
        if (selected instanceof Indicator indicator && !indicator.code().equals(row.indicatorCode())) {
            return false;
        }
        String query = filterField.getText().strip().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return true;
        }
        String name = tableModel.regionName(row.regionCode()).toLowerCase(Locale.ROOT);
        String code = row.regionCode().toLowerCase(Locale.ROOT);
        return name.contains(query) || code.contains(query);
    }

    /** Liczy statystyki dla wierszy widocznych w tabeli (po filtrze) i aktualizuje 4 karty KPI. */
    public void refreshKpiCards() {
        List<Observation> visible = new ArrayList<>();
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            visible.add(tableModel.getRow(table.convertRowIndexToModel(viewRow)));
        }
        Statistics stats = statisticsService.calculate(visible);

        Object selected = filterIndicatorCombo.getSelectedItem();
        String unit = selected instanceof Indicator ind ? ind.unit() : null;
        String unitSuffix = unit == null ? "" : " " + unit;
        String sdUnit = unit == null ? "" : " " + ("%".equals(unit) ? "p.p." : unit);
        String unitNote = unit == null ? "mieszane jednostki" : "jednostka: " + unit;

        countCard.setMetric("N = " + stats.count(),
                "widoczne z " + tableModel.getRowCount() + " w zbiorze");
        meanCard.setMetric(format(stats.mean()) + (stats.mean() == null ? "" : unitSuffix), unitNote);
        medianCard.setMetric(format(stats.median()) + (stats.median() == null ? "" : unitSuffix), unitNote);
        sdCard.setMetric("s = " + format(stats.sampleSd()) + (stats.sampleSd() == null ? "" : sdUnit),
                "d = " + format(stats.meanAbsDeviation())
                        + (stats.meanAbsDeviation() == null ? "" : sdUnit));
    }

    private void refreshChart() {
        Region region = (Region) chartRegionCombo.getSelectedItem();
        Indicator indicator = (Indicator) chartIndicatorCombo.getSelectedItem();
        List<Observation> series = new ArrayList<>();
        if (region != null && indicator != null) {
            for (Observation o : service.findAllObservations()) {
                if (o.regionCode().equals(region.code()) && o.indicatorCode().equals(indicator.code())) {
                    series.add(o);
                }
            }
        }
        chartPanel.setSeries(region, indicator, series);
    }

    private void updateResponsiveLayout() {
        int width = getContentPane().getWidth();
        if (width <= 0 || splitPane == null) {
            return;
        }
        int orientation = width < 760 ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT;
        if (orientation != currentOrientation) {
            currentOrientation = orientation;
            double ratio = orientation == JSplitPane.VERTICAL_SPLIT ? 0.45 : 0.32;
            splitPane.setOrientation(orientation);
            splitPane.setResizeWeight(ratio);
            SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(ratio));
        }

        boolean compact = width < 680;
        if (compact != compactKpi) {
            compactKpi = compact;
            kpiPanel.setLayout(compact ? new GridLayout(2, 2, 8, 8) : new GridLayout(1, 4, 8, 8));
            kpiPanel.revalidate();
        }
    }

    // ------------------------------------------------------------------ CRUD

    private Observation selectedObservation() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return tableModel.getRow(modelRow);
    }

    private void onTableSelection() {
        Observation row = selectedObservation();
        deleteButton.setEnabled(row != null);
        if (row == null) {
            editedId = null;
            return;
        }
        editedId = row.id();
        yearField.setText(String.valueOf(row.year()));
        selectByCode(regionCombo, row.regionCode(), Region::code);
        selectByCode(indicatorCombo, row.indicatorCode(), Indicator::code);
        valueField.setText(row.value() == null ? "" : row.value().toPlainString().replace('.', ','));
        includedBox.setSelected(row.included());
    }

    private static <T> void selectByCode(JComboBox<T> combo, String code, Function<T, String> codeOf) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (codeOf.apply(combo.getItemAt(i)).equals(code)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectRowUnderPointer(MouseEvent e) {
        if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
            int row = table.rowAtPoint(e.getPoint());
            if (row >= 0 && !table.isRowSelected(row)) {
                table.setRowSelectionInterval(row, row);
            }
        }
    }

    private void newObservation() {
        table.clearSelection();
        editedId = null;
        yearField.setText("2025");
        valueField.setText("");
        includedBox.setSelected(true);
        showCard(CARD_DATA);
        yearField.requestFocusInWindow();
        showStatus("Nowa obserwacja.");
    }

    private void saveObservation() {
        try {
            int year;
            try {
                year = Integer.parseInt(yearField.getText().strip());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Rok musi być liczbą całkowitą.");
            }
            Region region = (Region) regionCombo.getSelectedItem();
            Indicator indicator = (Indicator) indicatorCombo.getSelectedItem();
            if (region == null || indicator == null) {
                throw new IllegalArgumentException("Wybierz województwo i wskaźnik.");
            }
            BigDecimal value = Numbers.parseDecimal(valueField.getText());

            Observation saved = service.save(new Observation(editedId, year, region.code(),
                    indicator.code(), value, includedBox.isSelected()));

            refreshTable();
            selectById(saved.id());
            showStatus("Zapisano obserwację.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void selectById(Long id) {
        for (int modelRow = 0; modelRow < tableModel.getRowCount(); modelRow++) {
            if (tableModel.getRow(modelRow).id().equals(id)) {
                int viewRow = table.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                    table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                } else {
                    editedId = id;
                }
                return;
            }
        }
    }

    private void deleteObservation() {
        Observation row = selectedObservation();
        if (row == null || editedId == null) {
            return;
        }
        String question = "Usunąć obserwację " + row.year() + " / " + row.regionCode()
                + " / " + row.indicatorCode() + "?";
        int answer = JOptionPane.showConfirmDialog(this, question, "Potwierdzenie usunięcia",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            service.delete(editedId);
            editedId = null;
            refreshTable();
            showStatus("Usunięto obserwację.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        showStatus("Błąd: " + message);
        JOptionPane.showMessageDialog(this, message, "Błąd", JOptionPane.ERROR_MESSAGE);
    }

    // ------------------------------------------------------------------ Dialogi i wykres

    private Region currentRegion() {
        Observation row = selectedObservation();
        if (row != null) {
            for (Region region : service.findAllRegions()) {
                if (region.code().equals(row.regionCode())) {
                    return region;
                }
            }
        }
        return (Region) regionCombo.getSelectedItem();
    }

    private void openRegionDetails() {
        Region region = currentRegion();
        if (region == null) {
            showError("Brak wybranego województwa.");
            return;
        }
        List<Observation> regionObservations = new ArrayList<>();
        for (Observation o : service.findAllObservations()) {
            if (o.regionCode().equals(region.code())) {
                regionObservations.add(o);
            }
        }
        RegionDetailsDialog dialog = new RegionDetailsDialog(this, region,
                service.findAllIndicators(), regionObservations);
        dialog.setSize(640, 340);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showSelectedOnChart() {
        Observation row = selectedObservation();
        if (row == null) {
            return;
        }
        selectByCode(chartRegionCombo, row.regionCode(), Region::code);
        selectByCode(chartIndicatorCombo, row.indicatorCode(), Indicator::code);
        showCard(CARD_CHART);
    }

    private void showAbout() {
        String text = "<html><b>EkoPanel — GUS BDL</b><br><br>"
                + "Panel danych z Banku Danych Lokalnych GUS:<br>"
                + "5 województw (Region), 3 wskaźniki makroekonomiczne (Indicator)<br>"
                + "oraz obserwacje roczne 2020–2024 (Observation).<br><br>"
                + "Wskaźniki: stopa bezrobocia rejestrowanego (%),<br>"
                + "przeciętne wynagrodzenie brutto (PLN), nakłady inwestycyjne na 1 mieszkańca (PLN).<br><br>"
                + "Java: " + System.getProperty("java.version")
                + " (" + System.getProperty("java.vendor") + ")<br>"
                + "Interfejs: Swing + FlatLaf</html>";
        JOptionPane.showMessageDialog(this, text, "O programie EkoPanel", JOptionPane.INFORMATION_MESSAGE);
    }

    // ------------------------------------------------------------------ Kalkulator

    private void computeChange() {
        try {
            BigDecimal start = Numbers.parseDecimal(startField.getText());
            BigDecimal end = Numbers.parseDecimal(endField.getText());
            BigDecimal diff = changeCalculator.difference(start, end);
            BigDecimal rate = changeCalculator.relativeChange(start, end);
            BigDecimal index = changeCalculator.dynamicsIndex(start, end);
            calcResultLabel.setText("Przyrost: " + signed(diff) + "; tempo zmian: " + signed(rate)
                    + " %; indeks dynamiki: " + plain(index) + " %");
            showStatus("Obliczono dynamikę zmian.");
        } catch (IllegalArgumentException e) {
            calcResultLabel.setText(" ");
            showError(e.getMessage());
        }
    }

    private void fillCalculatorFromChart() {
        Region region = (Region) chartRegionCombo.getSelectedItem();
        Indicator indicator = (Indicator) chartIndicatorCombo.getSelectedItem();
        BigDecimal first = null, last = null;
        for (Observation o : service.findAllObservations()) {
            if (region != null && indicator != null && o.regionCode().equals(region.code())
                    && o.indicatorCode().equals(indicator.code())) {
                if (o.year() == 2020) first = o.value();
                if (o.year() == 2024) last = o.value();
            }
        }
        if (first == null || last == null) {
            showError("Brak obserwacji z lat 2020 i 2024 dla wybranej serii na wykresie.");
            return;
        }
        startField.setText(plain(first));
        endField.setText(plain(last));
        computeChange();
    }

    private static String plain(BigDecimal value) {
        return value.toPlainString().replace('.', ',');
    }

    private static String signed(BigDecimal value) {
        return (value.signum() > 0 ? "+" : "") + plain(value);
    }

    private static String format(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        NumberFormat nf = NumberFormat.getNumberInstance(PL);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(value);
    }

    // ------------------------------------------------------------------ Import CSV

    private record ImportResult(List<Region> regions, List<Indicator> indicators,
                                List<Observation> observations) {
    }

    private void importPackage() {
        JFileChooser chooser = new JFileChooser(defaultDataDir());
        chooser.setDialogTitle("Wybierz plik obserwacji GUS BDL");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Pliki CSV", "csv"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path observationsPath = chooser.getSelectedFile().toPath();
        List<Region> currentRegions = service.findAllRegions();
        List<Indicator> currentIndicators = service.findAllIndicators();

        setImportRunning(true);
        showStatus("Wczytywanie pakietu GUS…");

        new SwingWorker<ImportResult, Void>() {
            @Override
            protected ImportResult doInBackground() throws Exception {
                GusCsvPackageReader reader = new GusCsvPackageReader();
                Path dir = observationsPath.toAbsolutePath().getParent();
                Path regionsPath = dir.resolve("regions.csv");
                Path indicatorsPath = dir.resolve("indicators.csv");

                List<Region> regions = Files.isRegularFile(regionsPath)
                        ? reader.readRegions(regionsPath) : currentRegions;
                List<Indicator> indicators = Files.isRegularFile(indicatorsPath)
                        ? reader.readIndicators(indicatorsPath) : currentIndicators;

                Map<String, Region> regionsByCode = new LinkedHashMap<>();
                for (Region r : regions) regionsByCode.put(r.code(), r);
                Map<String, Indicator> indicatorsByCode = new LinkedHashMap<>();
                for (Indicator i : indicators) indicatorsByCode.put(i.code(), i);

                List<Observation> observations =
                        reader.readObservations(observationsPath, regionsByCode, indicatorsByCode);
                return new ImportResult(regions, indicators, observations);
            }

            @Override
            protected void done() {
                setImportRunning(false);
                try {
                    ImportResult result = get();
                    service.replaceDataset(result.regions(), result.indicators(), result.observations());
                    editedId = null;
                    reloadDictionaries();
                    refreshTable();
                    showStatus("Wczytano " + result.observations().size() + " obserwacji ("
                            + result.regions().size() + " regionów, "
                            + result.indicators().size() + " wskaźniki).");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    showError("Import przerwany. " + cause.getMessage()
                            + " Zachowano " + tableModel.getRowCount() + " obserwacji.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showError("Import przerwany.");
                } catch (IllegalArgumentException e) {
                    showError("Import przerwany. " + e.getMessage()
                            + " Zachowano " + tableModel.getRowCount() + " obserwacji.");
                }
            }
        }.execute();
    }

    private void setImportRunning(boolean running) {
        saveButton.setEnabled(!running);
        importButton.setEnabled(!running);
        if (saveMenuItem != null) saveMenuItem.setEnabled(!running);
        if (importMenuItem != null) importMenuItem.setEnabled(!running);
        setCursor(running ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    private static File defaultDataDir() {
        for (Path candidate : List.of(Path.of("data"), Path.of("ekopanel-swing", "data"))) {
            if (Files.isDirectory(candidate)) {
                return candidate.toAbsolutePath().toFile();
            }
        }
        return new File(System.getProperty("user.dir"));
    }

    // ------------------------------------------------------------------ Klasy pomocnicze

    /** Renderer kolumny Wartość: format pl-PL z dwoma miejscami po przecinku. */
    private static final class DecimalRenderer extends DefaultTableCellRenderer {
        private final NumberFormat format = NumberFormat.getNumberInstance(PL);

        DecimalRenderer() {
            format.setMinimumFractionDigits(2);
            format.setMaximumFractionDigits(2);
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        protected void setValue(Object value) {
            setText(value instanceof BigDecimal bd ? format.format(bd) : "");
        }
    }

    /** Panel formularza dopasowujący szerokość do JScrollPane (brak poziomego przewijania). */
    private static final class ScrollableFormPanel extends JPanel implements Scrollable {
        ScrollableFormPanel(LayoutManager layout) {
            super(layout);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
