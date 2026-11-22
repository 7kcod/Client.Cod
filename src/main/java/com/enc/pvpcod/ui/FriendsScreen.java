package com.enc.pvpcod.ui;

import com.enc.packcod.ui.PvpTheme;
import com.enc.pvpcod.PvpCod;
import com.enc.pvpcod.Social;
import com.enc.pvpcod.module.Modules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Your friends list, and everyone online so you can add them with one click.
 * The same page is used for your team (team and friends are separate lists).
 */
public final class FriendsScreen extends UiScreen {

    private final boolean team;

    private EditBox name;
    private int scrollF, scrollO;
    private String status = "";
    private int statusColor;

    public FriendsScreen(Screen parent) {
        this(parent, false);
    }

    public FriendsScreen(Screen parent, boolean team) {
        super(team ? "Team" : "Friends", parent);
        this.team = team;
    }

    private List<String> list() { return team ? Social.team() : Social.friends(); }

    private boolean has(String n) { return team ? Social.isTeam(n) : Social.isFriend(n); }

    private boolean addName(String n) { return team ? Social.addTeam(n) : Social.addFriend(n); }

    private void removeName(String n) {
        if (team) Social.removeTeam(n);
        else Social.removeFriend(n);
    }

    private boolean toggle(String n) { return team ? Social.toggleTeam(n) : Social.toggleFriend(n); }

    private int colW() {
        return Math.min(220, (width - M * 3) / 2);
    }

    @Override
    protected void init() {
        int cw = colW();
        int x = width / 2 - cw - M / 2;
        name = new EditBox(font, x + 8, TOP + M + 26, cw - 70, 10, Component.literal("Name"));
        searchBox(name);
        name.setMaxLength(16);
        name.setHint(Component.literal("Player name"));
        addRenderableWidget(name);
    }

    private void add() {
        String n = name.getValue().trim();
        if (n.isEmpty()) return;
        if (has(n)) say(n + (team ? " is already on your team" : " is already a friend"), PvpTheme.dim());
        else if (addName(n)) {
            say("Added " + n, PvpTheme.good());
            name.setValue("");
        } else say("That isn't a Minecraft name", PvpTheme.bad());
    }

    private void say(String s, int c) {
        status = s;
        statusColor = c;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (name.isFocused() && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            add();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected void draw(GuiGraphics g, int mx, int my) {
        topBar(g, team ? "Team" : "Friends");
        String back = "Back";
        button(g, width - M - bw(back), 3, bw(back), 16, back, mx, my, false, b -> onClose());
        String st = team ? "Ping settings" : "Friend settings";
        button(g, width - M * 2 - bw(back) - bw(st), 3, bw(st), 16, st, mx, my, false,
                b -> minecraft.setScreen(new ModuleScreen(this, team ? Modules.PINGS : Modules.FRIENDS)));
        String other = team ? "Friends" : "Team";
        button(g, width - M * 3 - bw(back) - bw(st) - bw(other), 3, bw(other), 16, other, mx, my, false,
                b -> minecraft.setScreen(new FriendsScreen(parent, !team)));

        int cw = colW();
        int lx = width / 2 - cw - M / 2, rx = width / 2 + M / 2;
        int y = TOP + M, h = height - y - M;

        // ---- your friends
        PvpTheme.panel(g, lx, y, cw, h);
        List<String> friends = list();
        PvpTheme.label(g, font, (team ? "Team (" : "Friends (") + friends.size() + ")", lx + 8, y + 8);
        PvpTheme.slot(g, name.getX() - 4, name.getY() - 3, name.getWidth() + 8, 15);
        button(g, lx + cw - 58, name.getY() - 4, 50, 16, "Add", mx, my, true, b -> add());
        if (!status.isEmpty()) g.drawString(font, PvpTheme.fit(font, status, cw - 16), lx + 8, y + 44, statusColor, false);
        int listY = y + 58, rowH = 16;
        int vis = Math.max(1, (y + h - 18 - listY) / rowH);
        scrollF = clamp(scrollF, 0, Math.max(0, friends.size() - vis));
        wheels.add(new Wheel(lx, listY, cw, vis * rowH, d -> scrollF = clamp(scrollF + d, 0, Math.max(0, friends.size() - vis))));
        if (friends.isEmpty()) g.drawString(font, team ? "Nobody on your team yet." : "No friends yet.", lx + 8, listY + 4, PvpTheme.faint(), false);
        for (int i = scrollF; i < friends.size() && i < scrollF + vis; i++) {
            String f = friends.get(i);
            int ry = listY + (i - scrollF) * rowH;
            boolean hov = in(mx, my, lx + 4, ry, cw - 8, rowH - 1);
            if (hov) g.fill(lx + 4, ry, lx + cw - 4, ry + rowH - 1, PvpTheme.hoverFill());
            g.drawString(font, Social.decorate(f, Component.literal(f)), lx + 8, ry + 4, PvpTheme.text(), false);
            boolean xh = in(mx, my, lx + cw - 20, ry, 14, rowH - 1);
            g.drawString(font, "x", lx + cw - 15, ry + 4, xh ? PvpTheme.bad() : PvpTheme.faint(), false);
            hits.add(new Hit(lx + cw - 20, ry, 14, rowH - 1, b -> {
                removeName(f);
                say("Removed " + f, PvpTheme.dim());
            }, null));
        }
        scrollbar(g, lx + cw - 4, listY, vis * rowH, scrollF, vis, friends.size());
        String tip = team ? "Team members get your pings (middle-click in survival)"
                : "Tip: sneak + middle-click a player to add them (" + PvpCod.friendKey.getTranslatedKeyMessage().getString() + ")";
        g.drawString(font, PvpTheme.fit(font, tip, cw - 16), lx + 8, y + h - 13, PvpTheme.faint(), false);

        // ---- online now
        PvpTheme.panel(g, rx, y, cw, h);
        List<String> online = Social.onlinePlayers();
        PvpTheme.label(g, font, "Online now (" + online.size() + ")", rx + 8, y + 8);
        int oy = y + 24;
        int ovis = Math.max(1, (y + h - 6 - oy) / rowH);
        scrollO = clamp(scrollO, 0, Math.max(0, online.size() - ovis));
        wheels.add(new Wheel(rx, oy, cw, ovis * rowH, d -> scrollO = clamp(scrollO + d, 0, Math.max(0, online.size() - ovis))));
        if (online.isEmpty()) g.drawString(font, minecraft.level == null ? "Join a world or server to see players." : "Nobody else here.",
                rx + 8, oy + 4, PvpTheme.faint(), false);
        String me = minecraft.getUser().getName();
        for (int i = scrollO; i < online.size() && i < scrollO + ovis; i++) {
            String p = online.get(i);
            int ry = oy + (i - scrollO) * rowH;
            boolean friend = has(p), self = p.equalsIgnoreCase(me);
            boolean hov = in(mx, my, rx + 4, ry, cw - 8, rowH - 1);
            if (hov && !self) g.fill(rx + 4, ry, rx + cw - 4, ry + rowH - 1, PvpTheme.hoverFill());
            g.drawString(font, Social.decorate(p, Component.literal(p)), rx + 8, ry + 4, self ? PvpTheme.faint() : PvpTheme.text(), false);
            if (self) {
                g.drawString(font, "you", rx + cw - 8 - font.width("you"), ry + 4, PvpTheme.faint(), false);
                continue;
            }
            String act = friend ? "Remove" : team ? "+ Team" : "+ Friend";
            g.drawString(font, act, rx + cw - 8 - font.width(act), ry + 4, hov ? (friend ? PvpTheme.bad() : PvpTheme.accent()) : PvpTheme.dim(), false);
            hits.add(new Hit(rx + 4, ry, cw - 8, rowH - 1, b -> {
                boolean now = toggle(p);
                say(now ? "Added " + p : "Removed " + p, now ? PvpTheme.good() : PvpTheme.dim());
            }, null));
        }
        scrollbar(g, rx + cw - 4, oy, ovis * rowH, scrollO, ovis, online.size());
    }
}
