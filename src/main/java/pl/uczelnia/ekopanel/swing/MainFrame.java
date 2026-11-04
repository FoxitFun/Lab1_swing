package pl.uczelnia.ekopanel.swing;

import com.formdev.flatlaf.FlatLightLaf;
import pl.uczelnia.ekopanel.service.ObservationService;

import javax.swing.*;

public class MainFrame extends JFrame {
    private final ObservationService service;

    public MainFrame(ObservationService service){
        this.service = service;
    }

    public void showStatus(String message){

    }

}
