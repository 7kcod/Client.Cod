package dev.cod.pvpcod.ui;

import dev.cod.packcod.ui.PvpTheme;
import dev.cod.pvpcod.Wardrobe;
import dev.cod.pvpcod.module.Modules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

/** Skin swapper and cape swapper, with a live preview of you. */
public final class WardrobeScreen extends UiScreen {

    private List<String> skins = new ArrayList<>();
    private List<String> capeFiles = new ArrayList<>();
    private int scrollS, scrollC;
    private EditBox nameBox;
    private String nameText = "";
    private String status = "";
    private int statusColor;
    private boolean fetching;

    public WardrobeScreen(Screen parent) {
        super("Wardrobe", parent);
        refresh();
    }

    @Override
    protected void init() {
        nameBox = new EditBox(font, 0, 0, 80, 10, Component.literal("Player"));
        searchBox(nameBox);
        nameBox.setMaxLength(16);
        nameBox.setHint(Component.literal("Any player's name"));
        nameBox.setValue(nameText);
        nameBox.setResponder(v -> nameText = v);
        addRenderableWidget(nameBox);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (nameBox.isFocused() && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            fetch();
            return true;
        }
        return super.keyPressed(event);
    }

    /** Put on someone's skin just from their name. */
    private void fetch() {
        String n = nameBox.getValue().trim();
        if (n.isEmpty() || fetching) return;
        fetching = true;
        status = "Getting " + n + "'s skin...";
        statusColor = PvpTheme.dim();
        Wardrobe.fetchSkin(n, r -> {
            fetching = false;
            status = r.message();
            statusColor = r.file() == null ? PvpTheme.bad() : PvpTheme.good();
            if (r.file() == null) return;
            var skin = Modules.SKIN;
            Wardrobe.forgetSkin(r.file());
            skin.file.value = r.file();
            skin.slim.value = r.slim();
            skin.setEnabled(true);
            Modules.save();
            refresh();
            nameBox.setValue("");
        });
    }

    private long lastRefresh;

    /** Picks up capes saved by the cape maker. */
    private void refreshIfNeeded() {
        if (System.currentTimeMillis() - lastRefresh > 2000) {
            lastRefresh = System.currentTimeMillis();
            refresh();
        }
    }

    private void refresh() {
        skins = Wardrobe.skinFiles();
        capeFiles = Wardrobe.capeFiles();
    }

    @Override
    protected void draw(GuiGraphics g, int mx, int my) {
        topBar(g, "Wardrobe");
        String back = "Back";
        button(g, width - M - bw(back), 3, bw(back), 16, back, mx, my, false, b -> onClose());
        String rl = "Reload files";
        button(g, width - M * 2 - bw(back) - bw(rl), 3, bw(rl), 16, rl, mx, my, false, b -> {
            Wardrobe.reload();
            refresh();
        });

        int y = TOP + M, h = height - y - M;
        int pw = Math.min(150, width / 4);
        int cw = (width - M * 4 - pw) / 2;
        int sx = M, px = sx + cw + M, cx = px + pw + M;

        // ---- skins
        var skin = Modules.SKIN;
        PvpTheme.panel(g, sx, y, cw, h);
        PvpTheme.label(g, font, "Skins", sx + 8, y + 8);
        toggle(g, sx + cw - 28, y + 6, skin.enabled, mx, my, b -> skin.setEnabled(!skin.enabled));
        g.drawString(font, "Slim arms", sx + 8, y + 24, PvpTheme.dim(), false);
        toggle(g, sx + cw - 28, y + 23, skin.slim.value, mx, my, b -> skin.slim.value = !skin.slim.value);
        // wear anyone's skin by name
        nameBox.setX(sx + 12);
        nameBox.setY(y + 43);
        nameBox.setWidth(cw - 24 - 40);
        PvpTheme.slot(g, sx + 8, y + 40, cw - 16 - 40, 15);
        button(g, sx + cw - 44, y + 39, 36, 16, fetching ? "..." : "Get", mx, my, true, !fetching, b -> fetch());
        if (!status.isEmpty()) g.drawString(font, PvpTheme.fit(font, status, cw - 16), sx + 8, y + 59, statusColor, false);
        List<String> sRows = new ArrayList<>();
        sRows.add("");
        sRows.addAll(skins);
        int listY = y + 72, rowH = 20;
        int vis = Math.max(1, (y + h - 26 - listY) / rowH);
        scrollS = clamp(scrollS, 0, Math.max(0, sRows.size() - vis));
        wheels.add(new Wheel(sx, listY, cw, vis * rowH, d -> scrollS = clamp(scrollS + d, 0, Math.max(0, sRows.size() - vis))));
        for (int i = scrollS; i < sRows.size() && i < scrollS + vis; i++) {
            String f = sRows.get(i);
            int ry = listY + (i - scrollS) * rowH;
            boolean on = f.isEmpty() ? !skin.enabled || skin.file.value.isEmpty() : skin.enabled && f.equals(skin.file.value);
            boolean hov = in(mx, my, sx + 4, ry, cw - 8, rowH - 1);
            row(g, sx + 4, ry, cw - 8, rowH - 1, on, hov);
            if (!f.isEmpty()) {
                Identifier tex = Wardrobe.skinTexture(f);
                if (tex != null) {
                    // face + hat layer
                    int[] sz = Wardrobe.size(tex);
                    int k = Math.max(1, sz[0] / 64);
                    g.blit(RenderPipelines.GUI_TEXTURED, tex, sx + 8, ry + 2, 8f * k, 8f * k, 16, 16, 8 * k, 8 * k, sz[0], sz[1]);
                    g.blit(RenderPipelines.GUI_TEXTURED, tex, sx + 8, ry + 2, 40f * k, 8f * k, 16, 16, 8 * k, 8 * k, sz[0], sz[1]);
                }
            }
            String label = f.isEmpty() ? "My own skin" : Wardrobe.pretty(f);
            g.drawString(font, PvpTheme.fit(font, label, cw - 40), sx + 30, ry + 6, on ? PvpTheme.text() : PvpTheme.dim(), false);
            hits.add(new Hit(sx + 4, ry, cw - 8, rowH - 1, b -> {
                if (f.isEmpty()) skin.setEnabled(false);
                else {
                    skin.file.value = f;
                    skin.setEnabled(true);
                }
                Modules.save();
            }, null));
        }
        if (skins.isEmpty()) g.drawString(font, PvpTheme.fit(font, "Type a name above, or put skin files in the folder", cw - 16), sx + 8, listY + rowH + 6, PvpTheme.faint(), false);
        String of = "Open skins folder";
        button(g, sx + 8, y + h - 22, cw - 16, 16, of, mx, my, false, b -> Util.getPlatform().openPath(Wardrobe.SKINS));

        // ---- preview
        PvpTheme.slot(g, px, y, pw, h);
        if (minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, px + 4, y + 4, px + pw - 4, y + h - 20,
                    Math.min(pw, h) / 3, 0.0625f, mx, my, minecraft.player);
        } else {
            String s1 = "Join a world to see";
            String s2 = "yourself here";
            g.drawString(font, s1, px + (pw - font.width(s1)) / 2, y + h / 2 - 10, PvpTheme.faint(), false);
            g.drawString(font, s2, px + (pw - font.width(s2)) / 2, y + h / 2, PvpTheme.faint(), false);
        }
        String note = "Only you see these";
        g.drawString(font, note, px + (pw - font.width(note)) / 2, y + h - 13, PvpTheme.faint(), false);

        // ---- capes
        var cape = Modules.CAPE;
        PvpTheme.panel(g, cx, y, cw, h);
        PvpTheme.label(g, font, "Capes", cx + 8, y + 8);
        toggle(g, cx + cw - 28, y + 6, cape.enabled, mx, my, b -> cape.setEnabled(!cape.enabled));
        g.drawString(font, "On elytra too", cx + 8, y + 24, PvpTheme.dim(), false);
        toggle(g, cx + cw - 28, y + 23, cape.elytra.value, mx, my, b -> cape.elytra.value = !cape.elytra.value);
        g.drawString(font, "Cloth (flowing) capes", cx + 8, y + 40, PvpTheme.dim(), false);
        toggle(g, cx + cw - 28, y + 39, cape.cloth.value, mx, my, b -> cape.cloth.value = !cape.cloth.value);
        boolean mh = in(mx, my, cx + cw - 50, y + 38, 18, 12);
        g.drawString(font, "...", cx + cw - 46, y + 39, mh ? PvpTheme.accent() : PvpTheme.faint(), false);
        hits.add(new Hit(cx + cw - 50, y + 38, 18, 12, b -> minecraft.setScreen(new ModuleScreen(this, Modules.CAPE)), null));
        if (mh) tooltip = "More cape settings (cloth wave)";
        List<String> cRows = new ArrayList<>();
        cRows.add("");
        for (Wardrobe.CapeChoice c : Wardrobe.BUILTIN) cRows.add(c.id());
        for (String f : capeFiles) cRows.add("file:" + f);
        int cListY = y + 56;
        int cvis = Math.max(1, (y + h - 26 - cListY) / rowH);
        refreshIfNeeded();
        scrollC = clamp(scrollC, 0, Math.max(0, cRows.size() - cvis));
        wheels.add(new Wheel(cx, cListY, cw, cvis * rowH, d -> scrollC = clamp(scrollC + d, 0, Math.max(0, cRows.size() - cvis))));
        for (int i = scrollC; i < cRows.size() && i < scrollC + cvis; i++) {
            String id = cRows.get(i);
            int ry = cListY + (i - scrollC) * rowH;
            boolean on = id.isEmpty() ? !cape.enabled : cape.enabled && id.equals(cape.cape.value);
            boolean hov = in(mx, my, cx + 4, ry, cw - 8, rowH - 1);
            row(g, cx + 4, ry, cw - 8, rowH - 1, on, hov);
            if (!id.isEmpty()) {
                Identifier tex = Wardrobe.capeTexture(id);
                // the back of the cape, 10x16 at (1, 1) of a 64x32 sheet
                if (tex != null) {
                    int[] sz = Wardrobe.size(tex);
                    int k = Math.max(1, sz[0] / 64);
                    g.blit(RenderPipelines.GUI_TEXTURED, tex, cx + 10, ry + 1, (float) k, (float) k, 10, 16, 10 * k, 16 * k, sz[0], sz[1]);
                }
            }
            String label = id.isEmpty() ? "My own cape (or none)" : Wardrobe.capeLabel(id);
            g.drawString(font, PvpTheme.fit(font, label, cw - 40), cx + 30, ry + 6, on ? PvpTheme.text() : PvpTheme.dim(), false);
            hits.add(new Hit(cx + 4, ry, cw - 8, rowH - 1, b -> {
                if (id.isEmpty()) cape.setEnabled(false);
                else {
                    cape.cape.value = id;
                    cape.setEnabled(true);
                }
                Modules.save();
            }, null));
        }
        int half = (cw - 20) / 2;
        button(g, cx + 8, y + h - 22, half, 16, "Make a cape", mx, my, true, b -> minecraft.setScreen(new CapeMakerScreen(this)));
        button(g, cx + 12 + half, y + h - 22, cw - 20 - half, 16, "Open folder", mx, my, false, b -> Util.getPlatform().openPath(Wardrobe.CAPES));
    }

    private void row(GuiGraphics g, int x, int y, int w, int h, boolean on, boolean hov) {
        if (on) {
            g.fill(x, y, x + w, y + h, PvpTheme.mix(PvpTheme.panel(), PvpTheme.accent(), 0.22f));
            g.fill(x, y, x + 2, y + h, PvpTheme.accent());
        } else if (hov) {
            g.fill(x, y, x + w, y + h, PvpTheme.hoverFill());
        }
    }
}
