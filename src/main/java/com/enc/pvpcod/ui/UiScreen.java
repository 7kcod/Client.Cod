package com.enc.pvpcod.ui;

import com.enc.packcod.ui.PvpTheme;
import com.enc.pvpcod.PvpCod;
import com.enc.pvpcod.module.Modules;
import com.enc.pvpcod.module.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Base for every pvp.cod screen. Drawn in the same style as pack.cod (same palette and
 * shapes, from pack.cod's Look screen). Clickable areas are collected each frame while drawing.
 */
public abstract class UiScreen extends Screen {

    public interface Click {
        void on(int button);
    }

    public interface Drag {
        void to(double mx, double my);
    }

    public interface Scroll {
        void by(int dir);
    }

    record Hit(int x, int y, int w, int h, Click click, Drag drag) {
        boolean in(double mx, double my) {
            return mx >= x && my >= y && mx < x + w && my < y + h;
        }
    }

    record Wheel(int x, int y, int w, int h, Scroll scroll) {
        boolean in(double mx, double my) {
            return mx >= x && my >= y && mx < x + w && my < y + h;
        }
    }

    protected static final int TOP = 22;
    protected static final int M = 6;

    protected final Screen parent;
    protected final List<Hit> hits = new ArrayList<>();
    protected final List<Wheel> wheels = new ArrayList<>();
    protected Drag dragging;
    protected String tooltip;
    private static final ItemStack LOGO = new ItemStack(Items.COD);

    protected UiScreen(String title, Screen parent) {
        super(Component.literal(title));
        this.parent = parent;
        PvpTheme.refresh();
    }

    // ------------------------------------------------------------------ frame

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
        hits.clear();
        wheels.clear();
        tooltip = null;
        backdrop(g);
        draw(g, mx, my);
    }

    protected void backdrop(GuiGraphics g) {
        g.fill(0, 0, width, height, PvpTheme.bg());
    }

    protected abstract void draw(GuiGraphics g, int mx, int my);

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        if (tooltip != null) {
            g.nextStratum();
            int w = font.width(tooltip) + 8;
            int x = Math.max(2, Math.min(mx + 10, width - w - 2));
            int y = Math.max(2, my - 14);
            g.fill(x, y, x + w, y + 13, PvpTheme.tip());
            PvpTheme.outline(g, x, y, w, 13, PvpTheme.accentDark());
            g.drawString(font, tooltip, x + 4, y + 3, PvpTheme.text(), false);
        }
    }

    // ------------------------------------------------------------------ pieces

    /** The top bar: cod + "pvp.cod" + small version, then a page name. Returns where free space starts. */
    protected int topBar(GuiGraphics g, String page) {
        g.fill(0, 0, width, TOP, PvpTheme.panel());
        g.fill(0, TOP - 1, width, TOP, PvpTheme.line());
        g.renderItem(LOGO, M, 3);
        int tx = M + 19;
        g.drawString(font, "pvp", tx, 7, PvpTheme.text(), false);
        g.drawString(font, ".cod", tx + font.width("pvp"), 7, PvpTheme.accent(), false);
        String v = "v" + PvpCod.VERSION;
        int vx = tx + font.width("pvp.cod") + 4;
        g.drawString(font, v, vx, 7, PvpTheme.faint(), false);
        int px = vx + font.width(v) + 10;
        if (page != null && !page.isEmpty()) {
            g.fill(px - 5, 6, px - 4, 16, PvpTheme.line());
            g.drawString(font, page, px, 7, PvpTheme.dim(), false);
            px += font.width(page) + 10;
        }
        return px;
    }

    protected void button(GuiGraphics g, int x, int y, int w, int h, String text, int mx, int my, boolean primary, Click action) {
        button(g, x, y, w, h, text, mx, my, primary, true, action);
    }

    protected void button(GuiGraphics g, int x, int y, int w, int h, String text, int mx, int my, boolean primary, boolean enabled, Click action) {
        boolean hov = enabled && in(mx, my, x, y, w, h);
        PvpTheme.raised(g, x, y, w, h, hov, primary, enabled);
        int color = !enabled ? PvpTheme.faint() : primary ? PvpTheme.onAccent() : PvpTheme.text();
        String t = PvpTheme.fit(font, text, w - 4);
        g.drawString(font, t, x + (w - font.width(t)) / 2, y + (h - 8) / 2, color, false);
        if (enabled) hits.add(new Hit(x, y, w, h, action, null));
    }

    /** Width a button needs for its text. */
    protected int bw(String text) {
        return font.width(text) + 12;
    }

    /** On/off switch, 20x10. */
    protected void toggle(GuiGraphics g, int x, int y, boolean on, int mx, int my, Click action) {
        boolean hov = in(mx, my, x - 2, y - 2, 24, 14);
        int track = on ? PvpTheme.accent() : PvpTheme.mix(PvpTheme.slotColor(), PvpTheme.text(), hov ? 0.15f : 0.05f);
        PvpTheme.rounded(g, x, y, 20, 10, track, 4);
        int kx = on ? x + 11 : x + 1;
        PvpTheme.rounded(g, kx, y + 1, 8, 8, on ? PvpTheme.onAccent() : PvpTheme.dim(), 3);
        if (action != null) hits.add(new Hit(x - 2, y - 2, 24, 14, action, null));
    }

    /** A slider for a number setting. */
    protected void slider(GuiGraphics g, int x, int y, int w, Setting.Num s, int mx, int my, Runnable changed) {
        PvpTheme.slot(g, x, y + 3, w, 4);
        float t = (s.value - s.min) / (s.max - s.min);
        int fx = x + Math.round(t * (w - 6));
        g.fill(x + 1, y + 4, fx + 3, y + 6, PvpTheme.accentDark());
        boolean hov = in(mx, my, x - 2, y - 2, w + 4, 14);
        PvpTheme.rounded(g, fx, y, 6, 10, hov || dragging != null ? PvpTheme.mix(PvpTheme.accent(), 0xFFFFFFFF, 0.2f) : PvpTheme.accent(), 2);
        Drag d = (dmx, dmy) -> {
            float nt = (float) ((dmx - x - 3) / (w - 6));
            s.set(s.min + Math.max(0, Math.min(1, nt)) * (s.max - s.min));
            if (changed != null) changed.run();
        };
        hits.add(new Hit(x - 2, y - 2, w + 4, 14, null, d));
        wheels.add(new Wheel(x - 2, y - 2, w + 4, 14, dir -> {
            s.set(s.value - dir * s.step);
            if (changed != null) changed.run();
        }));
    }

    /** A colour swatch (checker behind it so see-through colours show). */
    protected void swatch(GuiGraphics g, int x, int y, int w, int h, int argb) {
        for (int yy = 0; yy < h; yy += 3) {
            for (int xx = 0; xx < w; xx += 3) {
                boolean dark = ((xx / 3) + (yy / 3)) % 2 == 0;
                g.fill(x + xx, y + yy, Math.min(x + w, x + xx + 3), Math.min(y + h, y + yy + 3), dark ? 0xFF555555 : 0xFF999999);
            }
        }
        g.fill(x, y, x + w, y + h, argb);
        PvpTheme.outline(g, x, y, w, h, PvpTheme.line());
    }

    protected void searchBox(EditBox box) {
        box.setBordered(false);
        box.setTextColor(PvpTheme.text());
    }

    protected void scrollbar(GuiGraphics g, int x, int y, int h, int scroll, int visible, int total) {
        if (total <= visible) return;
        int knobH = Math.max(10, h * visible / total);
        int knobY = y + (h - knobH) * scroll / Math.max(1, total - visible);
        g.fill(x, y, x + 2, y + h, PvpTheme.slotColor());
        g.fill(x, knobY, x + 2, knobY + knobH, PvpTheme.faint());
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        double mx = event.x(), my = event.y();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.in(mx, my)) {
                setFocused(null);
                if (h.drag() != null) {
                    dragging = h.drag();
                    dragging.to(mx, my);
                } else if (h.click() != null) {
                    h.click().on(event.button());
                }
                return true;
            }
        }
        setFocused(null);
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) {
            dragging.to(event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        for (int i = wheels.size() - 1; i >= 0; i--) {
            Wheel w = wheels.get(i);
            if (w.in(mx, my)) {
                w.scroll().by(sy > 0 ? -1 : sy < 0 ? 1 : 0);
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public void onClose() {
        Modules.save();
        minecraft.setScreen(parent);
    }

    protected static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    protected static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
