package pl.uczelnia.ekopanel.swing;

import pl.uczelnia.ekopanel.model.Observation;
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

        JPanel dataView = new JPanel();
        JPanel chartView = new JPanel();
        JPanel calculatorView = new JPanel();

//            dataView.setBackground(Color.BLACK);
//            chartView.setBackground(Color.BLUE);

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
}
