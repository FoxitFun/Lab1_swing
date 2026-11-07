package pl.uczelnia.ekopanel.swing;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jthemedetecor.OsThemeDetector;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public final class ThemeManager {
    private static final String PREF_KEY = "theme";
    private static final Preferences PREFS = Preferences.userRoot().node("pl/uczelnia/ekopanel/swing");

    private static ThemeMode mode = ThemeMode.SYSTEM;
    private static OsThemeDetector detector;
    private static boolean listenerRegistered = false;
    private static final Consumer<Boolean> SYSTEM_LISTENER = dark -> SwingUtilities.invokeLater(() -> {
        if (mode == ThemeMode.SYSTEM) {
            applyLaf(dark);
        }
    });

    private ThemeManager() {
    }

    /** Wczytuje zapisany motyw i ustawia Look and Feel przed utworzeniem okien. */
    public static void init() {
        ThemeMode saved;
        try {
            saved = ThemeMode.valueOf(PREFS.get(PREF_KEY, ThemeMode.SYSTEM.name()));
        } catch (IllegalArgumentException e) {
            saved = ThemeMode.SYSTEM;
        }
        apply(saved);
    }

    public static ThemeMode getMode() {
        return mode;
    }

    public static boolean isDark() {
        return FlatLaf.isLafDark();
    }

    public static void apply(ThemeMode newMode) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> apply(newMode));
            return;
        }
        mode = newMode;
        PREFS.put(PREF_KEY, newMode.name());

        OsThemeDetector d = detector();
        // WindowsThemeDetector.removeListener() rzuca NPE, gdy nasłuch nie był wcześniej
        // zarejestrowany, dlatego rejestrujemy go tylko raz i sprawdzamy tryb w samym listenerze.
        if (d != null && !listenerRegistered) {
            try {
                d.registerListener(SYSTEM_LISTENER);
                listenerRegistered = true;
            } catch (RuntimeException e) {
                // brak obsługi zmian motywu systemowego na tej platformie
            }
        }
        switch (newMode) {
            case LIGHT -> applyLaf(false);
            case DARK -> applyLaf(true);
            case SYSTEM -> applyLaf(isSystemDark(d));
        }
    }

    private static void applyLaf(boolean dark) {
        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        // Odświeżenie wszystkich otwartych okien, w tym JDialog.
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            window.repaint();
        }
    }

    private static boolean isSystemDark(OsThemeDetector d) {
        try {
            return d != null && d.isDark();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static OsThemeDetector detector() {
        if (detector == null) {
            try {
                detector = OsThemeDetector.getDetector();
            } catch (RuntimeException | LinkageError e) {
                return null;
            }
        }
        return detector;
    }
}
