package com.clinic.ui;

import javax.swing.*;
import java.awt.*;

/**
 * A panel meant to sit inside a JScrollPane that only scrolls vertically.
 * It always matches the viewport's width, so its children never get
 * clipped sideways with no way to reach them.
 */
public class VScrollPanel extends JPanel implements Scrollable {

    public VScrollPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
    @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 18; }
    @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 120; }
}