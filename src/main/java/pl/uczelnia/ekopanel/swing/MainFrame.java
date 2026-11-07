package pl.uczelnia.ekopanel.swing;

import pl.uczelnia.ekopanel.model.Indicator;
import pl.uczelnia.ekopanel.model.Observation;
import pl.uczelnia.ekopanel.model.Region;
import pl.uczelnia.ekopanel.model.Statistics;
import pl.uczelnia.ekopanel.service.ObservationService;
import pl.uczelnia.ekopanel.service.StatisticsService;

import java.math.BigDecimal;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame {
    private final ObservationService service;
    private final StatisticsService statisticsService;
    private final JLabel statusLabel;

    private final KpiCardPanel countCard;
    private final KpiCardPanel meanCard;
    private final KpiCardPanel medianCard;
    private final KpiCardPanel sdCard;

    public MainFrame(ObservationService service){

        this.service = service;
        this.statisticsService = new StatisticsService();

        this.setTitle("EkoPanel — GUS BDL");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1080, 720);
        this.setMinimumSize(new Dimension(480, 360));
        this.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel navPanel = new JPanel(new GridLayout(1, 4, 8, 8));

        JButton dataButton = new JButton("Dane");
        JButton chartsButton = new JButton("Wykres");
        JButton calcButton = new JButton("Kalkulator");
        JButton regionDetailsButton = new JButton("Szczegóły regionu…");
        navPanel.add(dataButton);
        navPanel.add(chartsButton);
        navPanel.add(calcButton);
        navPanel.add(regionDetailsButton);
        CardLayout cardLayout = new CardLayout();
        JPanel cardPanel = new JPanel(cardLayout);

        JPanel dataView = new JPanel(new BorderLayout(12, 12));

        JPanel formFields = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

//      Pola formularza
//        Wiersz 0
        JLabel yearLabel = new JLabel("Rok:");
        JTextField yearField = new JTextField("2025");
//        Wiersz 1
        JLabel regionLabel = new JLabel("Województwo:");
        JComboBox<Region> regionCombo = new JComboBox<>(
                service.findAllRegions().toArray(new Region[0])
        );
//        Wiersz 2
        JLabel indicatorLabel = new JLabel("Wskaźnik GUS:");
        JComboBox<Indicator> indicatorCombo = new JComboBox<>(
                service.findAllIndicators().toArray(new Indicator[0])
        );
//        Wiersz 3
        JLabel valueLabel = new JLabel("Wartość:");
        JTextField valueField = new JTextField("5,80");

//        Wiersz 4
        JCheckBox includedBox = new JCheckBox("Uwzględnij w analizie", true);

        addFormField(formFields, gbc, 0, yearLabel, yearField);
        addFormField(formFields, gbc, 1, regionLabel, regionCombo);
        addFormField(formFields, gbc, 2, indicatorLabel, indicatorCombo);
        addFormField(formFields, gbc, 3, valueLabel, valueField);

//        Wierasz 4, kolumna 0
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;

        formFields.add(includedBox, gbc);

//          Panel przycisków
        JPanel formBtnPanel = new JPanel(new GridLayout(1, 3, 8, 8));

        JButton newBtnForm = new JButton("Nowa");
        JButton saveBtnForm = new JButton("Zapisz");
        JButton deleteBtnForm = new JButton("Usuń");

        formBtnPanel.add(newBtnForm);
        formBtnPanel.add(saveBtnForm);
        formBtnPanel.add(deleteBtnForm);

//          Panel łączący pola i przyciski
        JPanel formPanel = new JPanel(new BorderLayout(8, 8));

        formPanel.add(formFields, BorderLayout.NORTH);
        formPanel.add(formBtnPanel, BorderLayout.SOUTH);

//          ScrollPane
        JScrollPane formScrollPane = new JScrollPane(formPanel);

        formScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        formScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        formScrollPane.setPreferredSize(new Dimension(800, 220));

        dataView.add(formScrollPane, BorderLayout.NORTH);


        JPanel chartView = new JPanel();
        JPanel calculatorView = new JPanel();

        cardPanel.add(dataView, "DATA");
        cardPanel.add(chartView, "CHART");
        cardPanel.add(calculatorView, "CALCULATOR");

        dataButton.addActionListener(e -> cardLayout.show(cardPanel, "DATA"));
        chartsButton.addActionListener(e -> cardLayout.show(cardPanel, "CHART"));
        calcButton.addActionListener(e -> cardLayout.show(cardPanel, "CALCULATOR"));

        this.statusLabel = new JLabel("Gotowe");

        JPanel statPanel = new JPanel(new GridLayout(1, 4, 8,8));
        this.countCard = new KpiCardPanel("Liczba obserwacji");
        this.meanCard = new KpiCardPanel("Średnia (x̄)");
        this.medianCard = new KpiCardPanel("Mediana (Me)");
        this.sdCard = new KpiCardPanel("Odchylenie (s / d)");

        statPanel.add(countCard);
        statPanel.add(meanCard);
        statPanel.add(medianCard);
        statPanel.add(sdCard);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));

        northPanel.add(navPanel);
        northPanel.add(statPanel);

        panel.add(northPanel, BorderLayout.NORTH);
        panel.add(cardPanel, BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);

        this.setContentPane(panel);
        this.refreshKpiCards();

    }

    public void showStatus(String message){
        this.statusLabel.setText(message);
    }

    public void refreshKpiCards(){
        List<Observation> observationResults = service.findAllObservations();
        Statistics stats = statisticsService.calculate(observationResults);

        countCard.setMetric(
                String.valueOf(stats.count()),
                ""
        );

        meanCard.setMetric(
                String.valueOf(stats.mean()),
                ""
        );

        medianCard.setMetric(
                String.valueOf(stats.median()),
                ""
        );

        sdCard.setMetric(
                String.valueOf(stats.sampleSd()),
                ""
        );
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, JLabel label, JComponent field){
//        Pozycja etykiety pola
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;

        panel.add(label,gbc);

//        Pozycja pola
        gbc.gridy = row;
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        panel.add(field, gbc);
    }
}
