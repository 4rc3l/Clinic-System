package com.clinic.ui;

import java.awt.*;

/**
 * Single place for the app's look. Every panel should use these
 * constants so changing the design later means editing only this file.
 */
public final class Theme {

    // ---- Colors (sampled from the login design) ----
    public static final Color PRIMARY         = new Color(0x22607A);
    public static final Color PRIMARY_HOVER   = new Color(0x1B4E64);
    public static final Color PRIMARY_PRESSED = new Color(0x143D4F);
    public static final Color BORDER          = new Color(0xD3D8DD);
    public static final Color CARD_BORDER     = new Color(0xA9B3BA);
    public static final Color FIELD_BG        = new Color(0xFAFAFA);
    public static final Color TEXT_MUTED      = new Color(0x6F8C9B);
    public static final Color ERROR           = new Color(0xC0392B);
    public static final Color WHITE           = Color.WHITE;

    // ---- Main window / dashboard ----
    public static final Color SIDEBAR_BG      = new Color(0x0E3C55);
    public static final Color SIDEBAR_ACTIVE  = new Color(0x66A9C6);
    public static final Color CONTENT_BG      = new Color(0xD9D9D9);
    public static final Color TEXT_DARK       = new Color(0x1B2A41);
    public static final Color CARD_STROKE     = new Color(0xC4C4C4);
    public static final Color TILE_BLUE       = new Color(0x6AA9C4);
    public static final Color TILE_YELLOW     = new Color(0xFFF29E);
    public static final Color TILE_GREEN      = new Color(0xC3F59B);

    // Status pills: background / text
    public static final Color PENDING_BG      = new Color(0xFFF0A3);
    public static final Color PENDING_FG      = new Color(0xE08A00);
    public static final Color ONGOING_BG      = new Color(0xB9DFF5);
    public static final Color ONGOING_FG      = new Color(0x1B6FA8);
    public static final Color DONE_BG         = new Color(0xBFF2A0);
    public static final Color DONE_FG         = new Color(0x1E7B1E);
    public static final Color CANCELLED_BG    = new Color(0xF8CFCA);
    public static final Color CANCELLED_FG    = new Color(0xB03A2E);

    // ---- Fonts ----
    // The design uses Inter. Java falls back to a default sans-serif if
    // the family isn't installed, so "Segoe UI" is a safe choice on Windows.
    public static final String FONT_FAMILY = "Segoe UI";

    public static Font font(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    // ---- Helpers ----
    public static void antialias(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    public static Cursor hand() {
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }

    private Theme() {}
}