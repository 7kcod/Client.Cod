package com.enc.packcod.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * pvp.cod's window into pack.cod's Theme, so both mods share one look.
 * Whatever palette, accent and style you pick on pack.cod's Look screen is used here too.
 * (Theme is package-private, so this lives in the same package.)
 */
public final class PvpTheme {

    public static void refresh() {
        Theme.apply();
    }

    public static int bg() { return Theme.BG; }
    public static int panel() { return Theme.PANEL; }
    public static int panelHi() { return Theme.PANEL_HI; }
    public static int line() { return Theme.LINE; }
    public static int slotColor() { return Theme.SLOT; }
    public static int text() { return Theme.TEXT; }
    public static int dim() { return Theme.DIM; }
    public static int faint() { return Theme.FAINT; }
    public static int accent() { return Theme.ACCENT; }
    public static int accentDark() { return Theme.ACCENT_DARK; }
    public static int onAccent() { return Theme.ON_ACCENT; }
    public static int hoverFill() { return Theme.HOVER_FILL; }
    public static int tip() { return Theme.TIP; }
    public static int good() { return Theme.GOOD; }
    public static int bad() { return Theme.BAD; }

    public static void panel(GuiGraphics g, int x, int y, int w, int h) { Theme.panel(g, x, y, w, h); }
    public static void slot(GuiGraphics g, int x, int y, int w, int h) { Theme.slot(g, x, y, w, h); }
    public static void raised(GuiGraphics g, int x, int y, int w, int h, boolean hover, boolean primary, boolean enabled) {
        Theme.raised(g, x, y, w, h, hover, primary, enabled);
    }
    public static void label(GuiGraphics g, Font font, String s, int x, int y) { Theme.label(g, font, s, x, y); }
    public static void outline(GuiGraphics g, int x, int y, int w, int h, int c) { Theme.outline(g, x, y, w, h, c); }
    public static void rounded(GuiGraphics g, int x, int y, int w, int h, int c, int r) { Theme.rounded(g, x, y, w, h, c, r); }
    public static int mix(int a, int b, float t) { return Theme.mix(a, b, t); }
    public static String fit(Font font, String s, int w) { return Theme.fit(font, s, w); }

    private PvpTheme() {
    }
}
