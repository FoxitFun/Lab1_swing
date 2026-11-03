package pl.uczelnia.lab1;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
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
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

//            JPanel panel = new JPanel(new BorderLayout(8, 8));
//            JLabel message = new JLabel("Panel obserwacji ekonomicznych", SwingConstants.CENTER);
//            JButton button = new JButton("Przywitaj");
//            button.addActionListener(event -> message.setText("Interfejs reaguje na zdarzenie"));
//
//            panel.add(message, BorderLayout.CENTER);
//            panel.add(button, BorderLayout.SOUTH);
//            frame.setContentPane(panel);
        });
    }
}
