package com.enc.pvpcod.module;

import com.enc.pvpcod.config.Store;

import java.util.ArrayList;
import java.util.List;

/** Every module, in the order they show in the menu. */
public final class Modules {

    // HUD
    public static final HudModules.Keystrokes KEYSTROKES = new HudModules.Keystrokes();
    public static final HudModules.Fps FPS = new HudModules.Fps();
    public static final HudModules.Cps CPS = new HudModules.Cps();
    public static final HudModules.Ping PING = new HudModules.Ping();
    public static final HudModules.Coords COORDS = new HudModules.Coords();
    public static final HudModules.Facing DIRECTION = new HudModules.Facing();
    public static final HudModules.Speed SPEED = new HudModules.Speed();
    public static final HudModules.Clock CLOCK = new HudModules.Clock();
    public static final HudModules.Reach REACH = new HudModules.Reach();
    public static final HudModules.Combo COMBO = new HudModules.Combo();
    public static final HudModules.Server SERVER = new HudModules.Server();
    public static final HudModules.Armor ARMOR = new HudModules.Armor();
    public static final HudModules.Potions POTIONS = new HudModules.Potions();
    public static final HudModules.ItemCounter TOTEMS = new HudModules.ItemCounter("totems", "Totem counter", 0, 0.42f, 0.9f, false);
    public static final HudModules.ItemCounter COUNTER = new HudModules.ItemCounter("counter", "Item counter", 1, 0.58f, 0.9f, false);

    public static final MoreModules.TargetHud TARGET_HUD = new MoreModules.TargetHud();
    public static final MoreModules.Saturation SATURATION = new MoreModules.Saturation();
    public static final MoreModules.Memory MEMORY = new MoreModules.Memory();
    public static final MoreModules.Biome BIOME = new MoreModules.Biome();

    // Visual
    public static final MoreModules.Crosshair CROSSHAIR = new MoreModules.Crosshair();
    public static final MoreModules.CleanHud CLEAN_HUD = new MoreModules.CleanHud();
    public static final MoreModules.AspectRatio ASPECT = new MoreModules.AspectRatio();
    public static final MoreModules.ShulkerTooltips SHULKER = new MoreModules.ShulkerTooltips();
    public static final MoreModules.BlockOutline BLOCK_OUTLINE = new MoreModules.BlockOutline();
    public static final MoreModules.MotionBlur MOTION_BLUR = new MoreModules.MotionBlur();
    public static final MoreModules.NoPumpkin NO_PUMPKIN = new MoreModules.NoPumpkin();
    public static final MoreModules.BoatMap BOAT_MAP = new MoreModules.BoatMap();
    public static final MoreModules.ItemScale ITEM_SCALE = new MoreModules.ItemScale();
    public static final MoreModules.RiptideShield RIPTIDE_SHIELD = new MoreModules.RiptideShield();
    public static final GameModules.LowFire LOW_FIRE = new GameModules.LowFire();
    public static final GameModules.ClearWater CLEAR_WATER = new GameModules.ClearWater();
    public static final GameModules.ClearLava CLEAR_LAVA = new GameModules.ClearLava();
    public static final GameModules.SideShield SIDE_SHIELD = new GameModules.SideShield();
    public static final GameModules.Hitboxes HITBOXES = new GameModules.Hitboxes();
    public static final GameModules.Zoom ZOOM = new GameModules.Zoom();
    public static final GameModules.Fullbright FULLBRIGHT = new GameModules.Fullbright();
    public static final GameModules.NoHurtCam NO_HURT_CAM = new GameModules.NoHurtCam();

    // PvP
    public static final SocialModules.Friends FRIENDS = new SocialModules.Friends();
    public static final GameModules.ToggleSprint TOGGLE_SPRINT = new GameModules.ToggleSprint();
    public static final MoreModules.TotemPops TOTEM_POPS = new MoreModules.TotemPops();
    public static final MoreModules.TeamPings PINGS = new MoreModules.TeamPings();
    public static final MoreModules.TierTagger TIERS = new MoreModules.TierTagger();
    public static final MoreModules.Cosmetics COSMETICS = new MoreModules.Cosmetics();
    public static final MoreModules.OtherCapes OTHER_CAPES = new MoreModules.OtherCapes();

    // Cosmetic
    public static final SocialModules.NameIcons NAME_ICONS = new SocialModules.NameIcons();
    public static final SocialModules.Skin SKIN = new SocialModules.Skin();
    public static final SocialModules.Cape CAPE = new SocialModules.Cape();

    public static final List<Module> ALL = List.of(
            KEYSTROKES, FPS, CPS, PING, COORDS, DIRECTION, SPEED, CLOCK, REACH, COMBO, SERVER, ARMOR, POTIONS, TOTEMS, COUNTER,
            TARGET_HUD, SATURATION, MEMORY, BIOME,
            CROSSHAIR, BLOCK_OUTLINE, MOTION_BLUR, ITEM_SCALE, NO_PUMPKIN, BOAT_MAP, RIPTIDE_SHIELD, CLEAN_HUD, ASPECT, SHULKER, LOW_FIRE, CLEAR_WATER, CLEAR_LAVA, SIDE_SHIELD, HITBOXES, ZOOM, FULLBRIGHT, NO_HURT_CAM,
            FRIENDS, PINGS, TIERS, TOGGLE_SPRINT, TOTEM_POPS,
            NAME_ICONS, COSMETICS, SKIN, CAPE, OTHER_CAPES);

    public static List<HudModule> hud() {
        List<HudModule> out = new ArrayList<>();
        for (Module m : ALL) if (m instanceof HudModule h) out.add(h);
        return out;
    }

    public static void load() {
        Store s = Store.get();
        for (Module m : ALL) m.load(s);
    }

    public static void save() {
        Store s = Store.get();
        for (Module m : ALL) m.save(s);
        s.save();
    }

    private Modules() {
    }
}
