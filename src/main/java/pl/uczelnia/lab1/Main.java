package pl.uczelnia.lab1;

import java.awt.*;

import javax.swing.*;

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
            
            frame.setVisible(true);
        });
    }
}
