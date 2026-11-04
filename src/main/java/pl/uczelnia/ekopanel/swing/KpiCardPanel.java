package pl.uczelnia.ekopanel.swing;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;


public class KpiCardPanel extends JPanel {

    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel subTitleLabel;

    public KpiCardPanel(String title) {

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        this.titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));

        this.valueLabel = new JLabel("-");
        valueLabel.setFont(new Font("Arial", Font.PLAIN, 18));

        this.subTitleLabel = new JLabel();
        subTitleLabel.setFont(new Font("Arial", Font.PLAIN, 11));

        this.add(titleLabel);
        this.add(valueLabel);
        this.add(subTitleLabel);
    }

    public void setMetric(String formattedValue, String subtitle) {
        valueLabel.setText(formattedValue);
        subTitleLabel.setText(subtitle);
    }
}
