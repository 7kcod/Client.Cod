package dev.cod.pvpcod.ui;

import dev.cod.packcod.ui.PvpTheme;
import dev.cod.pvpcod.Social;
import dev.cod.pvpcod.config.Store;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * The admin panel. Needs the admin key to open (you make the key the first time).
 * Here you choose the icon next to each player's name; anyone you don't set gets the
 * normal one (the cod, unless you change the default).
 */
public final class AdminScreen extends UiScreen {

    private enum State { CREATE, LOCKED, OPEN }

    /** Stays unlocked until the game closes, so you don't retype the key every time. */
    private static boolean unlocked;

    private State state;
    private EditBox key1, key2, player;
    private String status = "";
    private int statusColor;
    /** null = the default icon ("everyone else"). */
    private String target;
    private int scrollP, scrollI;
    /** Right side shows icons, or tiers. */
    private static boolean tiersTab;
    private int tierMode;

    public AdminScreen(Screen parent) {
        super("Admin", parent);
        state = unlocked ? State.OPEN : Social.hasAdminKey() ? State.LOCKED : State.CREATE;
    }

    @Override
    protected void init() {
        clearWidgets();
        int w = Math.min(220, width - M * 2), x = (width - w) / 2;
        if (state == State.OPEN) {
            int lw = Math.min(170, width / 3);
            player = new EditBox(font, M + 8, height - M - 40, lw - 16 - 44, 10, Component.literal("Player"));
            searchBox(player);
            player.setMaxLength(16);
            player.setHint(Component.literal("Add a player"));
            addRenderableWidget(player);
            return;
        }
        key1 = keyBox(x + 12, height / 2 - 14, w - 24);
        if (state == State.CREATE) key2 = keyBox(x + 12, height / 2 + 10, w - 24);
        setFocused(key1);
    }

    private EditBox keyBox(int x, int y, int w) {
        EditBox b = new EditBox(font, x, y, w, 10, Component.literal("Key"));
        searchBox(b);
        b.setMaxLength(64);
        b.addFormatter((s, pos) -> FormattedCharSequence.forward("*".repeat(s.length()), Style.EMPTY));
        addRenderableWidget(b);
        return b;
    }

    private void say(String s, int c) {
        status = s;
        statusColor = c;
    }

    private void submit() {
        if (state == State.CREATE) {
            String a = key1.getValue(), b = key2.getValue();
            if (a.length() < 4) say("Make the key at least 4 characters", PvpTheme.bad());
            else if (!a.equals(b)) say("The two keys don't match", PvpTheme.bad());
            else {
                Social.setAdminKey(a);
                open();
            }
        } else if (state == State.LOCKED) {
            if (Social.checkAdminKey(key1.getValue())) open();
            else {
                say("Wrong key", PvpTheme.bad());
                key1.setValue("");
            }
        }
    }

    private void open() {
        unlocked = true;
        state = State.OPEN;
        status = "";
        rebuildWidgets();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean enter = event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER;
        if (enter && state != State.OPEN) {
            if (state == State.CREATE && key1.isFocused()) setFocused(key2);
            else submit();
            return true;
        }
        if (enter && player != null && player.isFocused()) {
            addPlayer();
            return true;
        }
        return super.keyPressed(event);
    }

    private void addPlayer() {
        String n = player.getValue().trim();
        if (!n.matches("[A-Za-z0-9_]{1,16}")) {
            say("That isn't a Minecraft name", PvpTheme.bad());
            return;
        }
        if (!tiersTab) Social.assign(n, Social.defaultIcon());
        else dev.cod.pvpcod.Tiers.setOverride(n, dev.cod.pvpcod.Tiers.MODES[tierMode], "LT5");
        Store.get().save();
        target = n.toLowerCase();
        player.setValue("");
        say("Added " + n + (tiersTab ? ", now pick their tier" : ", now pick their icon"), PvpTheme.good());
    }

    @Override
    protected void draw(GuiGraphics g, int mx, int my) {
        topBar(g, "Admin");
        String back = "Back";
        button(g, width - M - bw(back), 3, bw(back), 16, back, mx, my, false, b -> onClose());
        if (state == State.OPEN) drawPanel(g, mx, my);
        else drawGate(g, mx, my);
    }

    // ------------------------------------------------------------------ key gate

    private void drawGate(GuiGraphics g, int mx, int my) {
        int w = Math.min(220, width - M * 2), x = (width - w) / 2;
        boolean create = state == State.CREATE;
        int top = height / 2 - 52, h = create ? 112 : 88;
        PvpTheme.panel(g, x, top, w, h);
        String title = create ? "Make an admin key" : "Admin key";
        g.drawString(font, title, x + 12, top + 10, PvpTheme.text(), false);
        String sub = create ? "You'll need it to open this panel." : "Type the key to open the admin panel.";
        g.drawString(font, PvpTheme.fit(font, sub, w - 24), x + 12, top + 22, PvpTheme.faint(), false);
        PvpTheme.slot(g, key1.getX() - 4, key1.getY() - 3, key1.getWidth() + 8, 15);
        if (create) {
            g.drawString(font, "Again", x + 12, key2.getY() - 12, PvpTheme.dim(), false);
            PvpTheme.slot(g, key2.getX() - 4, key2.getY() - 3, key2.getWidth() + 8, 15);
        }
        if (!status.isEmpty()) g.drawString(font, status, x + 12, top + h - 32, statusColor, false);
        String go = create ? "Save key" : "Open";
        button(g, x + w - 12 - bw(go), top + h - 22, bw(go), 16, go, mx, my, true, b -> submit());
    }

    // ------------------------------------------------------------------ the panel

    private List<String> players() {
        TreeSet<String> set = new TreeSet<>(Social.assigned().keySet());
        set.addAll(dev.cod.pvpcod.Tiers.overrides().keySet());
        for (String p : Social.onlinePlayers()) set.add(p.toLowerCase());
        return new ArrayList<>(set);
    }

    private void drawPanel(GuiGraphics g, int mx, int my) {
        int y = TOP + M, h = height - y - M;
        int lw = Math.min(170, width / 3);
        Map<String, String> assigned = Social.assigned();

        // ---- left: who
        PvpTheme.panel(g, M, y, lw, h);
        PvpTheme.label(g, font, "Players", M + 8, y + 8);
        int ry = y + 22, rowH = 15;
        ry = whoRow(g, M + 4, ry, lw - 8, rowH, null, "Everyone else (normal)", Social.defaultIcon(), mx, my);
        g.fill(M + 6, ry + 1, M + lw - 6, ry + 2, PvpTheme.line());
        ry += 4;
        List<String> ps = players();
        int listTop = ry;
        int vis = Math.max(1, (y + h - 50 - listTop) / rowH);
        scrollP = clamp(scrollP, 0, Math.max(0, ps.size() - vis));
        wheels.add(new Wheel(M, listTop, lw, vis * rowH, d -> scrollP = clamp(scrollP + d, 0, Math.max(0, ps.size() - vis))));
        for (int i = scrollP; i < ps.size() && i < scrollP + vis; i++) {
            String p = ps.get(i);
            String icon = assigned.get(p);
            ry = whoRow(g, M + 4, ry, lw - 8, rowH, p, displayName(p) + (icon == null ? "  (normal)" : ""), icon == null ? Social.defaultIcon() : icon, mx, my);
        }
        if (ps.isEmpty()) g.drawString(font, "Add a player below", M + 8, ry + 4, PvpTheme.faint(), false);
        scrollbar(g, M + lw - 4, listTop, vis * rowH, scrollP, vis, ps.size());
        PvpTheme.slot(g, player.getX() - 4, player.getY() - 3, player.getWidth() + 8, 15);
        button(g, M + lw - 48, player.getY() - 4, 40, 16, "Add", mx, my, true, b -> addPlayer());
        if (!status.isEmpty()) g.drawString(font, PvpTheme.fit(font, status, lw - 16), M + 8, y + h - 16, statusColor, false);

        // ---- right: icon grid (or tiers) for the selected row
        int rx = M + lw + M, rw = width - rx - M;
        PvpTheme.panel(g, rx, y, rw, h);
        // tabs
        int tx = rx + rw - 8;
        for (String tab : new String[]{"Tiers", "Icons"}) {
            boolean on = tab.equals("Tiers") == tiersTab;
            tx -= bw(tab);
            button(g, tx, y + 4, bw(tab), 14, tab, mx, my, on, b -> {
                tiersTab = tab.equals("Tiers");
                scrollI = 0;
            });
            tx -= 3;
        }
        if (tiersTab) {
            drawTiers(g, mx, my, rx, y, rw, h);
            drawBottom(g, mx, my, rx, y, h);
            return;
        }
        String who = target == null ? "Everyone else" : displayName(target);
        String current = target == null ? Social.defaultIcon() : assigned.getOrDefault(target, null);
        PvpTheme.label(g, font, "Icon for " + who, rx + 8, y + 8);
        // big preview of how the name looks
        String previewName = target == null ? "Steve" : displayName(target);
        Social.Icon ic = Social.icon(current == null ? Social.defaultIcon() : current);
        Component preview = ic == null ? Component.literal(previewName)
                : Component.empty().append(Component.object(new net.minecraft.network.chat.contents.objects.AtlasSprite(ic.atlas(), ic.sprite()))).append(" " + previewName);
        PvpTheme.slot(g, rx + 8, y + 22, rw - 16, 30);
        g.pose().pushMatrix();
        g.pose().translate(rx + 8 + (rw - 16) / 2f, y + 29);
        g.pose().scale(2f, 2f);
        g.drawString(font, preview, -font.width(preview) / 2, 0, PvpTheme.text(), true);
        g.pose().popMatrix();

        int gy = y + 60, cell = 24;
        int per = Math.max(1, (rw - 16) / cell);
        List<String> ids = new ArrayList<>();
        if (target != null) {
            ids.add("__normal");
            ids.add("none");
        }
        for (Social.Icon i : Social.ICONS) ids.add(i.id());
        int rows = (ids.size() + per - 1) / per;
        int visRows = Math.max(1, (y + h - 30 - gy) / cell);
        scrollI = clamp(scrollI, 0, Math.max(0, rows - visRows));
        wheels.add(new Wheel(rx, gy, rw, visRows * cell, d -> scrollI = clamp(scrollI + d, 0, Math.max(0, rows - visRows))));
        for (int i = scrollI * per; i < ids.size(); i++) {
            int r = i / per - scrollI;
            if (r >= visRows) break;
            int cx = rx + 8 + (i % per) * cell, cy = gy + r * cell;
            String id = ids.get(i);
            boolean sel = id.equals("__normal") ? current == null : id.equals(current);
            boolean hov = in(mx, my, cx, cy, cell - 2, cell - 2);
            PvpTheme.slot(g, cx, cy, cell - 2, cell - 2);
            if (sel) PvpTheme.outline(g, cx - 1, cy - 1, cell, cell, PvpTheme.accent());
            else if (hov) PvpTheme.outline(g, cx, cy, cell - 2, cell - 2, PvpTheme.dim());
            if (id.equals("__normal")) {
                g.drawString(font, "N", cx + 8, cy + 7, PvpTheme.dim(), false);
                if (hov) tooltip = "Normal (" + Social.defaultIcon() + ")";
            } else if (id.equals("none")) {
                g.drawString(font, "-", cx + 9, cy + 7, PvpTheme.dim(), false);
                if (hov) tooltip = "No icon";
            } else {
                Social.Icon icon = Social.icon(id);
                g.pose().pushMatrix();
                g.pose().translate(cx + 3, cy + 3);
                g.pose().scale(2f, 2f);
                g.drawString(font, Component.object(new net.minecraft.network.chat.contents.objects.AtlasSprite(icon.atlas(), icon.sprite())), 0, 0, 0xFFFFFFFF, false);
                g.pose().popMatrix();
                if (hov) tooltip = icon.label();
            }
            hits.add(new Hit(cx, cy, cell - 2, cell - 2, b -> pick(id), null));
        }
        scrollbar(g, rx + rw - 4, gy, visRows * cell, scrollI, visRows, rows);

        drawBottom(g, mx, my, rx, y, h);
    }

    private void drawTiers(GuiGraphics g, int mx, int my, int rx, int y, int rw, int h) {
        if (target == null) {
            PvpTheme.label(g, font, "Tiers", rx + 8, y + 8);
            g.drawString(font, "Pick a player on the left to set their tier.", rx + 8, y + 30, PvpTheme.dim(), false);
            g.drawString(font, "Anyone you don't set shows their real MCTiers tier.", rx + 8, y + 42, PvpTheme.faint(), false);
            return;
        }
        String who = displayName(target);
        String over = dev.cod.pvpcod.Tiers.overrides().get(target);
        String curMode = over == null ? null : over.substring(0, over.indexOf(':'));
        String curTier = over == null ? null : over.substring(over.indexOf(':') + 1);
        PvpTheme.label(g, font, "Tier for " + who, rx + 8, y + 8);
        // preview of the name tag
        Component tag = dev.cod.pvpcod.Tiers.tag(who, null);
        Component preview = tag == null ? Component.literal(who + "  (their MCTiers tier)") : Component.empty().append(who + " ").append(tag);
        PvpTheme.slot(g, rx + 8, y + 22, rw - 16, 22);
        g.drawString(font, preview, rx + 8 + (rw - 16 - font.width(preview)) / 2, y + 29, PvpTheme.text(), true);

        int gy = y + 52;
        g.drawString(font, "Game mode", rx + 8, gy, PvpTheme.dim(), false);
        gy += 12;
        if (curMode != null && tierMode < 0) tierMode = 0;
        int bx = rx + 8;
        for (int i = 0; i < dev.cod.pvpcod.Tiers.MODES.length; i++) {
            String n = dev.cod.pvpcod.Tiers.MODE_NAMES[i];
            int w = bw(n);
            if (bx + w > rx + rw - 8) {
                bx = rx + 8;
                gy += 18;
            }
            int idx = i;
            button(g, bx, gy, w, 16, n, mx, my, tierMode == i, b -> tierMode = idx);
            bx += w + 3;
        }
        gy += 24;
        g.drawString(font, "Tier", rx + 8, gy, PvpTheme.dim(), false);
        gy += 12;
        bx = rx + 8;
        for (String t : dev.cod.pvpcod.Tiers.TIERS) {
            int w = 34;
            if (bx + w > rx + rw - 8) {
                bx = rx + 8;
                gy += 18;
            }
            boolean sel = t.equals(curTier) && dev.cod.pvpcod.Tiers.MODES[tierMode].equals(curMode);
            boolean hov = in(mx, my, bx, gy, w, 16);
            PvpTheme.raised(g, bx, gy, w, 16, hov, sel, true);
            g.drawString(font, t, bx + (w - font.width(t)) / 2, gy + 4, 0xFF000000 | dev.cod.pvpcod.Tiers.color(t), true);
            String tt = t;
            String name = who;
            hits.add(new Hit(bx, gy, w, 16, b -> {
                dev.cod.pvpcod.Tiers.setOverride(name, dev.cod.pvpcod.Tiers.MODES[tierMode], tt);
                say(name + " is now " + tt + " " + dev.cod.pvpcod.Tiers.MODE_NAMES[tierMode], PvpTheme.good());
            }, null));
            bx += w + 3;
        }
        gy += 24;
        String clear = "Clear (use their real tier)";
        String name = who;
        button(g, rx + 8, gy, bw(clear), 16, clear, mx, my, false, over != null, b -> {
            dev.cod.pvpcod.Tiers.clearOverride(name);
            say(name + " shows their real tier again", PvpTheme.dim());
        });
        if (over != null) {
            String now = "Set: " + curTier + " in " + dev.cod.pvpcod.Tiers.modeName(curMode);
            g.drawString(font, now, rx + 12 + bw(clear), gy + 4, PvpTheme.faint(), false);
        }
    }

    private void drawBottom(GuiGraphics g, int mx, int my, int rx, int y, int h) {
        // ---- bottom buttons
        int by = y + h - 22, bx = rx + 8;
        String copy = "Copy code";
        button(g, bx, by, bw(copy), 16, copy, mx, my, false, b -> {
            minecraft.keyboardHandler.setClipboard(Social.exportCode());
            say("Copied icons + tiers. Friends paste it in their admin panel.", PvpTheme.good());
        });
        bx += bw(copy) + 4;
        String paste = "Paste code";
        button(g, bx, by, bw(paste), 16, paste, mx, my, false, b -> {
            int n = Social.importCode(minecraft.keyboardHandler.getClipboard());
            if (n < 0) say("Clipboard doesn't have a pvp.cod code", PvpTheme.bad());
            else say("Loaded icons and tiers", PvpTheme.good());
        });
        bx += bw(paste) + 4;
        String ck = "Change key";
        button(g, bx, by, bw(ck), 16, ck, mx, my, false, b -> {
            state = State.CREATE;
            unlocked = false;
            status = "";
            rebuildWidgets();
        });
        bx += bw(ck) + 4;
        String lock = "Lock";
        button(g, bx, by, bw(lock), 16, lock, mx, my, false, b -> {
            unlocked = false;
            onClose();
        });
    }

    private void pick(String id) {
        if (target == null) {
            if (!id.startsWith("__") && !id.equals("none")) Social.setDefaultIcon(id);
        } else if (id.equals("__normal")) {
            Social.unassign(target);
        } else {
            Social.assign(target, id);
        }
        Store.get().save();
    }

    private int whoRow(GuiGraphics g, int x, int y, int w, int h, String key, String label, String iconId, int mx, int my) {
        boolean sel = key == null ? target == null : key.equals(target);
        boolean hov = in(mx, my, x, y, w, h - 1);
        if (sel) {
            g.fill(x, y, x + w, y + h - 1, PvpTheme.mix(PvpTheme.panel(), PvpTheme.accent(), 0.22f));
            g.fill(x, y, x + 2, y + h - 1, PvpTheme.accent());
        } else if (hov) g.fill(x, y, x + w, y + h - 1, PvpTheme.hoverFill());
        Social.Icon ic = Social.icon(iconId);
        int tx = x + 5;
        if (ic != null && !"none".equals(iconId)) {
            g.drawString(font, Component.object(new net.minecraft.network.chat.contents.objects.AtlasSprite(ic.atlas(), ic.sprite())), tx, y + 3, 0xFFFFFFFF, false);
        } else {
            g.drawString(font, "-", tx + 2, y + 3, PvpTheme.faint(), false);
        }
        g.drawString(font, PvpTheme.fit(font, label, w - 24), tx + 12, y + 3, sel ? PvpTheme.text() : PvpTheme.dim(), false);
        hits.add(new Hit(x, y, w, h - 1, b -> {
            target = key;
            scrollI = 0;
            String o = key == null ? null : dev.cod.pvpcod.Tiers.overrides().get(key);
            tierMode = 0;
            if (o != null) for (int i = 0; i < dev.cod.pvpcod.Tiers.MODES.length; i++) if (o.startsWith(dev.cod.pvpcod.Tiers.MODES[i] + ":")) tierMode = i;
        }, null));
        return y + h;
    }

    /** Shows the name as the player spells it if they're online, else as stored. */
    private String displayName(String lower) {
        for (String p : Social.onlinePlayers()) if (p.equalsIgnoreCase(lower)) return p;
        return lower;
    }
}
