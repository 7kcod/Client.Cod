package dev.cod.pvpcod.ui;

import dev.cod.packcod.ui.PvpTheme;
import dev.cod.pvpcod.Social;
import dev.cod.pvpcod.module.Module;
import dev.cod.pvpcod.module.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Little live pictures of what a module does, drawn on its settings page.
 * Most are "Off | On" side by side so you can see the difference as you move the sliders.
 */
final class Previews {

    private static final Identifier BLOCKS = Identifier.withDefaultNamespace("blocks");
    private static final ItemStack GRASS = new ItemStack(Items.GRASS_BLOCK);
    private static final ItemStack LOG = new ItemStack(Items.OAK_LOG);
    private static final ItemStack STONE = new ItemStack(Items.STONE);
    private static final ItemStack SHIELD = new ItemStack(Items.SHIELD);
    private static final ItemStack SWORD = new ItemStack(Items.DIAMOND_SWORD);

    /** Height the preview wants, or 0 if this module has no preview. */
    static int height(Module m) {
        if (m == Modules.HITBOXES) return 96;
        if (m == Modules.LOW_FIRE || m == Modules.CLEAR_WATER || m == Modules.CLEAR_LAVA || m == Modules.SIDE_SHIELD
                || m == Modules.ZOOM || m == Modules.FULLBRIGHT || m == Modules.NO_HURT_CAM) return 90;
        if (m == Modules.NAME_ICONS || m == Modules.FRIENDS) return 56;
        if (m == Modules.CROSSHAIR || m == Modules.ASPECT || m == Modules.PINGS) return 80;
        if (m == Modules.COSMETICS) return 120;
        if (m == Modules.BLOCK_OUTLINE) return 80;
        if (m == Modules.SHULKER) return 70;
        if (m == Modules.TIERS) return 56;
        return 0;
    }

    static void draw(Module m, GuiGraphics g, Font font, int x, int y, int w, int h) {
        PvpTheme.slot(g, x, y, w, h);
        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        try {
            if (m == Modules.HITBOXES) hitboxes(g, font, x, y, w, h);
            else if (m == Modules.NAME_ICONS || m == Modules.FRIENDS) nametags(g, font, x, y, w, h);
            else if (m == Modules.CROSSHAIR) crosshairs(g, font, x, y, w, h);
            else if (m == Modules.COSMETICS) cosmetics(g, font, x, y, w, h);
            else if (m == Modules.BLOCK_OUTLINE) outline(g, font, x, y, w, h);
            else if (m == Modules.SHULKER) shulker(g, font, x, y, w, h);
            else if (m == Modules.PINGS) pings(g, font, x, y, w, h);
            else if (m == Modules.TIERS) tiers(g, font, x, y, w, h);
            else split(m, g, font, x, y, w, h);
        } finally {
            g.disableScissor();
        }
    }

    // ------------------------------------------------------------------ off | on

    private static void split(Module m, GuiGraphics g, Font font, int x, int y, int w, int h) {
        int half = (w - 6) / 2;
        int lx = x + 2, rx = x + 4 + half, fy = y + 2, fh = h - 4;
        frame(m, g, font, lx, fy, half, fh, false);
        frame(m, g, font, rx, fy, half, fh, true);
        tag(g, font, "Off", lx + 3, fy + 3);
        tag(g, font, m.enabled ? "On" : "On (switch it on to use)", rx + 3, fy + 3);
    }

    private static void tag(GuiGraphics g, Font font, String s, int x, int y) {
        g.fill(x - 1, y - 1, x + font.width(s) + 2, y + 9, 0xA0000000);
        g.drawString(font, s, x + 1, y, 0xFFFFFFFF, false);
    }

    /** One half of a split preview. on = with the module's settings applied. */
    private static void frame(Module m, GuiGraphics g, Font font, int x, int y, int w, int h, boolean on) {
        g.enableScissor(x, y, x + w, y + h);
        if (m == Modules.LOW_FIRE) {
            scene(g, x, y, w, h);
            var lf = Modules.LOW_FIRE;
            if (!(on && lf.hide.value)) {
                float drop = on ? lf.lower.value : 0f;
                int fw = Math.round(w * 0.42f), fh = Math.round(h * 0.95f);
                int fy = y + h - fh + Math.round(drop * h * 1.2f);
                sprite(g, font, "block/fire_1", x - fw / 6, fy, fw, fh);
                sprite(g, font, "block/fire_1", x + w - fw + fw / 6, fy, fw, fh);
            }
        } else if (m == Modules.CLEAR_WATER) {
            var cw = Modules.CLEAR_WATER;
            scene(g, x, y, w, h);
            float fog = on ? Math.max(0f, 0.92f - cw.view.value / 100f * 0.9f) : 0.92f;
            fogLayers(g, x, y, w, h, 0xFF1A3C8A, fog);
            if (!(on && cw.overlay.value)) g.fill(x, y, x + w, y + h, 0x352050C0);
        } else if (m == Modules.CLEAR_LAVA) {
            var cl = Modules.CLEAR_LAVA;
            scene(g, x, y, w, h);
            float fog = on ? Math.max(0f, 0.97f - cl.view.value / 100f * 0.95f) : 0.97f;
            fogLayers(g, x, y, w, h, 0xFFB83A0A, fog);
        } else if (m == Modules.FULLBRIGHT) {
            scene(g, x, y, w, h);
            if (!on) g.fill(x, y, x + w, y + h, 0xC0000008);
        } else if (m == Modules.NO_HURT_CAM) {
            g.pose().pushMatrix();
            if (!on) {
                g.pose().translate(x + w / 2f, y + h / 2f);
                g.pose().rotate((float) Math.toRadians(-8));
                g.pose().scale(1.15f, 1.15f);
                g.pose().translate(-(x + w / 2f), -(y + h / 2f));
            }
            scene(g, x, y, w, h);
            g.pose().popMatrix();
            if (!on) g.fill(x, y, x + w, y + h, 0x30FF0000);
        } else if (m == Modules.ZOOM) {
            float f = on ? Modules.ZOOM.factor.value : 1f;
            g.pose().pushMatrix();
            g.pose().translate(x + w / 2f, y + h / 2f);
            g.pose().scale(f, f);
            g.pose().translate(-(x + w / 2f), -(y + h / 2f));
            scene(g, x, y, w, h);
            g.pose().popMatrix();
            crosshair(g, x, y, w, h);
        } else if (m == Modules.ASPECT) {
            // the world drawn for a narrower shape, stretched sideways to fill the screen
            float stretch = on ? (16f / 9f) / Modules.ASPECT.value() : 1f;
            g.pose().pushMatrix();
            g.pose().translate(x + w / 2f, y + h / 2f);
            g.pose().scale(stretch, 1f);
            g.pose().translate(-(x + w / 2f), -(y + h / 2f));
            scene(g, x, y, w, h);
            g.pose().popMatrix();
            crosshair(g, x, y, w, h);
        } else if (m == Modules.SIDE_SHIELD) {
            scene(g, x, y, w, h);
            crosshair(g, x, y, w, h);
            var ss = Modules.SIDE_SHIELD;
            float size = h * 0.75f;
            float sx = x + w * 0.04f, sy = y + h * 0.42f;
            if (on) {
                size *= ss.size.value;
                sx -= ss.x.value * w * 0.8f;
                sy += ss.y.value * h * 0.9f;
            }
            item(g, SHIELD, sx, sy, size);
            item(g, SWORD, x + w - h * 0.62f, y + h * 0.45f, h * 0.6f);
        }
        g.disableScissor();
    }

    /** Sky, a hill, and blocks near and far. */
    private static void scene(GuiGraphics g, int x, int y, int w, int h) {
        for (int i = 0; i < h; i++) {
            float t = i / (float) h;
            g.fill(x, y + i, x + w, y + i + 1, mixRgb(0xFF79A6FF, 0xFFC4DBFF, t));
        }
        int ground = y + h * 2 / 3;
        g.fill(x, ground, x + w, y + h, 0xFF5D9C3A);
        g.fill(x, ground, x + w, ground + 2, 0xFF77B84F);
        item(g, STONE, x + w * 0.62f, ground - h * 0.12f, h * 0.12f);
        item(g, LOG, x + w * 0.2f, ground - h * 0.22f, h * 0.22f);
        item(g, GRASS, x + w * 0.42f, ground - h * 0.05f, h * 0.38f);
    }

    /** Fog that gets thicker with distance: far things fade first. */
    private static void fogLayers(GuiGraphics g, int x, int y, int w, int h, int color, float amount) {
        if (amount <= 0) return;
        int a = Math.round(Math.min(1f, amount) * 255);
        g.fill(x, y, x + w, y + h, (Math.round(a * 0.55f) << 24) | (color & 0xFFFFFF));
        // a second, lighter layer over the top (far away) part
        g.fill(x, y, x + w, y + h * 2 / 3, (Math.round(a * 0.45f) << 24) | (color & 0xFFFFFF));
    }

    private static void crosshair(GuiGraphics g, int x, int y, int w, int h) {
        int cx = x + w / 2, cy = y + h / 2;
        g.fill(cx - 4, cy, cx + 5, cy + 1, 0xFFFFFFFF);
        g.fill(cx, cy - 4, cx + 1, cy + 5, 0xFFFFFFFF);
    }

    private static void item(GuiGraphics g, ItemStack s, float x, float y, float size) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(size / 16f, size / 16f);
        g.renderItem(s, 0, 0);
        g.pose().popMatrix();
    }

    /** Draws a block-atlas sprite (from whatever pack you have on) at any size. */
    private static void sprite(GuiGraphics g, Font font, String sprite, int x, int y, int w, int h) {
        Component c = Component.object(new AtlasSprite(BLOCKS, Identifier.withDefaultNamespace(sprite)));
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(w / 8f, h / 8f);
        g.drawString(font, c, 0, 0, 0xFFFFFFFF, false);
        g.pose().popMatrix();
    }

    // ------------------------------------------------------------------ crosshair

    private static void crosshairs(GuiGraphics g, Font font, int x, int y, int w, int h) {
        int half = (w - 6) / 2;
        int lx = x + 2, rx = x + 4 + half, fy = y + 2, fh = h - 4;
        for (int i = 0; i < 2; i++) {
            int fx = i == 0 ? lx : rx;
            g.enableScissor(fx, fy, fx + half, fy + fh);
            scene(g, fx, fy, half, fh);
            if (i == 1) figure(g, fx + half / 2, fy + fh / 2 - fh / 3, fh * 2 / 3);
            // drawn twice as big so you can see the detail
            g.pose().pushMatrix();
            g.pose().translate(fx + half / 2f, fy + fh / 2f);
            g.pose().scale(2f, 2f);
            Modules.CROSSHAIR.draw(g, 0, 0, i == 1);
            g.pose().popMatrix();
            g.disableScissor();
            tag(g, font, i == 0 ? "Aiming at nothing" : "On a player", fx + 3, fy + 3);
        }
    }

    // ------------------------------------------------------------------ block outline

    private static void outline(GuiGraphics g, Font font, int x, int y, int w, int h) {
        scene(g, x + 1, y + 1, w - 2, h - 2);
        var bo = Modules.BLOCK_OUTLINE;
        int size = h - 24;
        int bx = x + (w - size) / 2, by = y + 10;
        item(g, STONE, bx, by, size);
        // the outline around the block's front face (roughly where the game draws it)
        int pad = size / 10;
        int x0 = bx + pad, y0 = by + pad, x1 = bx + size - pad, y1 = by + size - pad;
        if (bo.fill.value) g.fill(x0, y0, x1, y1, bo.fillNow());
        int t = Math.max(1, Math.round(bo.thickness.value / 2f));
        int c = bo.outline();
        g.fill(x0 - t, y0 - t, x1 + t, y0, c);
        g.fill(x0 - t, y1, x1 + t, y1 + t, c);
        g.fill(x0 - t, y0, x0, y1, c);
        g.fill(x1, y0, x1 + t, y1, c);
    }

    // ------------------------------------------------------------------ cosmetics

    private static void cosmetics(GuiGraphics g, Font font, int x, int y, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            String s = "Join a world to see yourself wearing them";
            g.drawString(font, s, x + (w - font.width(s)) / 2, y + h / 2 - 4, PvpTheme.faint(), false);
            return;
        }
        int mx = (int) (mc.mouseHandler.xpos() * g.guiWidth() / mc.getWindow().getScreenWidth());
        int my = (int) (mc.mouseHandler.ypos() * g.guiHeight() / mc.getWindow().getScreenHeight());
        net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 2, y + 2, x + w - 2, y + h - 2,
                (h - 10) / 3, 0.0625f, mx, my, mc.player);
        String s = "Move your mouse to look around";
        g.drawString(font, s, x + 4, y + h - 12, PvpTheme.faint(), false);
    }

    // ------------------------------------------------------------------ shulker

    private static final ItemStack[] SHULKER_SAMPLE = {new ItemStack(Items.TOTEM_OF_UNDYING), new ItemStack(Items.GOLDEN_APPLE, 32),
            new ItemStack(Items.ENDER_PEARL, 16), new ItemStack(Items.END_CRYSTAL, 64), new ItemStack(Items.OBSIDIAN, 64),
            new ItemStack(Items.EXPERIENCE_BOTTLE, 64), new ItemStack(Items.NETHERITE_SWORD), new ItemStack(Items.COBWEB, 16)};

    private static void shulker(GuiGraphics g, Font font, int x, int y, int w, int h) {
        int gw = 9 * 18 + 2, gh = 3 * 18 + 2;
        int gx = x + (w - gw) / 2, gy = y + (h - gh) / 2;
        int col = Modules.SHULKER.tint.value ? 0x998E6B8E : 0x66000000;
        g.fill(gx, gy, gx + gw, gy + gh, col);
        for (int i = 0; i < 27; i++) {
            int sx = gx + 1 + (i % 9) * 18, sy = gy + 1 + (i / 9) * 18;
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x30FFFFFF);
            if (i % 3 == 0 && i / 3 < SHULKER_SAMPLE.length) {
                ItemStack s = SHULKER_SAMPLE[i / 3];
                g.renderItem(s, sx + 1, sy + 1);
                g.renderItemDecorations(font, s, sx + 1, sy + 1);
            }
        }
    }

    // ------------------------------------------------------------------ pings

    private static void pings(GuiGraphics g, Font font, int x, int y, int w, int h) {
        scene(g, x + 1, y + 1, w - 2, h - 2);
        var pm = Modules.PINGS;
        drawMarker(g, font, x + w / 3, y + h / 2 - 4, "Teammate", pm.color.value, "112 64 -30");
        drawMarker(g, font, x + w * 2 / 3, y + h / 2 + 4, "Enemy", pm.enemyColor.value, "140 66 -12");
    }

    private static void drawMarker(GuiGraphics g, Font font, int mx, int my, String who, int col, String coords) {
        var pm = Modules.PINGS;
        g.pose().pushMatrix();
        g.pose().translate(mx, my);
        g.pose().rotate((float) Math.toRadians(45));
        g.fill(-6, -6, 6, 6, 0xE6000000);
        g.fill(-5, -5, 5, 5, col);
        g.fill(-3, -3, 3, 3, 0x80FFFFFF);
        g.pose().popMatrix();
        if (pm.heads.value) {
            int a = Math.round(pm.headOpacity.value / 100f * 255);
            g.fill(mx - 6, my - 22, mx + 6, my - 10, (a << 24) | 0xB98A6A);
            g.fill(mx - 6, my - 22, mx + 6, my - 19, (a << 24) | 0x3B2716);
        }
        String label = who + "  24m";
        g.fill(mx - font.width(label) / 2 - 2, my + 9, mx + font.width(label) / 2 + 2, my + 19, 0x80000000);
        g.drawString(font, label, mx - font.width(label) / 2, my + 10, 0xFFFFFFFF, false);
        if (pm.coords.value) g.drawString(font, coords, mx - font.width(coords) / 2, my + 20, 0xB0DDDDDD, true);
    }

    // ------------------------------------------------------------------ tiers

    private static void tiers(GuiGraphics g, Font font, int x, int y, int w, int h) {
        String[] tiers = {"HT1", "LT3", "HT5"};
        String[] names = {"Champion", "Player", "Newbie"};
        int cell = (w - 4) / 3;
        var tt = Modules.TIERS;
        String mode = dev.cod.pvpcod.Tiers.MODE_SHORT[tt.mode.index];
        for (int i = 0; i < 3; i++) {
            int cx = x + 2 + i * cell + cell / 2;
            Component tag = Component.literal("[").withColor(0x777777)
                    .append(Component.literal(tiers[i]).withColor(dev.cod.pvpcod.Tiers.color(tiers[i])))
                    .append(tt.showMode.value ? Component.literal(" " + mode).withColor(0xAAAAAA) : Component.empty())
                    .append(Component.literal("]").withColor(0x777777));
            Component full = tt.before.value ? Component.empty().append(tag).append(" " + names[i])
                    : Component.literal(names[i] + " ").append(tag);
            int tw = font.width(full);
            g.fill(cx - tw / 2 - 2, y + 10, cx + tw / 2 + 2, y + 21, 0x40000000);
            g.drawString(font, full, cx - tw / 2, y + 12, 0xFFFFFFFF, false);
        }
        String s = "Showing: " + dev.cod.pvpcod.Tiers.MODE_NAMES[tt.mode.index];
        g.drawString(font, s, x + (w - font.width(s)) / 2, y + h - 14, PvpTheme.faint(), false);
    }

    // ------------------------------------------------------------------ hitboxes

    private static void hitboxes(GuiGraphics g, Font font, int x, int y, int w, int h) {
        var hb = Modules.HITBOXES;
        String[] labels = {"Normal", "Looking at", "Friend"};
        int[] colors = {hb.box.value, hb.target.value, hb.friend.value};
        int cell = (w - 4) / 3;
        int figH = h - 26;
        for (int i = 0; i < 3; i++) {
            int cx = x + 2 + i * cell + cell / 2;
            int top = y + 6, bottom = top + figH;
            figure(g, cx, top, figH);
            int bw = Math.round(figH * 0.6f / 1.8f);
            PvpTheme.outline(g, cx - bw / 2 - 1, top - 1, bw + 2, figH + 2, colors[i]);
            int eyeY = top + Math.round(figH * (1 - 1.62f / 1.8f));
            g.fill(cx - bw / 2 - 1, eyeY, cx + bw / 2 + 1, eyeY + 1, hb.eye.value);
            if (hb.look.value) {
                int len = Math.round(figH * 0.45f);
                g.fill(cx, eyeY, cx + len, eyeY + 1, 0xFF0000FF);
                g.fill(cx + len - 2, eyeY - 2, cx + len - 1, eyeY + 3, 0xFF0000FF);
                g.fill(cx + len - 3, eyeY - 1, cx + len - 2, eyeY + 2, 0xFF0000FF);
            }
            String l = labels[i];
            g.drawString(font, l, cx - font.width(l) / 2, bottom + 6, PvpTheme.dim(), false);
        }
    }

    /** A simple Steve-coloured player shape, h pixels tall (32 "pixels" of model). */
    private static void figure(GuiGraphics g, int cx, int top, int h) {
        float p = h / 32f;
        int hx = Math.round(4 * p);
        // head
        rect(g, cx - hx, top, cx + hx, top + Math.round(8 * p), 0xFFB98A6A);
        rect(g, cx - hx, top, cx + hx, top + Math.round(2 * p), 0xFF3B2716);
        // body + arms
        int by = top + Math.round(8 * p), bb = top + Math.round(20 * p);
        rect(g, cx - hx, by, cx + hx, bb, 0xFF2F9FA3);
        rect(g, cx - hx - Math.round(4 * p), by, cx - hx, bb, 0xFFB98A6A);
        rect(g, cx + hx, by, cx + hx + Math.round(4 * p), bb, 0xFFB98A6A);
        // legs
        rect(g, cx - hx, bb, cx + hx, top + h, 0xFF3A3F8F);
        rect(g, cx - 1, bb, cx, top + h, 0xFF2A2E6A);
    }

    private static void rect(GuiGraphics g, int x0, int y0, int x1, int y1, int c) {
        g.fill(x0, y0, x1, y1, c);
    }

    // ------------------------------------------------------------------ name tags

    private static void nametags(GuiGraphics g, Font font, int x, int y, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        String me = mc.getUser().getName();
        List<String> friends = Social.friends();
        String friend = friends.isEmpty() ? null : friends.get(0);
        String[] names = {me, friend, "Stranger"};
        String[] notes = {"You", friend == null ? "Add a friend to see theirs" : "Friend", "Anyone else"};
        int cell = (w - 4) / 3;
        for (int i = 0; i < 3; i++) {
            int cx = x + 2 + i * cell + cell / 2;
            Component tag = names[i] == null ? Component.literal("-") : Social.decorate(names[i], Component.literal(names[i]));
            int tw = font.width(tag);
            int ty = y + 12;
            g.fill(cx - tw / 2 - 2, ty - 2, cx + tw / 2 + 2, ty + 9, 0x40000000);
            g.drawString(font, tag, cx - tw / 2, ty, 0xFFFFFFFF, false);
            String n = PvpTheme.fit(font, notes[i], cell - 6);
            g.drawString(font, n, cx - font.width(n) / 2, y + h - 14, PvpTheme.faint(), false);
        }
    }

    private static int mixRgb(int a, int b, float t) {
        return PvpTheme.mix(a, b, t);
    }

    private Previews() {
    }
}
