package com.enc.pvpcod.ui;

import com.enc.packcod.ui.PvpTheme;
import com.enc.pvpcod.Hud;
import com.enc.pvpcod.module.HudModule;
import com.enc.pvpcod.module.Modules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Drag HUD boxes around. Scroll on one to resize it, right-click for its settings.
 * Boxes snap to the screen edges, the middle and each other (hold Shift to place freely).
 */
public final class HudEditorScreen extends UiScreen {

    private HudModule held;
    private int offX, offY;
    private Integer guideX, guideY;

    public HudEditorScreen(Screen parent) {
        super("Edit HUD", parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void backdrop(GuiGraphics g) {
        if (minecraft.level == null) g.fill(0, 0, width, height, PvpTheme.bg());
        else g.fill(0, 0, width, height, 0x30000000);
        // faint grid
        int c = 0x10FFFFFF;
        for (int x = 0; x < width; x += 20) g.fill(x, 0, x + 1, height, c);
        for (int y = 0; y < height; y += 20) g.fill(0, y, width, y + 1, c);
        g.fill(width / 2, 0, width / 2 + 1, height, 0x22FFFFFF);
        g.fill(0, height / 2, width, height / 2 + 1, 0x22FFFFFF);
    }

    private List<HudModule> shown() {
        List<HudModule> out = new ArrayList<>();
        for (HudModule m : Modules.hud()) if (m.enabled) out.add(m);
        return out;
    }

    private int[] rect(HudModule m) {
        int w = Math.round(m.width(font) * m.scale.value), h = Math.round(m.height(font) * m.scale.value);
        return new int[]{m.screenX(width, font), m.screenY(height, font), w, h};
    }

    private HudModule at(double mx, double my) {
        List<HudModule> list = shown();
        for (int i = list.size() - 1; i >= 0; i--) {
            int[] r = rect(list.get(i));
            if (in(mx, my, r[0], r[1], r[2], r[3])) return list.get(i);
        }
        return null;
    }

    @Override
    protected void draw(GuiGraphics g, int mx, int my) {
        HudModule hover = held != null ? held : at(mx, my);
        for (HudModule m : shown()) {
            int[] r = rect(m);
            Hud.drawAt(g, font, m, r[0], r[1], true);
            int col = m == hover ? PvpTheme.accent() : 0x60FFFFFF;
            PvpTheme.outline(g, r[0] - 1, r[1] - 1, r[2] + 2, r[3] + 2, col);
            if (m == hover) {
                String s = m.name + "  " + Math.round(m.scale.value * 100) + "%";
                int ty = r[1] - 11 < 2 ? r[1] + r[3] + 3 : r[1] - 11;
                g.fill(r[0] - 1, ty - 1, r[0] + font.width(s) + 3, ty + 9, PvpTheme.tip());
                g.drawString(font, s, r[0] + 1, ty, PvpTheme.text(), false);
            }
        }
        if (held != null) {
            if (guideX != null) g.fill(guideX, 0, guideX + 1, height, PvpTheme.accent());
            if (guideY != null) g.fill(0, guideY, width, guideY + 1, PvpTheme.accent());
        }

        // tool bar at the bottom middle
        String[] labels = {"Modules", "Reset all", "Done"};
        int tw = 8;
        for (String l : labels) tw += bw(l) + 4;
        int tx = (width - tw) / 2, ty = height - 30;
        boolean nearBar = my > height - 60 && held == null;
        if (nearBar || shown().isEmpty() || minecraft.level == null) {
            PvpTheme.panel(g, tx, ty - 2, tw, 22);
            int x = tx + 6;
            button(g, x, ty + 1, bw(labels[0]), 16, labels[0], mx, my, false, b -> minecraft.setScreen(new MenuScreen(this)));
            x += bw(labels[0]) + 4;
            button(g, x, ty + 1, bw(labels[1]), 16, labels[1], mx, my, false, b -> {
                for (HudModule m : Modules.hud()) {
                    m.resetPosition();
                    m.scale.reset();
                }
            });
            x += bw(labels[1]) + 4;
            button(g, x, ty + 1, bw(labels[2]), 16, labels[2], mx, my, true, b -> onClose());
        } else {
            String hint = "Drag to move  -  scroll to resize  -  right-click for settings  -  Shift: no snapping";
            g.drawString(font, hint, (width - font.width(hint)) / 2, height - 12, 0xA0FFFFFF, true);
        }
        if (shown().isEmpty()) {
            String s = "No HUD modules are on. Press Modules to switch some on.";
            g.drawString(font, s, (width - font.width(s)) / 2, height / 2 - 20, PvpTheme.text(), true);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // buttons first
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.in(event.x(), event.y()) && h.click() != null) {
                h.click().on(event.button());
                return true;
            }
        }
        HudModule m = at(event.x(), event.y());
        if (m == null) return super.mouseClicked(event, doubleClick);
        if (event.button() == 1) {
            minecraft.setScreen(new ModuleScreen(this, m));
            return true;
        }
        int[] r = rect(m);
        held = m;
        offX = (int) event.x() - r[0];
        offY = (int) event.y() - r[1];
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (held == null) return super.mouseDragged(event, dx, dy);
        int[] r = rect(held);
        int nx = (int) event.x() - offX, ny = (int) event.y() - offY;
        guideX = null;
        guideY = null;
        if (!event.hasShiftDown()) {
            int snap = 5;
            List<int[]> xs = new ArrayList<>(), ys = new ArrayList<>();
            // {candidate position, guide line}
            xs.add(new int[]{0, 0});
            xs.add(new int[]{width - r[2], width - 1});
            xs.add(new int[]{width / 2 - r[2] / 2, width / 2});
            ys.add(new int[]{0, 0});
            ys.add(new int[]{height - r[3], height - 1});
            ys.add(new int[]{height / 2 - r[3] / 2, height / 2});
            for (HudModule o : shown()) {
                if (o == held) continue;
                int[] q = rect(o);
                xs.add(new int[]{q[0], q[0]});
                xs.add(new int[]{q[0] + q[2] - r[2], q[0] + q[2]});
                xs.add(new int[]{q[0] + q[2] + 2, q[0] + q[2] + 1});
                xs.add(new int[]{q[0] - r[2] - 2, q[0] - 1});
                ys.add(new int[]{q[1], q[1]});
                ys.add(new int[]{q[1] + q[3] - r[3], q[1] + q[3]});
                ys.add(new int[]{q[1] + q[3] + 2, q[1] + q[3] + 1});
                ys.add(new int[]{q[1] - r[3] - 2, q[1] - 1});
            }
            int best = snap + 1;
            for (int[] c : xs) if (Math.abs(c[0] - nx) < best) {
                best = Math.abs(c[0] - nx);
                nx = c[0];
                guideX = c[1];
            }
            best = snap + 1;
            for (int[] c : ys) if (Math.abs(c[0] - ny) < best) {
                best = Math.abs(c[0] - ny);
                ny = c[0];
                guideY = c[1];
            }
        }
        held.moveTo(clamp(nx, 0, width - r[2]), clamp(ny, 0, height - r[3]), width, height, font);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (held != null) {
            held = null;
            guideX = guideY = null;
            Modules.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        HudModule m = at(mx, my);
        if (m == null) return super.mouseScrolled(mx, my, sx, sy);
        int[] before = rect(m);
        m.scale.set(m.scale.value + (sy > 0 ? 0.05f : -0.05f));
        // keep it roughly where it was
        m.moveTo(before[0], before[1], width, height, font);
        return true;
    }
}
