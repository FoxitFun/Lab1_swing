package pl.uczelnia.ekopanel.swing;

import javax.swing.*;
import java.awt.*;

public class KpiCardPanel extends JPanel {

    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel subTitleLabel;

    public KpiCardPanel(String title) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        titleLabel = new JLabel(title);
        valueLabel = new JLabel("—");
        subTitleLabel = new JLabel(" ");

        add(titleLabel);
        add(Box.createVerticalStrut(4));
        add(valueLabel);
        add(Box.createVerticalStrut(2));
        add(subTitleLabel);

        applyStyle();
    }

    public void setMetric(String formattedValue, String subtitle) {
        valueLabel.setText(formattedValue);
        subTitleLabel.setText(subtitle == null || subtitle.isEmpty() ? " " : subtitle);
    }

    /** Po zmianie motywu (updateComponentTreeUI) odświeżamy kolory obramowania i czcionki. */
    @Override
    public void updateUI() {
        super.updateUI();
        if (titleLabel != null) {
            SwingUtilities.invokeLater(this::applyStyle);
        }
    }

    private void applyStyle() {
        Color border = UIManager.getColor("Component.borderColor");
        if (border == null) border = Color.GRAY;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = new JLabel().getFont();
        titleLabel.setFont(base.deriveFont(Font.PLAIN, 12f));
        valueLabel.setFont(base.deriveFont(Font.BOLD, 18f));
        subTitleLabel.setFont(base.deriveFont(Font.PLAIN, 11f));
    }
}
