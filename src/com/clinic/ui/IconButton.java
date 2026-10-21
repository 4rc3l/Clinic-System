package com.clinic.ui;

import javax.swing.*;
import java.awt.*;

/** Flat button that shows only an icon, with a rounded highlight on hover. */
public class IconButton extends JButton {
    private final Color hoverColor;

    public IconButton(Icon icon, String tooltip, Color hoverColor) {
        super(icon);
        this.hoverColor = hoverColor;
        setToolTipText(tooltip);
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setCursor(Theme.hand());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (getModel().isRollover()) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(hoverColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
        }
        super.paintComponent(g);
    }
}