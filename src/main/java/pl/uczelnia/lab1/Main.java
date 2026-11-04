package pl.uczelnia.lab1;

import java.awt.*;

import javax.swing.*;
import javax.swing.text.LabelView;
import java.util.List;

import com.formdev.flatlaf.FlatLightLaf;
import pl.uczelnia.ekopanel.model.Indicator;
import pl.uczelnia.ekopanel.model.Observation;
import pl.uczelnia.ekopanel.model.Region;
import pl.uczelnia.ekopanel.repository.ObservationRepository;
import pl.uczelnia.ekopanel.service.ObservationService;
import pl.uczelnia.ekopanel.repository.MemoryObservationRepository;
import pl.uczelnia.ekopanel.swing.MainFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            ObservationRepository repository = new MemoryObservationRepository();
            ObservationService service = new ObservationService(repository);
            MainFrame frame = new MainFrame(service);
            frame.setTitle("EkoPanel — GUS BDL");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1080, 720);
            frame.setMinimumSize(new Dimension(480, 360));
            JPanel panel = new JPanel(new BorderLayout(12, 12));
            panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            JPanel navPanel = new JPanel(new GridLayout(1, 4, 8, 8));

            JButton dataButton = new JButton("Dane");
            JButton chartsButton = new JButton("Wykres");
            JButton calcButton = new JButton("Kalkulator");
            JButton regionDetailsButtons = new JButton("Szczegóły regionu…");
            navPanel.add(dataButton);
            navPanel.add(chartsButton);
            navPanel.add(calcButton);
            navPanel.add(regionDetailsButtons);
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

            JLabel statusLabel = new JLabel("Gotowe");

            panel.add(navPanel, BorderLayout.NORTH);
            panel.add(cardPanel, BorderLayout.CENTER);
            panel.add(statusLabel, BorderLayout.SOUTH);

            frame.setContentPane(panel);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
