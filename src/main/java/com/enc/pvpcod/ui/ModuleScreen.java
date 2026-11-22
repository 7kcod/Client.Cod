package com.enc.pvpcod.ui;

import com.enc.packcod.ui.PvpTheme;
import com.enc.pvpcod.Hud;
import com.enc.pvpcod.PvpCod;
import com.enc.pvpcod.module.HudModule;
import com.enc.pvpcod.module.Module;
import com.enc.pvpcod.module.Modules;
import com.enc.pvpcod.module.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/** Every setting of one module, each with its own control. */
public final class ModuleScreen extends UiScreen {

    private final Module module;
    private int scroll;
    /** Where the live window onto the game is (x, y, w, h), from the last frame. */
    private int[] live;

    public ModuleScreen(Screen parent, Module module) {
        super(module.name, parent);
        this.module = module;
    }

    /** Modules you can watch working live in the game behind this page. */
    private boolean liveModule() {
        return module == Modules.BLOCK_OUTLINE || module == Modules.HITBOXES || module == Modules.FULLBRIGHT
                || module == Modules.CLEAR_WATER || module == Modules.CLEAR_LAVA || module == Modules.LOW_FIRE
                || module == Modules.MOTION_BLUR || module == Modules.CROSSHAIR || module == Modules.ASPECT
                || module == Modules.CLEAN_HUD || module == Modules.NO_PUMPKIN || module == Modules.ITEM_SCALE
                || module == Modules.BOAT_MAP || module == Modules.RIPTIDE_SHIELD;
    }

    private boolean showLive() {
        return minecraft != null && minecraft.level != null && liveModule();
    }

    @Override
    public boolean isPauseScreen() {
        // keep the world running so the live view moves
        return !showLive();
    }

    @Override
    protected void backdrop(GuiGraphics g) {
        if (!showLive() || live == null) {
            super.backdrop(g);
            return;
        }
        // everything except the live window
        int x = live[0], y = live[1], w = live[2], h = live[3], c = PvpTheme.bg();
        g.fill(0, 0, width, y, c);
        g.fill(0, y + h, width, height, c);
        g.fill(0, y, x, y + h, c);
        g.fill(x + w, y, width, y + h, c);
    }

    private List<Setting> rows() {
        List<Setting> out = new ArrayList<>();
        for (Setting s : module.settings) if (!(s instanceof Setting.Text)) out.add(s);
        return out;
    }

    @Override
    protected void draw(GuiGraphics g, int mx, int my) {
        int px = topBar(g, module.name);
        int bx = width - M;
        String back = "Back";
        bx -= bw(back);
        button(g, bx, 3, bw(back), 16, back, mx, my, false, b -> onClose());

        int w = Math.min(380, width - M * 2);
        int x = (width - w) / 2;
        int y = TOP + M;

        // header card: icon, name, description, on/off
        PvpTheme.panel(g, x, y, w, 40);
        PvpTheme.slot(g, x + 6, y + 6, 28, 28);
        g.pose().pushMatrix();
        g.pose().translate(x + 8, y + 8);
        g.pose().scale(1.5f, 1.5f);
        g.renderItem(module.icon(), 0, 0);
        g.pose().popMatrix();
        g.drawString(font, module.name, x + 40, y + 8, PvpTheme.text(), false);
        g.drawString(font, PvpTheme.fit(font, module.description, w - 80), x + 40, y + 22, PvpTheme.dim(), false);
        toggle(g, x + w - 28, y + 8, module.enabled, mx, my, b -> module.setEnabled(!module.enabled));
        if (module == Modules.ZOOM) {
            String k = "Key: " + PvpCod.zoomKey.getTranslatedKeyMessage().getString();
            g.drawString(font, k, x + w - 8 - font.width(k), y + 26, PvpTheme.faint(), false);
        }
        y += 46;

        // live preview of HUD modules
        if (module instanceof HudModule h) {
            int ph = Math.min(70, Math.round(h.height(font) * h.scale.value) + 12);
            PvpTheme.slot(g, x, y, w, ph);
            int dw = Math.round(h.width(font) * h.scale.value), dh = Math.round(h.height(font) * h.scale.value);
            g.enableScissor(x + 1, y + 1, x + w - 1, y + ph - 1);
            Hud.drawAt(g, font, h, x + (w - dw) / 2, y + (ph - dh) / 2, true);
            g.disableScissor();
            y += ph + 6;
        } else if (showLive()) {
            int ph = Math.max(70, Math.min(150, (height - y - 30) / 2));
            live = new int[]{x + 1, y + 1, w - 2, ph - 2};
            PvpTheme.outline(g, x, y, w, ph, PvpTheme.line());
            String tag = "LIVE";
            g.fill(x + 4, y + 4, x + 8 + font.width(tag) + 8, y + 15, 0xC0000000);
            g.fill(x + 7, y + 8, x + 10, y + 11, 0xFFFF3B3B);
            g.drawString(font, tag, x + 13, y + 6, 0xFFFFFFFF, false);
            String hint = !module.enabled ? "Switch it on to see it here" : module == Modules.BLOCK_OUTLINE ? "Look at a block" : "This is your game, live";
            g.fill(x + 4, y + ph - 15, x + 8 + font.width(hint), y + ph - 4, 0xC0000000);
            g.drawString(font, hint, x + 6, y + ph - 13, 0xFFFFFFFF, false);
            y += ph + 6;
        } else if (Previews.height(module) > 0) {
            int ph = Math.min(Previews.height(module), Math.max(40, (height - y - 30) / 2));
            Previews.draw(module, g, font, x, y, w, ph);
            y += ph + 6;
        }

        // settings
        List<Setting> rows = rows();
        int rowH = 20;
        int bottom = height - 30;
        int vis = Math.max(1, (bottom - y) / rowH);
        scroll = clamp(scroll, 0, Math.max(0, rows.size() - vis));
        int listTop = y;
        wheels.add(new Wheel(x, listTop, w, vis * rowH, d -> scroll = clamp(scroll + d, 0, Math.max(0, rows.size() - vis))));
        for (int i = scroll; i < rows.size() && i < scroll + vis; i++) {
            row(g, rows.get(i), x, y, w, rowH, mx, my);
            y += rowH;
        }
        scrollbar(g, x + w + 3, listTop, vis * rowH, scroll, vis, rows.size());
        if (rows.isEmpty()) g.drawString(font, "Nothing to set here, just switch it on or off.", x + 6, y + 4, PvpTheme.faint(), false);

        // footer
        int fy = height - 24;
        int fx = x;
        String reset = "Reset settings";
        button(g, fx, fy, bw(reset), 16, reset, mx, my, false, b -> module.resetSettings());
        fx += bw(reset) + 4;
        if (module instanceof HudModule h) {
            String move = "Move on screen";
            button(g, fx, fy, bw(move), 16, move, mx, my, true, b -> minecraft.setScreen(new HudEditorScreen(this)));
            fx += bw(move) + 4;
            String rp = "Reset position";
            button(g, fx, fy, bw(rp), 16, rp, mx, my, false, b -> h.resetPosition());
        }
        if (module == Modules.OTHER_CAPES) {
            String t = "Reload capes";
            button(g, fx, fy, bw(t), 16, t, mx, my, true, b -> com.enc.pvpcod.OtherCapes.clear());
        }
        if (module == Modules.PINGS) {
            String t = "Edit team";
            button(g, fx, fy, bw(t), 16, t, mx, my, true, b -> minecraft.setScreen(new FriendsScreen(this, true)));
        }
        if (module == Modules.TIERS) {
            String t = "Set tiers (admin)";
            button(g, fx, fy, bw(t), 16, t, mx, my, true, b -> minecraft.setScreen(new AdminScreen(this)));
            String r = "Refresh tiers";
            button(g, fx + bw(t) + 4, fy, bw(r), 16, r, mx, my, false, b -> com.enc.pvpcod.Tiers.refresh());
        }
        if (module == Modules.NAME_ICONS) {
            String ad = "Change icons (admin)";
            button(g, fx, fy, bw(ad), 16, ad, mx, my, true, b -> minecraft.setScreen(new AdminScreen(this)));
        }
    }

    private void row(GuiGraphics g, Setting s, int x, int y, int w, int h, int mx, int my) {
        boolean hov = in(mx, my, x, y, w, h);
        if (hov) g.fill(x, y, x + w, y + h - 1, PvpTheme.hoverFill());
        g.fill(x, y + h - 1, x + w, y + h, PvpTheme.line());
        g.drawString(font, s.label, x + 6, y + (h - 8) / 2, PvpTheme.text(), false);
        int cw = Math.min(150, w / 2);
        int cx = x + w - cw - 6;
        int cy = y + (h - 10) / 2;
        if (s instanceof Setting.Bool b) {
            toggle(g, x + w - 26, cy, b.value, mx, my, k -> b.value = !b.value);
        } else if (s instanceof Setting.Num n) {
            String v = n.shown();
            int vw = font.width("00.00x") + 4;
            slider(g, cx, cy, cw - vw - 4, n, mx, my, null);
            g.drawString(font, v, x + w - 6 - font.width(v), y + (h - 8) / 2, PvpTheme.dim(), false);
        } else if (s instanceof Setting.Color c) {
            String hex = String.format("#%08X", c.value);
            int sx = x + w - 6 - 30;
            swatch(g, sx, y + 4, 30, h - 8, c.value);
            g.drawString(font, hex, sx - 6 - font.width(hex), y + (h - 8) / 2, PvpTheme.dim(), false);
            hits.add(new Hit(sx - 6 - font.width(hex), y + 2, 36 + font.width(hex), h - 4,
                    k -> minecraft.setScreen(new ColorScreen(this, s.label, c)), null));
        } else if (s instanceof Setting.Choice c) {
            String v = c.get();
            int bwid = Math.max(70, font.width(v) + 30);
            int bx = x + w - 6 - bwid;
            PvpTheme.slot(g, bx, y + 3, bwid, h - 6);
            boolean lh = in(mx, my, bx, y + 3, 12, h - 6), rh = in(mx, my, bx + bwid - 12, y + 3, 12, h - 6);
            g.drawString(font, "<", bx + 3, y + (h - 8) / 2, lh ? PvpTheme.accent() : PvpTheme.dim(), false);
            g.drawString(font, ">", bx + bwid - 8, y + (h - 8) / 2, rh ? PvpTheme.accent() : PvpTheme.dim(), false);
            g.drawString(font, v, bx + (bwid - font.width(v)) / 2, y + (h - 8) / 2, PvpTheme.text(), false);
            hits.add(new Hit(bx, y + 3, bwid / 2, h - 6, k -> c.next(-1), null));
            hits.add(new Hit(bx + bwid / 2, y + 3, bwid - bwid / 2, h - 6, k -> c.next(1), null));
        }
    }
}
