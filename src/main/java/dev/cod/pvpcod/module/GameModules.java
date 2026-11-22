package dev.cod.pvpcod.module;

import dev.cod.pvpcod.PvpCod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;

/** Modules that change how the game looks or plays (no HUD box). Mixins read these. */
public final class GameModules {

    public static final class LowFire extends Module {
        public final Setting.Num lower = num("lower", "Lower by", 0, 1.2f, 0.02f, 0.3f, "");
        public final Setting.Bool hide = bool("hide", "Hide fire completely", false);

        public LowFire() {
            super("lowfire", "Low fire", Category.VISUAL, Items.FLINT_AND_STEEL, true,
                    "Pushes the fire overlay down so you can see while burning.");
        }
    }

    public static final class ClearWater extends Module {
        public final Setting.Num view = num("view", "How far you see", 10, 100, 5, 100, "%");
        public final Setting.Bool overlay = bool("overlay", "Remove the underwater tint", true);

        public ClearWater() {
            super("clearwater", "Clear water", Category.VISUAL, Items.WATER_BUCKET, true,
                    "Removes underwater fog so you can see far.");
        }
    }

    public static final class ClearLava extends Module {
        public final Setting.Num view = num("view", "How far you see", 5, 100, 5, 40, "%");
        public final Setting.Bool fireResOnly = bool("fireres", "Only with Fire Resistance", false);

        public ClearLava() {
            super("clearlava", "Clear lava", Category.VISUAL, Items.LAVA_BUCKET, true,
                    "Lets you see through lava instead of a wall of orange.");
        }

        public boolean active() {
            if (!enabled) return false;
            LocalPlayer p = Minecraft.getInstance().player;
            return !fireResOnly.value || (p != null && p.hasEffect(MobEffects.FIRE_RESISTANCE));
        }
    }

    public static final class SideShield extends Module {
        public final Setting.Num x = num("x", "Sideways", -0.6f, 0.6f, 0.02f, 0.14f, "");
        public final Setting.Num y = num("y", "Down", -0.2f, 0.6f, 0.02f, 0.18f, "");
        public final Setting.Num z = num("z", "Back", -0.4f, 0.4f, 0.02f, 0f, "");
        public final Setting.Num size = num("size", "Size", 0.4f, 1.2f, 0.05f, 0.85f, "x");
        public final Setting.Bool keepWhenBlocking = bool("blocking", "Keep moved while blocking", true);

        public SideShield() {
            super("sideshield", "Side shield", Category.VISUAL, Items.SHIELD, true,
                    "Moves your shield down to the side so it doesn't block your view.");
        }
    }

    public static final class Hitboxes extends Module {
        public final Setting.Color box = color("box", "Box colour", 0xFFFFFFFF);
        public final Setting.Color target = color("target", "Box colour (what you look at)", 0xFFE8A33D);
        public final Setting.Color friend = color("friend", "Box colour (friends)", 0xFF41D97A);
        public final Setting.Color eye = color("eye", "Eye line colour", 0xFFFF0000);
        public final Setting.Bool look = bool("look", "Look direction arrow", true);

        public Hitboxes() {
            super("hitboxes", "Hitboxes", Category.VISUAL, Items.ARMOR_STAND, false,
                    "Shows entity hitboxes (same as F3+B), in your colours.");
        }

        @Override
        protected void onToggle(boolean on) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.debugEntries != null) {
                mc.debugEntries.setStatus(DebugScreenEntries.ENTITY_HITBOXES, on ? DebugScreenEntryStatus.ALWAYS_ON : DebugScreenEntryStatus.NEVER);
            }
        }

        @Override
        public void alwaysTick(Minecraft mc) {
            // Keep the module in step with F3+B.
            if (mc.debugEntries != null) enabled = mc.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES);
        }
    }

    public static final class Fullbright extends Module {
        public Fullbright() {
            super("fullbright", "Fullbright", Category.VISUAL, Items.GLOWSTONE, false,
                    "Everything at full brightness, even caves at night.");
        }
    }

    public static final class NoHurtCam extends Module {
        public NoHurtCam() {
            super("nohurtcam", "No hurt cam", Category.VISUAL, Items.RED_DYE, false,
                    "Stops the screen shaking when you take damage.");
        }
    }

    public static final class Zoom extends Module {
        public final Setting.Num factor = num("factor", "Zoom", 1.5f, 12, 0.5f, 4, "x");
        public final Setting.Bool smooth = bool("smooth", "Smooth zoom", true);
        public final Setting.Bool cinematic = bool("cinematic", "Smooth camera while zoomed", true);
        private float current = 1;
        private boolean wasZooming, hadCinematic;

        public Zoom() {
            super("zoom", "Zoom", Category.VISUAL, Items.SPYGLASS, true,
                    "Hold the zoom key (C by default) to zoom in like OptiFine.");
        }

        public boolean held() {
            return enabled && PvpCod.zoomKey != null && PvpCod.zoomKey.isDown() && Minecraft.getInstance().screen == null;
        }

        /** Called every frame from the FOV mixin. */
        public float divisor() {
            float target = held() ? factor.value : 1;
            if (!smooth.value) current = target;
            else current += (target - current) * 0.35f;
            if (Math.abs(current - target) < 0.01f) current = target;
            return current;
        }

        @Override
        public void alwaysTick(Minecraft mc) {
            boolean z = held();
            if (z != wasZooming && cinematic.value) {
                if (z) {
                    hadCinematic = mc.options.smoothCamera;
                    mc.options.smoothCamera = true;
                } else {
                    mc.options.smoothCamera = hadCinematic;
                }
            }
            wasZooming = z;
        }
    }

    public static final class ToggleSprint extends Module {
        public final Setting.Choice mode = choice("mode", "Mode", 0, "Always sprint", "Toggle with sprint key");
        private boolean toggled = true, wasDown, forced;

        public ToggleSprint() {
            super("togglesprint", "Toggle sprint", Category.PVP, Items.SUGAR, false,
                    "Sprint without holding the key.");
        }

        public boolean toggledOn() {
            return mode.index == 0 || toggled;
        }

        @Override
        public void tick(Minecraft mc) {
            LocalPlayer p = mc.player;
            if (p == null) return;
            if (mode.index == 1) {
                // In toggle mode the real sprint key flips it on and off.
                boolean down = isPhysicallyDown(mc);
                if (down && !wasDown) toggled = !toggled;
                wasDown = down;
            }
            boolean want = toggledOn() && mc.options.keyUp.isDown() && !p.isShiftKeyDown() && !p.isUsingItem();
            if (want) {
                mc.options.keySprint.setDown(true);
                forced = true;
            } else if (forced) {
                mc.options.keySprint.setDown(isPhysicallyDown(mc));
                forced = false;
            }
        }

        private static boolean isPhysicallyDown(Minecraft mc) {
            var key = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(mc.options.keySprint);
            return key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM
                    && com.mojang.blaze3d.platform.InputConstants.isKeyDown(mc.getWindow(), key.getValue());
        }

        @Override
        protected void onToggle(boolean on) {
            if (!on && forced) {
                Minecraft.getInstance().options.keySprint.setDown(false);
                forced = false;
            }
        }
    }

    private GameModules() {
    }
}
