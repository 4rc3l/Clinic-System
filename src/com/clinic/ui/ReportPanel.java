package com.clinic.ui;

import com.clinic.service.ClinicManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * "Reports" page. Currently a placeholder landing spot for the
 * Generate Report button on the Appointments page; no report
 * generation logic yet.
 */
public class ReportPanel extends JPanel {

    public ReportPanel(ClinicManager clinic) {
        setLayout(new BorderLayout(0, 18));
        setBackground(Theme.CONTENT_BG);
        setBorder(new EmptyBorder(18, 22, 22, 22));
        add(UiKit.pageTopBar("Reports"), BorderLayout.NORTH);

        UiKit.Card card = new UiKit.Card(18, Color.WHITE, null);
        card.setLayout(new GridBagLayout());

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel(Icons.of(Icons.Type.REPORTS, 56, Theme.TEXT_MUTED));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Reports - coming soon");
        title.setFont(Theme.font(Font.BOLD, 20));
        title.setForeground(Theme.TEXT_DARK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Master patient list and daily appointment reports will appear here.");
        sub.setFont(Theme.font(Font.PLAIN, 14));
        sub.setForeground(Theme.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        box.add(icon);
        box.add(Box.createVerticalStrut(14));
        box.add(title);
        box.add(Box.createVerticalStrut(6));
        box.add(sub);
        card.add(box);
        add(card, BorderLayout.CENTER);
    }
}