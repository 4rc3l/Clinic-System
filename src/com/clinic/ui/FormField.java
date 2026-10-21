package com.clinic.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.beans.PropertyChangeListener;

/**
 * Pill-shaped input row: [optional icon] [text field / combo box] [optional trailing component].
 * Highlights its outline while anything inside it has keyboard focus.
 */
public class FormField extends JPanel {

    private boolean focused;
    private final PropertyChangeListener focusWatcher = e -> {
        Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        boolean now = owner != null && SwingUtilities.isDescendingFrom(owner, this);
        if (now != focused) {
            focused = now;
            repaint();
        }
    };

    public FormField(Icon leftIcon, JComponent center, JComponent trailing) {
        setOpaque(false);
        setLayout(new BorderLayout(10, 0));
        setBorder(new EmptyBorder(0, 18, 0, trailing == null ? 14 : 8));
        setPreferredSize(new Dimension(100, 46));
        if (leftIcon != null) add(new JLabel(leftIcon), BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
        if (trailing != null) add(trailing, BorderLayout.EAST);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addPropertyChangeListener("permanentFocusOwner", focusWatcher);
    }

    @Override
    public void removeNotify() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .removePropertyChangeListener("permanentFocusOwner", focusWatcher);
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        int w = getWidth() - 1, h = getHeight() - 1;
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, w, h, h, h);
        g2.setColor(focused ? Theme.SIDEBAR_BG : Theme.BORDER);
        g2.setStroke(new BasicStroke(focused ? 1.8f : 1.2f));
        g2.drawRoundRect(0, 0, w, h, h, h);
        g2.dispose();
    }

    /** A borderless text field that shows grey hint text while empty. */
    public static JTextField textField(String hint) {
        JTextField f = new HintTextField(hint);
        f.setBorder(null);
        f.setOpaque(false);
        f.setFont(Theme.font(Font.PLAIN, 15));
        f.setForeground(Theme.TEXT_DARK);
        f.setCaretColor(Theme.SIDEBAR_BG);
        return f;
    }

    private static class HintTextField extends JTextField {
        private final String hint;

        HintTextField(String hint) {
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getDocument().getLength() > 0) return;
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(Theme.TEXT_MUTED);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(hint, getInsets().left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
}