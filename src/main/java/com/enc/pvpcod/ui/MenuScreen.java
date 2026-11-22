package com.enc.pvpcod.ui;

import com.enc.packcod.ui.MixerScreen;
import com.enc.packcod.ui.PvpTheme;
import com.enc.pvpcod.module.HudModule;
import com.enc.pvpcod.module.Module;
import com.enc.pvpcod.module.Modules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The main pvp.cod menu: every module as a tile, plus the other pages down the side. */
public final class MenuScreen extends UiScreen {

    private static Module.Category filter; // null = all; remembered between opens
    private EditBox search;
    private int scroll;
    private int sideScroll;
    private int[] sideClip;

    public MenuScreen(Screen parent) {
        super("pvp.cod", parent);
    }

    @Override
    protected void init() {
        int sw = Math.min(150, width / 4);
        search = new EditBox(font, width - M - sw - 2, 7, sw - 6, 10, Component.literal("Search"));
        searchBox(search);
        search.setHint(Component.literal("Search modules"));
        search.setResponder(s -> scroll = 0);
        addRenderableWidget(search);
    }

    private int sideW() {
        return Math.max(84, Math.min(110, width / 6));
    }

    private List<Module> shown() {
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        for (Module m : Modules.ALL) {
            if (filter != null && m.category != filter) continue;
            if (!q.isEmpty() && !m.name.toLowerCase(Locale.ROOT).contains(q) && !m.description.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(m);
        }
        return out;
    }

    @Override
    protected void draw(GuiGraphics g, int mx, int my) {
        topBar(g, null);
        PvpTheme.slot(g, search.getX() - 4, 3, search.getWidth() + 8, 15);

        // ---- side bar (scrolls if the window is short)
        int sx = M, sy = TOP + M, sw = sideW(), sh = height - sy - M;
        PvpTheme.panel(g, sx, sy, sw, sh);
        int content = 6 + 13 + 14 * (Module.Category.values().length + 1) + 6 + 13 + 14 * 11 + 6;
        sideScroll = clamp(sideScroll, 0, Math.max(0, content - sh));
        int maxSide = Math.max(0, content - sh);
        wheels.add(new Wheel(sx, sy, sw, sh, d -> sideScroll = clamp(sideScroll + d * 14, 0, maxSide)));
        g.enableScissor(sx + 1, sy + 1, sx + sw - 1, sy + sh - 1);
        sideClip = new int[]{sy, sy + sh};
        int y = sy + 6 - sideScroll;
        PvpTheme.label(g, font, "Modules", sx + 6, y);
        y += 13;
        y = sideRow(g, sx, y, sw, "All", filter == null, mx, my, () -> select(null));
        for (Module.Category c : Module.Category.values()) {
            Module.Category cc = c;
            y = sideRow(g, sx, y, sw, c.label, filter == c, mx, my, () -> select(cc));
        }
        y += 6;
        PvpTheme.label(g, font, "Pages", sx + 6, y);
        y += 13;
        y = sideRow(g, sx, y, sw, "Edit HUD", false, mx, my, () -> minecraft.setScreen(new HudEditorScreen(this)));
        y = sideRow(g, sx, y, sw, "Friends", false, mx, my, () -> minecraft.setScreen(new FriendsScreen(this)));
        y = sideRow(g, sx, y, sw, "Team", false, mx, my, () -> minecraft.setScreen(new FriendsScreen(this, true)));
        y = sideRow(g, sx, y, sw, "Wardrobe", false, mx, my, () -> minecraft.setScreen(new WardrobeScreen(this)));
        y = sideRow(g, sx, y, sw, "Cape maker", false, mx, my, () -> minecraft.setScreen(new CapeMakerScreen(this)));
        y = sideRow(g, sx, y, sw, "pack.cod", false, mx, my, () -> minecraft.setScreen(new MixerScreen(this)));
        y = sideRow(g, sx, y, sw, "Media", false, mx, my, () -> minecraft.setScreen(new MediaScreen(this)));
        y = sideRow(g, sx, y, sw, "Admin", false, mx, my, () -> minecraft.setScreen(new AdminScreen(this)));
        y = sideRow(g, sx, y, sw, "Credits", false, mx, my, () -> minecraft.setScreen(new CreditsScreen(this)));
        g.disableScissor();
        sideClip = null;
        scrollbar(g, sx + sw - 3, sy + 2, sh - 4, sideScroll, sh, content);

        // how many modules are on, in the top bar (it used to sit under the side bar and overlap)
        int on = 0;
        for (Module m : Modules.ALL) if (m.enabled) on++;
        String count = on + " of " + Modules.ALL.size() + " on";
        g.drawString(font, count, search.getX() - 12 - font.width(count), 7, PvpTheme.faint(), false);

        // ---- tiles
        int cx = sx + sw + M, cy = TOP + M, cw = width - cx - M, ch = height - cy - M;
        List<Module> list = shown();
        int tileW = 132, tileH = 46, gap = 4;
        int cols = Math.max(1, (cw + gap) / (tileW + gap));
        tileW = (cw - gap * (cols - 1)) / cols;
        int rows = (list.size() + cols - 1) / cols;
        int visRows = Math.max(1, (ch + gap) / (tileH + gap));
        scroll = clamp(scroll, 0, Math.max(0, rows - visRows));
        wheels.add(new Wheel(cx, cy, cw, ch, d -> scroll = clamp(scroll + d, 0, Math.max(0, rows - visRows))));
        if (list.isEmpty()) {
            String s = "No modules match that";
            g.drawString(font, s, cx + (cw - font.width(s)) / 2, cy + 20, PvpTheme.faint(), false);
        }
        for (int i = scroll * cols; i < list.size(); i++) {
            int r = i / cols - scroll, c = i % cols;
            if (r >= visRows) break;
            tile(g, list.get(i), cx + c * (tileW + gap), cy + r * (tileH + gap), tileW, tileH, mx, my);
        }
        scrollbar(g, width - 4, cy, ch, scroll, visRows, rows);
    }

    private void select(Module.Category c) {
        filter = c;
        scroll = 0;
    }

    private int sideRow(GuiGraphics g, int x, int y, int w, String text, boolean on, int mx, int my, Runnable action) {
        boolean hov = in(mx, my, x + 2, y, w - 4, 13);
        if (on) g.fill(x + 2, y, x + w - 2, y + 13, PvpTheme.mix(PvpTheme.panel(), PvpTheme.accent(), 0.25f));
        else if (hov) g.fill(x + 2, y, x + w - 2, y + 13, PvpTheme.hoverFill());
        if (on) g.fill(x + 2, y, x + 4, y + 13, PvpTheme.accent());
        g.drawString(font, text, x + 8, y + 3, on ? PvpTheme.text() : hov ? PvpTheme.text() : PvpTheme.dim(), false);
        if (sideClip == null || (y + 13 > sideClip[0] && y < sideClip[1])) {
            int top = sideClip == null ? y : Math.max(y, sideClip[0]), bot = sideClip == null ? y + 13 : Math.min(y + 13, sideClip[1]);
            hits.add(new Hit(x + 2, top, w - 4, bot - top, b -> action.run(), null));
        }
        return y + 14;
    }

    private void tile(GuiGraphics g, Module m, int x, int y, int w, int h, int mx, int my) {
        boolean hov = in(mx, my, x, y, w, h);
        PvpTheme.panel(g, x, y, w, h);
        if (hov) g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PvpTheme.hoverFill());
        if (m.enabled) g.fill(x, y, x + 2, y + h, PvpTheme.accent());
        PvpTheme.slot(g, x + 5, y + 5, 20, 20);
        g.renderItem(m.icon(), x + 7, y + 7);
        g.drawString(font, PvpTheme.fit(font, m.name, w - 50), x + 30, y + 6, PvpTheme.text(), false);
        g.drawString(font, m.category.label + (m instanceof HudModule ? "" : ""), x + 30, y + 16, PvpTheme.faint(), false);
        g.drawString(font, PvpTheme.fit(font, m.description, w - 12), x + 6, y + h - 13, PvpTheme.dim(), false);
        // whole tile = settings, the switch = on/off
        hits.add(new Hit(x, y, w, h, b -> openSettings(m), null));
        toggle(g, x + w - 26, y + 6, m.enabled, mx, my, b -> {
            m.setEnabled(!m.enabled);
            Modules.save();
        });
        if (hov) tooltip = "Click for settings";
    }

    private void openSettings(Module m) {
        if (m == Modules.FRIENDS) minecraft.setScreen(new FriendsScreen(this));
        else if (m == Modules.PINGS) minecraft.setScreen(new ModuleScreen(this, m));
        else if (m == Modules.SKIN || m == Modules.CAPE) minecraft.setScreen(new WardrobeScreen(this));
        else minecraft.setScreen(new ModuleScreen(this, m));
    }

    static ItemStack stack() {
        return new ItemStack(Items.COD);
    }
}
