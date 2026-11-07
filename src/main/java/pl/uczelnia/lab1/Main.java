package pl.uczelnia.lab1;

import com.formdev.flatlaf.FlatLightLaf;
import pl.uczelnia.ekopanel.repository.MemoryObservationRepository;
import pl.uczelnia.ekopanel.repository.ObservationRepository;
import pl.uczelnia.ekopanel.service.ObservationService;
import pl.uczelnia.ekopanel.swing.MainFrame;
import pl.uczelnia.ekopanel.swing.ThemeManager;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            ThemeManager.init();

            ObservationRepository repository = new MemoryObservationRepository();
            ObservationService service = new ObservationService(repository);

            new MainFrame(service).setVisible(true);
        });
    }
}
