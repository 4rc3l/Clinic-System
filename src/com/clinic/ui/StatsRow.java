package com.clinic.ui;

import javax.swing.*;
import java.awt.*;

/**
 * The row of stat tiles + action button at the top of a page (Appointments, Payments).
 *
 * Every item gets an equal share of the page width, so the row always fills the panel and
 * grows / shrinks by itself when the window is resized or the sidebar opens and closes.
 * Pages just pass in their items; they no longer size or position anything themselves.
 */
public class StatsRow extends JPanel {

    private static final int GAP = 24;

    public StatsRow(JComponent... items) {
        super(new GridLayout(1, items.length, GAP, 0));
        setOpaque(false);
        for (JComponent item : items) add(item);
    }
}
