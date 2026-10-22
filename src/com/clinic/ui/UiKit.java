package com.clinic.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/** Small reusable widgets shared by the Search and Records pages. */
public final class UiKit {

    public static final Color AVATAR_BLUE = new Color(0x2B4C8C);

    private UiKit() {}

    // ------------------------------------------------------------------
    // Text helpers
    // ------------------------------------------------------------------

    /** Escapes text so it can be placed inside a JLabel's HTML. */
    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** "Oct. 23, 2026" (May has no period). */
    public static String formatDate(LocalDate d) {
        String shortName = d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        String fullName = d.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return shortName + (shortName.equals(fullName) ? "" : ".") + " " + d.getDayOfMonth() + ", " + d.getYear();
    }

    // ------------------------------------------------------------------
    // Card: rounded panel with fill and optional outline
    // ------------------------------------------------------------------

    public static class Card extends JPanel {
        private final int arc;
        private Color fill;
        private Color outline;
        private float outlineWidth = 1f;

        public Card(int arc, Color fill, Color outline) {
            this.arc = arc;
            this.fill = fill;
            this.outline = outline;
            setOpaque(false);
        }

        public void setFill(Color fill) {
            this.fill = fill;
            repaint();
        }

        public void setOutline(Color outline, float width) {
            this.outline = outline;
            this.outlineWidth = width;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            if (outline != null) {
                int inset = (int) Math.ceil(outlineWidth / 2);
                g2.setColor(outline);
                g2.setStroke(new BasicStroke(outlineWidth));
                g2.drawRoundRect(inset, inset, getWidth() - 1 - 2 * inset, getHeight() - 1 - 2 * inset, arc, arc);
            }
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Avatar: filled circle with an icon
    // ------------------------------------------------------------------

    public static class Avatar extends JComponent {
        private final int diameter;
        private final Color background;
        private final Icon icon;

        public Avatar(Icons.Type type, int diameter, Color background, Color iconColor) {
            this.diameter = diameter;
            this.background = background;
            this.icon = Icons.of(type, (int) Math.round(diameter * 0.58), iconColor);
            Dimension d = new Dimension(diameter, diameter);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(background);
            g2.fillOval(0, 0, diameter, diameter);
            icon.paintIcon(this, g2, (diameter - icon.getIconWidth()) / 2, (diameter - icon.getIconHeight()) / 2);
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Tag: outlined rounded label with an optional icon (e.g. "Pediatrics")
    // ------------------------------------------------------------------

    public static class Tag extends JComponent {
        private final String text;
        private final Icon icon;
        private final Color color;

        public Tag(String text, Icon icon, Color color) {
            this.text = text;
            this.icon = icon;
            this.color = color;
            setFont(Theme.font(Font.BOLD, 16));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int w = 16 + (icon != null ? icon.getIconWidth() + 8 : 0) + fm.stringWidth(text) + 16;
            return new Dimension(w, 38);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth(), h = getHeight();
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, h, h);

            int x = 16;
            if (icon != null) {
                icon.paintIcon(this, g2, x, (h - icon.getIconHeight()) / 2);
                x += icon.getIconWidth() + 8;
            }
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, x, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Pill: coloured rounded status label
    // ------------------------------------------------------------------

    public static class Pill extends JLabel {
        private Color fill = Color.LIGHT_GRAY;

        public Pill() {
            setFont(Theme.font(Font.BOLD, 13));
            setHorizontalAlignment(CENTER);
            setOpaque(false);
        }

        public void style(String text, Color fill, Color fg) {
            setText(text);
            this.fill = fill;
            setForeground(fg);
        }

        /** Colours by meaning: pending / completed / cancelled / available / on leave. */
        public void setStatus(String status) {
            String key = status == null ? "" : status.trim().toLowerCase();
            String label = key.isEmpty() ? "" : Character.toUpperCase(key.charAt(0)) + key.substring(1);
            switch (key) {
                case "completed":
                case "available":
                    style(label, Theme.DONE_BG, Theme.DONE_FG);
                    break;
                case "cancelled":
                case "on leave":
                    style(label, Theme.CANCELLED_BG, Theme.CANCELLED_FG);
                    break;
                case "pending":
                    style(label, Theme.PENDING_BG, Theme.PENDING_FG);
                    break;
                default:
                    style(label, new Color(0xE3E6E8), Theme.TEXT_MUTED);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width + 30, d.height + 10);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Table cell renderer that shows an appointment status as a Pill. */
    public static class StatusRenderer implements TableCellRenderer {
        private final JPanel holder = new JPanel(new GridBagLayout());
        private final Pill pill = new Pill();

        public StatusRenderer() {
            holder.setOpaque(true);
            holder.setBackground(Color.WHITE);
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.WEST;
            gc.weightx = 1;
            gc.insets = new Insets(0, 14, 0, 0);
            holder.add(pill, gc);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean foc, int r, int c) {
            pill.setStatus(String.valueOf(value));
            return holder;
        }
    }

    // ------------------------------------------------------------------
    // Table styling
    // ------------------------------------------------------------------

    public static void styleTable(JTable table) {
        table.setRowHeight(46);
        table.setFont(Theme.font(Font.PLAIN, 14));
        table.setForeground(Theme.TEXT_DARK);
        table.setBackground(Color.WHITE);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(0xE2E2E2));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setRowSelectionAllowed(false);
        table.setFocusable(false);
        table.setFillsViewportHeight(true);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
                l.setBorder(new EmptyBorder(0, 14, 0, 8));
                l.setForeground(Theme.TEXT_DARK);
                return l;
            }
        });

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setPreferredSize(new Dimension(100, 42));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
                l.setOpaque(true);
                l.setBackground(Theme.CONTENT_BG);
                l.setForeground(Theme.TEXT_DARK);
                l.setFont(Theme.font(Font.BOLD, 14));
                l.setBorder(new EmptyBorder(0, 14, 0, 8));
                return l;
            }
        });
    }
}