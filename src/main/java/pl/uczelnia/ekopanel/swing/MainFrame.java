package pl.uczelnia.ekopanel.swing;

import pl.uczelnia.ekopanel.service.ObservationService;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private final ObservationService service;
    private final JLabel statusLabel;

    public MainFrame(ObservationService service){

        this.service = service;

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
        KpiCardPanel countCard = new KpiCardPanel("Liczba obserwacji");
        KpiCardPanel meanCard = new KpiCardPanel("Średnia (x̄)");
        KpiCardPanel medianCard = new KpiCardPanel("Mediana (Me)");
        KpiCardPanel sdCard = new KpiCardPanel("Odchylenie (s / d)");

        panel.add(navPanel, BorderLayout.NORTH);
        panel.add(cardPanel, BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);

        this.setContentPane(panel);

    }

    public void showStatus(String message){
        this.statusLabel.setText(message);
    }

}
