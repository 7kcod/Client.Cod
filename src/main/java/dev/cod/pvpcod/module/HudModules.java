package dev.cod.pvpcod.module;

import dev.cod.pvpcod.Tracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** All the HUD modules. */
public final class HudModules {

    static Minecraft mc() {
        return Minecraft.getInstance();
    }

    // ------------------------------------------------------------------ keystrokes

    public static final class Keystrokes extends HudModule {
        final Setting.Num keySize = num("keysize", "Key size", 14, 32, 1, 20, "px");
        final Setting.Num gap = num("gap", "Gap", 0, 6, 1, 2, "px");
        final Setting.Bool mouse = bool("mouse", "Mouse buttons", true);
        final Setting.Bool cps = bool("cps", "CPS on mouse buttons", true);
        final Setting.Bool space = bool("space", "Space bar", true);
        final Setting.Color pressed = color("pressed", "Pressed colour", 0xC0FFFFFF);
        final Setting.Color pressedText = color("pressedtext", "Pressed text colour", 0xFF000000);
        final Setting.Num fade = num("fade", "Fade speed", 0, 1, 0.05f, 0.35f, "");
        private final float[] anim = new float[7];

        public Keystrokes() {
            super("keystrokes", "Keystrokes", Items.LEVER, true, 0.01f, 0.25f,
                    "WASD, mouse buttons and space, lighting up as you press them.");
        }

        private int k() { return Math.round(keySize.value); }

        private int gp() { return Math.round(gap.value); }

        @Override
        public int width(Font font) {
            return k() * 3 + gp() * 2;
        }

        @Override
        public int height(Font font) {
            int h = k() * 2 + gp();
            if (mouse.value) h += gp() + k();
            if (space.value) h += gp() + Math.max(8, k() / 2);
            return h;
        }

        @Override
        public void draw(GuiGraphics g, Font font, boolean editing) {
            var o = mc().options;
            int k = k(), gp = gp(), w = width(font);
            key(g, font, 0, k + gp, 0, k, k, label(o.keyUp), o.keyUp.isDown(), null);
            key(g, font, 1, 0, k + gp, k, k, label(o.keyLeft), o.keyLeft.isDown(), null);
            key(g, font, 2, k + gp, k + gp, k, k, label(o.keyDown), o.keyDown.isDown(), null);
            key(g, font, 3, (k + gp) * 2, k + gp, k, k, label(o.keyRight), o.keyRight.isDown(), null);
            int y = (k + gp) * 2;
            if (mouse.value) {
                int mw = (w - gp) / 2;
                key(g, font, 4, 0, y, mw, k, "LMB", o.keyAttack.isDown(), cps.value ? Tracker.leftCps() + " CPS" : null);
                key(g, font, 5, mw + gp, y, w - mw - gp, k, "RMB", o.keyUse.isDown(), cps.value ? Tracker.rightCps() + " CPS" : null);
                y += k + gp;
            }
            if (space.value) {
                int sh = Math.max(8, k / 2);
                key(g, font, 6, 0, y, w, sh, null, o.keyJump.isDown(), null);
            }
        }

        private static String label(KeyMapping km) {
            String s = km.getTranslatedKeyMessage().getString();
            return s.length() > 4 ? s.substring(0, 4) : s;
        }

        private void key(GuiGraphics g, Font font, int i, int x, int y, int w, int h, String label, boolean down, String sub) {
            float target = down ? 1 : 0;
            float speed = fade.value <= 0 ? 1 : Math.min(1, 0.08f + (1 - fade.value) * 0.5f);
            anim[i] += (target - anim[i]) * speed;
            float a = anim[i];
            int bg = background.value ? lerp(bgColor.value, pressed.value, a) : (a > 0.01f ? lerp(0, pressed.value, a) : 0);
            if ((bg >>> 24) != 0) g.fill(x, y, x + w, y + h, bg);
            int tc = lerp(textColor.value, pressedText.value, a);
            if (label == null) {
                // Space bar: a little line instead of a word.
                int lw = w / 3;
                g.fill(x + (w - lw) / 2, y + h / 2, x + (w + lw) / 2, y + h / 2 + 1, tc);
                return;
            }
            if (sub == null) {
                g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, tc, shadow.value && a < 0.5f);
            } else {
                g.drawString(font, label, x + (w - font.width(label)) / 2, y + h / 2 - 7, tc, shadow.value && a < 0.5f);
                g.pose().pushMatrix();
                g.pose().translate(x + w / 2f, y + h / 2f + 2);
                g.pose().scale(0.5f, 0.5f);
                g.drawString(font, sub, -font.width(sub) / 2, 0, tc, false);
                g.pose().popMatrix();
            }
        }

        static int lerp(int a, int b, float t) {
            t = Math.max(0, Math.min(1, t));
            int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
            int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
            return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16)
                    | (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
        }
    }

    // ------------------------------------------------------------------ text ones

    public static final class Fps extends TextHud {
        public Fps() {
            super("fps", "FPS", Items.REPEATER, true, 0.01f, 0.01f, "Frames per second.");
        }

        @Override protected String label() { return "FPS"; }

        @Override protected String value(boolean editing) { return Integer.toString(mc().getFps()); }
    }

    public static final class Cps extends TextHud {
        final Setting.Bool right = bool("right", "Show right click too", true);

        public Cps() {
            super("cps", "CPS", Items.STONE_BUTTON, false, 0.01f, 0.06f, "Clicks per second (left, and right if you want).");
        }

        @Override protected String label() { return "CPS"; }

        @Override protected String value(boolean editing) {
            return right.value ? Tracker.leftCps() + " | " + Tracker.rightCps() : Integer.toString(Tracker.leftCps());
        }
    }

    public static final class Ping extends TextHud {
        public Ping() {
            super("ping", "Ping", Items.ENDER_EYE, false, 0.01f, 0.11f, "Your ping to the server, in ms.");
        }

        @Override protected String label() { return "Ping"; }

        @Override protected String value(boolean editing) {
            Minecraft mc = mc();
            if (mc.player == null || mc.getConnection() == null) return editing ? "34 ms" : "-";
            if (mc.hasSingleplayerServer()) return "0 ms";
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            return (info == null ? 0 : info.getLatency()) + " ms";
        }
    }

    public static final class Coords extends TextHud {
        final Setting.Choice decimals = choice("decimals", "Decimals", 0, "0", "1", "2");

        public Coords() {
            super("coords", "Coordinates", Items.MAP, false, 0.01f, 0.16f, "Where you are: X Y Z.");
        }

        @Override protected String label() { return "XYZ"; }

        @Override protected String value(boolean editing) {
            LocalPlayer p = mc().player;
            if (p == null) return "128 64 -32";
            String f = "%." + decimals.index + "f";
            return String.format(f + " " + f + " " + f, p.getX(), p.getY(), p.getZ());
        }
    }

    public static final class Facing extends TextHud {
        final Setting.Bool degrees = bool("degrees", "Show degrees", true);

        public Facing() {
            super("direction", "Direction", Items.COMPASS, false, 0.01f, 0.21f, "Which way you're facing.");
        }

        @Override protected String label() { return "Facing"; }

        @Override protected String value(boolean editing) {
            LocalPlayer p = mc().player;
            if (p == null) return degrees.value ? "N (180.0)" : "N";
            Direction d = p.getDirection();
            String n = switch (d) {
                case NORTH -> "N";
                case SOUTH -> "S";
                case EAST -> "E";
                case WEST -> "W";
                default -> d.getName();
            };
            float yaw = ((p.getYRot() % 360) + 360) % 360;
            return degrees.value ? n + String.format(" (%.1f)", yaw) : n;
        }
    }

    public static final class Speed extends TextHud {
        public Speed() {
            super("speed", "Speed", Items.FEATHER, false, 0.01f, 0.26f, "How fast you're moving, in blocks per second.");
        }

        @Override protected String label() { return "Speed"; }

        @Override protected String value(boolean editing) {
            return String.format("%.2f b/s", mc().player == null ? 4.32 : Tracker.speed);
        }
    }

    public static final class Clock extends TextHud {
        final Setting.Choice format = choice("format", "Format", 0, "12 hour", "24 hour");

        public Clock() {
            super("clock", "Clock", Items.CLOCK, false, 0.01f, 0.31f, "The real time.");
        }

        @Override protected String label() { return "Time"; }

        @Override protected String value(boolean editing) {
            LocalTime t = LocalTime.now();
            if (format.index == 1) return String.format("%02d:%02d", t.getHour(), t.getMinute());
            int h = t.getHour() % 12;
            return String.format("%d:%02d %s", h == 0 ? 12 : h, t.getMinute(), t.getHour() < 12 ? "AM" : "PM");
        }
    }

    public static final class Reach extends TextHud {
        final Setting.Num hideAfter = num("hide", "Hide after", 0, 10, 0.5f, 3, "s");

        public Reach() {
            super("reach", "Reach", Items.IRON_SWORD, false, 0.45f, 0.62f, "How far away your last hit landed.");
        }

        @Override protected String label() { return "Reach"; }

        @Override protected String value(boolean editing) {
            return Tracker.lastReach < 0 ? "3.00" : String.format("%.2f", Tracker.lastReach);
        }

        @Override
        public boolean visible(Minecraft mc) {
            if (hideAfter.value <= 0) return true;
            return Tracker.lastReach >= 0 && System.currentTimeMillis() - Tracker.lastHitAt < hideAfter.value * 1000;
        }
    }

    public static final class Combo extends TextHud {
        final Setting.Bool hideZero = bool("hidezero", "Hide when 0", true);

        public Combo() {
            super("combo", "Combo", Items.BLAZE_POWDER, false, 0.55f, 0.62f, "Hits in a row without getting hit back.");
        }

        @Override protected String label() { return "Combo"; }

        @Override protected String value(boolean editing) {
            return Integer.toString(editing && Tracker.combo == 0 ? 3 : Tracker.combo);
        }

        @Override
        public boolean visible(Minecraft mc) {
            return !hideZero.value || Tracker.combo > 0;
        }
    }

    public static final class Server extends TextHud {
        public Server() {
            super("server", "Server", Items.OAK_SIGN, false, 0.01f, 0.36f, "The server you're on.");
        }

        @Override protected String label() { return "Server"; }

        @Override protected String value(boolean editing) {
            Minecraft mc = mc();
            if (mc.hasSingleplayerServer()) return "Singleplayer";
            ServerData d = mc.getCurrentServer();
            return d == null ? (editing ? "play.example.net" : "-") : d.ip;
        }
    }

    // ------------------------------------------------------------------ item counter

    public static class ItemCounter extends HudModule {
        static final String[] NAMES = {"Totems", "Splash potions", "Golden apples", "Ender pearls", "End crystals", "Arrows", "Cobwebs", "Obsidian"};
        static final Item[] ITEMS = {Items.TOTEM_OF_UNDYING, Items.SPLASH_POTION, Items.GOLDEN_APPLE, Items.ENDER_PEARL,
                Items.END_CRYSTAL, Items.ARROW, Items.COBWEB, Items.OBSIDIAN};
        final Setting.Choice item;
        final Setting.Bool hideZero = bool("hidezero", "Hide when you have none", false);

        public ItemCounter(String id, String name, int def, float x, float y, boolean on) {
            super(id, name, ITEMS[def], on, x, y, "Shows how many of an item you're carrying.");
            item = choice("item", "Item", def, NAMES);
        }

        int count() {
            LocalPlayer p = mc().player;
            if (p == null) return -1;
            Item it = ITEMS[item.index];
            int n = 0;
            var inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.is(it)) n += s.getCount();
            }
            return n;
        }

        @Override
        public boolean visible(Minecraft mc) {
            return !hideZero.value || count() > 0;
        }

        @Override
        public int width(Font font) {
            return 16 + 4 + font.width("64") + 6;
        }

        @Override
        public int height(Font font) {
            return 20;
        }

        @Override
        public void draw(GuiGraphics g, Font font, boolean editing) {
            int n = count();
            if (n < 0) n = 2;
            box(g, width(font), height(font));
            g.renderItem(new ItemStack(ITEMS[item.index]), 2, 2);
            text(g, font, Integer.toString(n), 22, 6, n == 0 ? 0xFFD9614C : textColor.value);
        }
    }

    // ------------------------------------------------------------------ armour

    public static final class Armor extends HudModule {
        final Setting.Choice direction = choice("direction", "Direction", 0, "Vertical", "Horizontal");
        final Setting.Choice durability = choice("durability", "Durability", 0, "Number", "Percent", "Off", "Both");
        final Setting.Bool hand = bool("hand", "Include held item", true);
        final Setting.Bool colourDura = bool("colourdura", "Colour by durability", true);
        private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        private static final Item[] SAMPLE = {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS};

        public Armor() {
            super("armor", "Armor status", Items.DIAMOND_CHESTPLATE, false, 0.99f, 0.6f, "Your armour and its durability.");
        }

        private List<ItemStack> stacks(boolean editing) {
            List<ItemStack> out = new ArrayList<>();
            LocalPlayer p = mc().player;
            for (int i = 0; i < 4; i++) {
                ItemStack s = p == null ? ItemStack.EMPTY : p.getItemBySlot(SLOTS[i]);
                if (s.isEmpty() && (editing || p == null)) s = new ItemStack(SAMPLE[i]);
                out.add(s);
            }
            if (hand.value) {
                ItemStack s = p == null ? ItemStack.EMPTY : p.getMainHandItem();
                if (s.isEmpty() && (editing || p == null)) s = new ItemStack(Items.DIAMOND_SWORD);
                out.add(s);
            }
            return out;
        }

        private int cellW(Font font) {
            return durability.index == 2 ? 18 : 18 + font.width(durability.index == 3 ? "1561 100%" : "1561") + 2;
        }

        @Override
        public int width(Font font) {
            int n = hand.value ? 5 : 4;
            return direction.index == 0 ? cellW(font) + 2 : n * cellW(font) + 2;
        }

        @Override
        public int height(Font font) {
            int n = hand.value ? 5 : 4;
            return direction.index == 0 ? n * 18 + 2 : 20;
        }

        @Override
        public void draw(GuiGraphics g, Font font, boolean editing) {
            box(g, width(font), height(font));
            List<ItemStack> list = stacks(editing);
            for (int i = 0; i < list.size(); i++) {
                ItemStack s = list.get(i);
                int x = direction.index == 0 ? 1 : 1 + i * cellW(font);
                int y = direction.index == 0 ? 1 + i * 18 : 2;
                if (s.isEmpty()) continue;
                g.renderItem(s, x + 1, y + 1);
                if (durability.index != 2 && s.isDamageableItem()) {
                    int left = s.getMaxDamage() - s.getDamageValue();
                    float frac = left / (float) s.getMaxDamage();
                    String t = durability.index == 1 ? Math.round(frac * 100) + "%" : durability.index == 3 ? left + " " + Math.round(frac * 100) + "%" : Integer.toString(left);
                    int c = !colourDura.value ? textColor.value : frac > 0.5f ? 0xFF8DBF6A : frac > 0.2f ? 0xFFFFD84A : 0xFFD9614C;
                    text(g, font, t, x + 19, y + 5, c);
                } else if (s.getCount() > 1 && durability.index != 2) {
                    text(g, font, "x" + s.getCount(), x + 19, y + 5, textColor.value);
                }
            }
        }
    }

    // ------------------------------------------------------------------ potions

    public static final class Potions extends HudModule {
        final Setting.Bool icons = bool("icons", "Icons", true);
        final Setting.Bool effectColours = bool("effectcolours", "Use effect colours", true);
        final Setting.Bool blink = bool("blink", "Blink when ending", true);

        public Potions() {
            super("potions", "Potion effects", Items.BREWING_STAND, false, 0.99f, 0.3f, "Your active effects and how long they have left.");
        }

        private record Line(MobEffectInstance effect, String text, int color) {}

        private List<Line> lines(boolean editing) {
            List<Line> out = new ArrayList<>();
            LocalPlayer p = mc().player;
            if (p != null) {
                for (MobEffectInstance e : p.getActiveEffects()) {
                    String name = e.getEffect().value().getDisplayName().getString();
                    if (e.getAmplifier() > 0) name += " " + roman(e.getAmplifier() + 1);
                    String time;
                    if (e.isInfiniteDuration()) time = "**:**";
                    else {
                        int secs = e.getDuration() / 20;
                        time = String.format("%d:%02d", secs / 60, secs % 60);
                    }
                    int c = effectColours.value ? 0xFF000000 | e.getEffect().value().getColor() : textColor.value;
                    out.add(new Line(e, name + "  " + time, c));
                }
            }
            if (out.isEmpty() && editing) {
                out.add(new Line(null, "Speed II  1:30", effectColours.value ? 0xFF33EBFF : textColor.value));
                out.add(new Line(null, "Strength II  1:30", effectColours.value ? 0xFFFFC700 : textColor.value));
            }
            return out;
        }

        static String roman(int n) {
            return switch (n) {
                case 2 -> "II";
                case 3 -> "III";
                case 4 -> "IV";
                case 5 -> "V";
                default -> Integer.toString(n);
            };
        }

        @Override
        public boolean visible(Minecraft mc) {
            return mc.player != null && !mc.player.getActiveEffects().isEmpty();
        }

        @Override
        public int width(Font font) {
            int w = font.width("Strength II  1:30");
            for (Line l : lines(true)) w = Math.max(w, font.width(l.text()));
            return w + 6 + (icons.value ? 12 : 0);
        }

        @Override
        public int height(Font font) {
            return Math.max(2, lines(true).size()) * 12 + 2;
        }

        @Override
        public void draw(GuiGraphics g, Font font, boolean editing) {
            box(g, width(font), height(font));
            List<Line> ls = lines(editing);
            int y = 2;
            for (Line l : ls) {
                int x = 3;
                if (blink.value && l.effect() != null && l.effect().endsWithin(200) && (System.currentTimeMillis() / 400) % 2 == 0) {
                    y += 12;
                    continue;
                }
                if (icons.value) {
                    if (l.effect() != null) g.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(l.effect().getEffect()), x, y, 10, 10);
                    x += 12;
                }
                text(g, font, l.text(), x, y + 1, l.color());
                y += 12;
            }
        }
    }

    private HudModules() {
    }
}
