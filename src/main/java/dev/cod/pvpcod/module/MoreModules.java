package dev.cod.pvpcod.module;

import dev.cod.pvpcod.Tracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/** Crosshair, Target HUD, totem pops, clean HUD and a few small info boxes. */
public final class MoreModules {

    static Minecraft mc() {
        return Minecraft.getInstance();
    }

    // ------------------------------------------------------------------ crosshair

    public static final class Crosshair extends Module {
        public final Setting.Choice style = choice("style", "Style", 0, "Cross", "Cross + dot", "Dot", "Circle", "T shape", "Plus gap");
        public final Setting.Num size = num("size", "Size", 1, 12, 1, 5, "px");
        public final Setting.Num gap = num("gap", "Gap", 0, 8, 1, 2, "px");
        public final Setting.Num thickness = num("thickness", "Thickness", 1, 4, 1, 1, "px");
        public final Setting.Color color = color("color", "Colour", 0xFFFFFFFF);
        public final Setting.Bool outline = bool("outline", "Black outline", true);
        public final Setting.Color targetColor = color("targetcolor", "Colour when on a player", 0xFFFF5A36);
        public final Setting.Bool onTarget = bool("ontarget", "Change colour on players", true);

        public Crosshair() {
            super("crosshair", "Custom crosshair", Category.VISUAL, Items.TARGET, false,
                    "Your own crosshair: style, size, gap, colour, and a colour for when you aim at a player.");
        }

        /** Draws centred at (cx, cy). */
        public void draw(GuiGraphics g, int cx, int cy, boolean aimingAtPlayer) {
            int c = onTarget.value && aimingAtPlayer ? targetColor.value : color.value;
            int s = Math.round(size.value), gp = Math.round(gap.value), t = Math.round(thickness.value);
            if (outline.value) shape(g, cx, cy, s, gp, t, 0xFF000000, true);
            shape(g, cx, cy, s, gp, t, c, false);
        }

        private void shape(GuiGraphics g, int cx, int cy, int s, int gp, int t, int c, boolean ol) {
            int o = ol ? 1 : 0;
            int h0 = -(t / 2), h1 = h0 + t; // thickness band around the centre line
            switch (style.index) {
                case 2 -> box(g, cx + h0 - o, cy + h0 - o, cx + h1 + o, cy + h1 + o, c);
                case 3 -> {
                    int r = s + gp;
                    for (int a = 0; a < 360; a += 6) {
                        double rad = Math.toRadians(a);
                        int x = cx + (int) Math.round(Math.cos(rad) * r), y = cy + (int) Math.round(Math.sin(rad) * r);
                        box(g, x - o, y - o, x + t + o, y + t + o, c);
                    }
                }
                default -> {
                    boolean top = style.index != 4;
                    int g0 = style.index == 5 ? gp + 2 : gp;
                    // left, right, bottom, (top)
                    box(g, cx - g0 - s - o, cy + h0 - o, cx - g0 + o, cy + h1 + o, c);
                    box(g, cx + g0 + 1 - o, cy + h0 - o, cx + g0 + 1 + s + o, cy + h1 + o, c);
                    box(g, cx + h0 - o, cy + g0 + 1 - o, cx + h1 + o, cy + g0 + 1 + s + o, c);
                    if (top) box(g, cx + h0 - o, cy - g0 - s - o, cx + h1 + o, cy - g0 + o, c);
                    if (style.index == 1) box(g, cx + h0 - o, cy + h0 - o, cx + h1 + o, cy + h1 + o, c);
                }
            }
        }

        private static void box(GuiGraphics g, int x0, int y0, int x1, int y1, int c) {
            g.fill(x0, y0, x1, y1, c);
        }
    }

    // ------------------------------------------------------------------ target hud

    public static final class TargetHud extends HudModule {
        final Setting.Num stay = num("stay", "Stay after a hit", 1, 15, 1, 5, "s");
        final Setting.Color barColor = color("barcolor", "Health bar colour", 0xFFD9614C);
        final Setting.Bool showPops = bool("pops", "Show totem pops", true);

        public TargetHud() {
            super("targethud", "Target HUD", Items.SKELETON_SKULL, false, 0.5f, 0.7f,
                    "Who you're fighting: their face, health bar, distance and totem pops.");
        }

        private LivingEntity target() {
            Entity e = Tracker.lastTargetEntity;
            if (!(e instanceof LivingEntity l) || !l.isAlive() || l.isRemoved()) return null;
            if (System.currentTimeMillis() - Tracker.lastHitAt > stay.value * 1000) return null;
            return l;
        }

        @Override
        public boolean visible(Minecraft mc) {
            return target() != null;
        }

        @Override
        public int width(Font font) {
            return 130;
        }

        @Override
        public int height(Font font) {
            return 38;
        }

        @Override
        public void draw(GuiGraphics g, Font font, boolean editing) {
            int w = width(font), h = height(font);
            box(g, w, h);
            LivingEntity t = target();
            String name;
            float hp, max, abs;
            double dist;
            int pops = 0;
            if (t == null) {
                name = "Steve";
                hp = 14;
                max = 20;
                abs = 4;
                dist = 2.9;
            } else {
                name = t.getName().getString();
                hp = t.getHealth();
                max = Math.max(1, t.getMaxHealth());
                abs = t.getAbsorptionAmount();
                LocalPlayer me = mc().player;
                dist = me == null ? 0 : me.distanceTo(t);
                if (t instanceof Player p) pops = TotemPops.count(p.getGameProfile().name());
            }
            // face (players) or the mob's spawn-egg-free fallback: a coloured square
            if (t instanceof AbstractClientPlayer p) {
                var tex = p.getSkin().body().texturePath();
                g.blit(RenderPipelines.GUI_TEXTURED, tex, 4, 4, 8f, 8f, 30, 30, 8, 8, 64, 64);
                g.blit(RenderPipelines.GUI_TEXTURED, tex, 4, 4, 40f, 8f, 30, 30, 8, 8, 64, 64);
            } else if (t == null) {
                g.fill(4, 4, 34, 34, 0xFF2F9FA3);
                g.fill(4, 4, 34, 12, 0xFF3B2716);
            } else {
                g.renderItem(new ItemStack(Items.SKELETON_SKULL), 11, 11);
            }
            text(g, font, name, 39, 5, textColor.value);
            String hs = String.format("%.1f", (hp + abs) / 2) + " ❤";
            text(g, font, hs, w - 4 - font.width(hs), 5, barColor.value);
            // bar
            int bx = 39, by = 17, bw = w - 43, bh = 5;
            g.fill(bx, by, bx + bw, by + bh, 0x80000000);
            g.fill(bx, by, bx + Math.round(bw * Math.min(1, hp / max)), by + bh, barColor.value);
            if (abs > 0) g.fill(bx, by, bx + Math.round(bw * Math.min(1, abs / max)), by + 2, 0xFFFFD84A);
            String d = String.format("%.1f m", dist);
            text(g, font, d, 39, 26, 0xFFAAAAAA);
            if (showPops.value && (pops > 0 || t == null)) {
                String ps = (t == null ? 2 : pops) + " pops";
                text(g, font, ps, w - 4 - font.width(ps), 26, 0xFFFFD84A);
            }
        }
    }

    // ------------------------------------------------------------------ totem pops

    public static final class TotemPops extends Module {
        public final Setting.Bool chat = bool("chat", "Say it in chat (only you see it)", true);
        public final Setting.Bool me = bool("me", "Count my own pops too", true);
        private static final Map<String, Integer> POPS = new HashMap<>();

        public TotemPops() {
            super("totempops", "Totem pop counter", Category.PVP, Items.ECHO_SHARD, true,
                    "Counts how many totems each player pops, resets when they die.");
        }

        public static int count(String name) {
            return POPS.getOrDefault(name.toLowerCase(), 0);
        }

        /** Called when a totem goes off on an entity. */
        public void onPop(Entity e) {
            if (!enabled || !(e instanceof Player p)) return;
            Minecraft mc = mc();
            boolean self = mc.player != null && p == mc.player;
            if (self && !me.value) return;
            String name = p.getGameProfile().name();
            int n = POPS.merge(name.toLowerCase(), 1, Integer::sum);
            if (chat.value && mc.player != null) {
                String who = self ? "You" : name;
                mc.player.displayClientMessage(Component.literal("[pvp.cod] ").withColor(0xE8A33D)
                        .append(Component.literal(who + " popped " + n + (n == 1 ? " totem" : " totems")).withColor(0xFFFFFF)), false);
            }
        }

        @Override
        public void alwaysTick(Minecraft mc) {
            if (mc.level == null) {
                POPS.clear();
                return;
            }
            // Dead players start again from zero.
            for (Player p : mc.level.players()) {
                if (p.isDeadOrDying()) POPS.remove(p.getGameProfile().name().toLowerCase());
            }
        }
    }

    // ------------------------------------------------------------------ clean hud

    public static final class CleanHud extends Module {
        public final Setting.Bool bossBar = bool("bossbar", "Hide boss bars", true);
        public final Setting.Bool scoreboard = bool("scoreboard", "Hide the scoreboard", false);
        public final Setting.Bool effects = bool("effects", "Hide effect icons (top right)", true);

        public CleanHud() {
            super("cleanhud", "Clean HUD", Category.VISUAL, Items.BRUSH, false,
                    "Hide boss bars, the scoreboard or the effect icons.");
        }
    }

    // ------------------------------------------------------------------ small info boxes

    public static final class Saturation extends TextHud {
        public Saturation() {
            super("saturation", "Saturation", Items.COOKED_BEEF, false, 0.01f, 0.41f, "Your hidden hunger saturation (eat when it's low).");
        }

        @Override protected String label() { return "Saturation"; }

        @Override protected String value(boolean editing) {
            LocalPlayer p = mc().player;
            return p == null ? "5.0" : String.format("%.1f", p.getFoodData().getSaturationLevel());
        }
    }

    public static final class Memory extends TextHud {
        public Memory() {
            super("memory", "Memory", Items.REDSTONE, false, 0.01f, 0.46f, "How much memory the game is using.");
        }

        @Override protected String label() { return "Mem"; }

        @Override protected String value(boolean editing) {
            Runtime r = Runtime.getRuntime();
            long used = (r.totalMemory() - r.freeMemory()) >> 20, max = r.maxMemory() >> 20;
            return used + "/" + max + " MB";
        }
    }

    public static final class Biome extends TextHud {
        public Biome() {
            super("biome", "Biome", Items.OAK_SAPLING, false, 0.01f, 0.51f, "The biome you're standing in.");
        }

        @Override protected String label() { return "Biome"; }

        @Override protected String value(boolean editing) {
            LocalPlayer p = mc().player;
            if (p == null || mc().level == null) return "Plains";
            return mc().level.getBiome(p.blockPosition()).unwrapKey()
                    .map(k -> pretty(k.identifier().getPath())).orElse("?");
        }

        private static String pretty(String s) {
            StringBuilder sb = new StringBuilder();
            for (String w : s.split("_")) if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
            return sb.toString().trim();
        }
    }

    // ------------------------------------------------------------------ team pings

    public static final class TeamPings extends Module {
        public final Setting.Num duration = num("duration", "How long pings stay", 3, 30, 1, 10, "s");
        public final Setting.Num cooldown = num("cooldown", "Time between your pings", 0.5f, 5, 0.5f, 1.5f, "s");
        public final Setting.Num range = num("range", "How far you can ping", 32, 512, 16, 256, " blocks");
        public final Setting.Bool sendChat = bool("chat", "Send the coords to your team (/msg)", true);
        public final Setting.Bool heads = bool("heads", "Show the pinger's head", true);
        public final Setting.Num headOpacity = num("headopacity", "Head see-through", 10, 100, 5, 60, "%");
        public final Setting.Bool coords = bool("coords", "Show coords under the marker", true);
        public final Setting.Bool sound = bool("sound", "Ping sound", true);
        public final Setting.Bool noPick = bool("nopick", "Middle click doesn't pick blocks in survival", true);
        public final Setting.Color color = color("color", "Ping colour", 0xFF4FC3F7);
        public final Setting.Color enemyColor = color("enemycolor", "Enemy ping colour", 0xFFFF5A36);

        public TeamPings() {
            super("pings", "Team pings", Category.PVP, Items.BELL, true,
                    "Middle-click in survival to ping where you look. Your team sees a marker with your head and gets the coords.");
        }

        /** Pinging is on and you're not in creative (where middle click picks blocks). */
        public boolean active() {
            Minecraft mc = mc();
            return enabled && mc.gameMode != null && mc.gameMode.getPlayerMode() != net.minecraft.world.level.GameType.CREATIVE;
        }
    }

    // ------------------------------------------------------------------ tier tagger

    public static final class TierTagger extends Module {
        public final Setting.Choice mode = choice("mode", "Game mode to show", 0, dev.cod.pvpcod.Tiers.MODE_NAMES);
        public final Setting.Bool fallback = bool("fallback", "Show their best tier if unranked in that mode", true);
        public final Setting.Bool showMode = bool("showmode", "Show the game mode next to the tier", false);
        public final Setting.Bool before = bool("before", "Put the tier before the name", false);

        public TierTagger() {
            super("tiers", "Tier tagger", Category.PVP, Items.NETHERITE_SWORD, true,
                    "Shows each player's MCTiers tier (like HT3) next to their name, in the game mode you pick.");
        }
    }

    // ------------------------------------------------------------------ aspect ratio

    public static final class AspectRatio extends Module {
        public final Setting.Choice ratio = choice("ratio", "Ratio", 0, "4:3", "5:4", "16:10", "1:1", "Custom");
        public final Setting.Num custom = num("custom", "Custom (width / height)", 0.8f, 2.4f, 0.01f, 1.33f, "");

        public AspectRatio() {
            super("aspect", "Rescale aspect ratio", Category.VISUAL, Items.ITEM_FRAME, false,
                    "Stretched resolution: draw the world as 4:3 (or any ratio) stretched to fill your screen.");
        }

        public float value() {
            return switch (ratio.index) {
                case 0 -> 4f / 3f;
                case 1 -> 5f / 4f;
                case 2 -> 16f / 10f;
                case 3 -> 1f;
                default -> custom.value;
            };
        }
    }

    // ------------------------------------------------------------------ shulker tooltips

    public static final class ShulkerTooltips extends Module {
        public final Setting.Bool hideText = bool("hidetext", "Hide the text list of items", true);
        public final Setting.Bool tint = bool("tint", "Tint with the shulker's colour", true);

        public ShulkerTooltips() {
            super("shulker", "Shulker box tooltips", Category.VISUAL, Items.SHULKER_BOX, true,
                    "Hover a shulker box to see what's inside, as a grid of items.");
        }
    }

    // ------------------------------------------------------------------ cosmetics

    public static final class Cosmetics extends Module {
        public final Setting.Bool halo = bool("halo", "Halo", false);
        public final Setting.Color haloColor = color("halocolor", "Halo colour", 0xFFFFE27A);
        public final Setting.Bool topHat = bool("tophat", "Top hat", false);
        public final Setting.Color topHatColor = color("tophatcolor", "Top hat band colour", 0xFFE8A33D);
        public final Setting.Bool crown = bool("crown", "Crown", false);
        public final Setting.Color crownColor = color("crowncolor", "Crown colour", 0xFFFFD84A);
        public final Setting.Bool ears = bool("ears", "Cat ears", false);
        public final Setting.Color earsColor = color("earscolor", "Ears colour", 0xFF2B2B2B);
        public final Setting.Bool horns = bool("horns", "Devil horns", false);
        public final Setting.Color hornsColor = color("hornscolor", "Horns colour", 0xFFD9282F);
        public final Setting.Bool shades = bool("shades", "Shades", false);
        public final Setting.Color shadesColor = color("shadescolor", "Shades colour", 0xFF111111);
        public final Setting.Bool bandana = bool("bandana", "Bandana", false);
        public final Setting.Color bandanaColor = color("bandanacolor", "Bandana colour", 0xFFD9614C);
        public final Setting.Bool wings = bool("wings", "Wings", false);
        public final Setting.Color wingsColor = color("wingscolor", "Wings colour", 0xFFF2F2F2);
        public final Setting.Bool codHat = bool("codhat", "Cod on your head", false);

        public Cosmetics() {
            super("cosmetics", "Cosmetics", Category.COSMETIC, Items.GOLDEN_HELMET, true,
                    "Halo, top hat, crown, cat ears, horns, shades, bandana, wings and a cod hat. Turn each on or off.");
        }
    }

    // ------------------------------------------------------------------ block outline

    public static final class BlockOutline extends Module {
        public final Setting.Color color = color("color", "Outline colour", 0xFF000000);
        public final Setting.Num thickness = num("thickness", "Thickness", 1, 10, 0.5f, 2.5f, "px");
        public final Setting.Bool chroma = bool("chroma", "Rainbow (chroma)", false);
        public final Setting.Num chromaSpeed = num("chromaspeed", "Rainbow speed", 0.1f, 3, 0.1f, 1, "x");
        public final Setting.Bool fill = bool("fill", "Fill the block", false);
        public final Setting.Color fillColor = color("fillcolor", "Fill colour", 0x30FFFFFF);

        public BlockOutline() {
            super("blockoutline", "Block outline", Category.VISUAL, Items.GLASS, false,
                    "Lunar-style block outline: your colour, thickness, rainbow, and a see-through fill.");
        }

        /** Outline colour right now (rainbow moves over time). */
        public int outline() {
            if (!chroma.value) return color.value;
            float hue = (System.currentTimeMillis() % 100000L) / 1000f * 0.25f * chromaSpeed.value;
            return (color.value & 0xFF000000) | (dev.cod.pvpcod.ui.ColorScreen.hsbToRgb(hue % 1f, 0.8f, 1f) & 0xFFFFFF);
        }

        public int fillNow() {
            if (!chroma.value) return fillColor.value;
            return (fillColor.value & 0xFF000000) | (outline() & 0xFFFFFF);
        }
    }

    // ------------------------------------------------------------------ other clients' capes

    public static final class OtherCapes extends Module {
        public final Setting.Bool optifine = bool("optifine", "OptiFine capes", true);
        public final Setting.Bool labymod = bool("labymod", "LabyMod capes", true);

        public OtherCapes() {
            super("othercapes", "LabyMod + OptiFine capes", Category.COSMETIC, Items.ELYTRA, true,
                    "See the capes other players have from LabyMod and OptiFine.");
        }
    }

    // ------------------------------------------------------------------ motion blur

    public static final class MotionBlur extends Module {
        public final Setting.Num strength = num("strength", "Strength", 1, 5, 1, 3, "");

        public MotionBlur() {
            super("motionblur", "Motion blur", Category.VISUAL, Items.PHANTOM_MEMBRANE, false,
                    "Smears the picture a little when you move or turn, like Lunar's motion blur.");
        }

        public net.minecraft.resources.Identifier effect() {
            return net.minecraft.resources.Identifier.fromNamespaceAndPath("pvpcod", "motion_blur_" + Math.round(strength.value));
        }

        @Override
        public void alwaysTick(Minecraft mc) {
            if (mc.gameRenderer == null || mc.level == null) return;
            var gr = mc.gameRenderer;
            var cur = gr.currentPostEffect();
            boolean ours = cur != null && cur.getNamespace().equals("pvpcod");
            if (enabled) {
                if (cur == null || (ours && !cur.equals(effect()))) ((dev.cod.pvpcod.mixin.GameRendererAccess) gr).pvpcod$setPostEffect(effect());
            } else if (ours) {
                gr.clearPostEffect();
            }
        }
    }

    // ------------------------------------------------------------------ small visual fixes

    public static final class NoPumpkin extends Module {
        public final Setting.Bool powderSnow = bool("powdersnow", "Also hide the powder snow frost", false);

        public NoPumpkin() {
            super("nopumpkin", "Pumpkin blur", Category.VISUAL, Items.CARVED_PUMPKIN, true,
                    "Removes the dark blur when you wear a carved pumpkin, so you can see.");
        }
    }

    public static final class BoatMap extends Module {
        public BoatMap() {
            super("boatmap", "Boat map visibility", Category.VISUAL, Items.FILLED_MAP, true,
                    "Keeps your map up on screen while you row a boat (the game normally lowers it).");
        }
    }

    public static final class ItemScale extends Module {
        public final Setting.Num size = num("size", "Size", 0.3f, 1.5f, 0.05f, 0.8f, "x");
        public final Setting.Num x = num("x", "Sideways", -0.5f, 0.5f, 0.02f, 0, "");
        public final Setting.Num y = num("y", "Up / down", -0.5f, 0.5f, 0.02f, 0, "");
        public final Setting.Num z = num("z", "Forward / back", -0.5f, 0.5f, 0.02f, 0, "");
        public final Setting.Choice which = choice("which", "Hands", 0, "Both", "Main hand", "Off hand");

        public ItemScale() {
            super("itemscale", "Item scaling", Category.VISUAL, Items.GOLDEN_SWORD, false,
                    "Make the items in your hands smaller or bigger and move them around.");
        }
    }

    public static final class RiptideShield extends Module {
        public RiptideShield() {
            super("riptideshield", "Riptide shield fix", Category.VISUAL, Items.TRIDENT, true,
                    "Hides your shield while you're spinning with Riptide, so it doesn't cover your screen.");
        }
    }

    private MoreModules() {
    }
}
