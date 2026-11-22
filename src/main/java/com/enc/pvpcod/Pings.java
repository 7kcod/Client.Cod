package com.enc.pvpcod;

import com.enc.pvpcod.module.Modules;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fortnite-style pings for your team. Middle-click (in survival) where you're looking: a marker
 * shows up for you, and the coordinates go to every team member online as a /msg. Team members
 * with pvp.cod see a marker with your head on it; everyone else just sees the coordinates in chat.
 */
public final class Pings {

    public record Ping(String from, Vec3 pos, long at, boolean enemy) {}

    private static final List<Ping> PINGS = new ArrayList<>();
    private static final Pattern MSG = Pattern.compile("\\[ping\\] (-?\\d+) (-?\\d+) (-?\\d+)");
    private static long lastSent;
    private static String lastSentCoords = "";
    /** Last real field of view (set by the FOV mixin), for placing markers on screen. */
    public static float fov = 70;

    /** Middle click: ping where you're looking. */
    public static void ping(Minecraft mc) {
        var pm = Modules.PINGS;
        if (mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastSent < pm.cooldown.value * 1000) return;
        HitResult hit = mc.hitResult;
        Vec3 pos;
        boolean enemy = false;
        if (hit instanceof EntityHitResult eh) {
            pos = eh.getEntity().position().add(0, eh.getEntity().getBbHeight(), 0);
            enemy = eh.getEntity() instanceof net.minecraft.world.entity.player.Player p
                    && !Social.isTeam(p.getGameProfile().name()) && !Social.isFriend(p.getGameProfile().name());
        } else {
            HitResult far = mc.player.pick(pm.range.value, 1f, false);
            if (far.getType() == HitResult.Type.MISS) return;
            pos = far.getLocation();
            if (far instanceof BlockHitResult bh) pos = Vec3.atCenterOf(bh.getBlockPos()).add(0, 0.5, 0);
        }
        lastSent = now;
        String me = mc.getUser().getName();
        add(new Ping(me, pos, now, enemy), mc);
        int x = (int) Math.floor(pos.x), y = (int) Math.floor(pos.y), z = (int) Math.floor(pos.z);
        String coords = x + " " + y + " " + z;
        lastSentCoords = coords;
        if (pm.sendChat.value && mc.getConnection() != null) {
            int sent = 0;
            for (String member : Social.team()) {
                if (member.equalsIgnoreCase(me) || mc.getConnection().getPlayerInfo(member) == null) continue;
                mc.getConnection().sendCommand("msg " + member + " [ping] " + coords + (enemy ? " enemy" : ""));
                sent++;
            }
            if (sent == 0 && Social.team().isEmpty()) {
                mc.player.displayClientMessage(Component.literal("Add team members on the Team page to send them pings").withColor(0xE8A33D), true);
            }
        }
    }

    private static void add(Ping p, Minecraft mc) {
        // one marker per person
        PINGS.removeIf(o -> o.from().equalsIgnoreCase(p.from()));
        PINGS.add(p);
        if (Modules.PINGS.sound.value) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL, 1.6f));
    }

    /** Reads incoming chat for pings from team members. Called for every chat line. */
    public static void onChat(Component message) {
        var pm = Modules.PINGS;
        if (!pm.enabled || message == null) return;
        String text = message.getString();
        Matcher m = MSG.matcher(text);
        if (!m.find()) return;
        Minecraft mc = Minecraft.getInstance();
        String coords = m.group(1) + " " + m.group(2) + " " + m.group(3);
        // our own "You whisper to X: [ping] ..." echo
        if (coords.equals(lastSentCoords) && System.currentTimeMillis() - lastSent < 3000) return;
        String before = text.substring(0, m.start());
        String from = null;
        for (String member : Social.team()) {
            if (before.toLowerCase().contains(member.toLowerCase())) {
                from = member;
                break;
            }
        }
        if (from == null) return; // only team members can ping you
        Vec3 pos = new Vec3(Integer.parseInt(m.group(1)) + 0.5, Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)) + 0.5);
        add(new Ping(from, pos, System.currentTimeMillis(), text.substring(m.end()).trim().startsWith("enemy")), mc);
    }

    // ------------------------------------------------------------------ drawing

    public static void render(GuiGraphics g, Minecraft mc) {
        var pm = Modules.PINGS;
        if (!pm.enabled || mc.player == null || PINGS.isEmpty()) return;
        long now = System.currentTimeMillis();
        long life = (long) (pm.duration.value * 1000);
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 c = cam.position();
        double yaw = Math.toRadians(cam.yRot()), pitch = Math.toRadians(cam.xRot());
        // camera axes
        double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        double ux = -Math.sin(yaw) * Math.sin(pitch), uy = Math.cos(pitch), uz = Math.cos(yaw) * Math.sin(pitch);
        int w = g.guiWidth(), h = g.guiHeight();
        double f = (h / 2.0) / Math.tan(Math.toRadians(fov) / 2);
        Font font = mc.font;
        for (Iterator<Ping> it = PINGS.iterator(); it.hasNext(); ) {
            Ping p = it.next();
            long age = now - p.at();
            if (age > life) {
                it.remove();
                continue;
            }
            float fade = Math.min(1f, (life - age) / 800f) * Math.min(1f, age / 120f + 0.3f);
            double dx = p.pos().x - c.x, dy = p.pos().y - c.y, dz = p.pos().z - c.z;
            double cx = dx * rx + dz * rz, cy = dx * ux + dy * uy + dz * uz, cz = dx * fx + dy * fy + dz * fz;
            double sx, sy;
            boolean edge = false;
            if (cz > 0.1) {
                sx = w / 2.0 + cx / cz * f;
                sy = h / 2.0 - cy / cz * f;
            } else {
                // behind you: push it to the edge in the right direction
                sx = w / 2.0 + (cx >= 0 ? 1 : -1) * w;
                sy = h / 2.0 - cy * 10;
            }
            int m = 14;
            if (sx < m || sx > w - m || sy < m || sy > h - m) {
                edge = true;
                sx = Math.max(m, Math.min(w - m, sx));
                sy = Math.max(m + 10, Math.min(h - m - 14, sy));
            }
            draw(g, font, mc, p, (int) sx, (int) sy, Math.sqrt(dx * dx + dy * dy + dz * dz), fade, edge);
        }
    }

    private static void draw(GuiGraphics g, Font font, Minecraft mc, Ping p, int x, int y, double dist, float fade, boolean edge) {
        var pm = Modules.PINGS;
        int col = p.enemy() ? pm.enemyColor.value : pm.color.value;
        int a = Math.round(fade * 255);
        int colA = (a << 24) | (col & 0xFFFFFF);
        // diamond marker with a little bounce when it appears
        float pop = 1f + Math.max(0, 0.6f - (System.currentTimeMillis() - p.at()) / 300f);
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(pop, pop);
        g.pose().rotate((float) Math.toRadians(45));
        g.fill(-6, -6, 6, 6, (Math.round(a * 0.9f) << 24));
        g.fill(-5, -5, 5, 5, colA);
        g.fill(-3, -3, 3, 3, (Math.round(a * 0.5f) << 24) | 0xFFFFFF);
        g.pose().popMatrix();
        // the pinger's head, small and see-through, above the marker
        if (pm.heads.value) {
            PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(p.from());
            Identifier skin = info != null ? info.getSkin().body().texturePath()
                    : mc.player != null && p.from().equalsIgnoreCase(mc.getUser().getName()) ? mc.player.getSkin().body().texturePath() : null;
            if (skin != null) {
                int ha = Math.round(fade * pm.headOpacity.value / 100f * 255);
                int tint = (ha << 24) | 0xFFFFFF;
                g.blit(RenderPipelines.GUI_TEXTURED, skin, x - 6, y - 22, 8f, 8f, 12, 12, 8, 8, 64, 64, tint);
                g.blit(RenderPipelines.GUI_TEXTURED, skin, x - 6, y - 22, 40f, 8f, 12, 12, 8, 8, 64, 64, tint);
            }
        }
        String label = p.from() + "  " + Math.round(dist) + "m";
        int lw = font.width(label);
        int ty = y + 10;
        g.fill(x - lw / 2 - 2, ty - 1, x + lw / 2 + 2, ty + 9, (Math.round(a * 0.5f) << 24));
        g.drawString(font, label, x - lw / 2, ty, (a << 24) | 0xFFFFFF, false);
        if (pm.coords.value && !edge) {
            String cs = (int) Math.floor(p.pos().x) + " " + (int) Math.floor(p.pos().y) + " " + (int) Math.floor(p.pos().z);
            g.drawString(font, cs, x - font.width(cs) / 2, ty + 10, (Math.round(a * 0.7f) << 24) | 0xDDDDDD, true);
        }
    }

    private Pings() {
    }
}
