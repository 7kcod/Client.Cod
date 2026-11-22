package com.enc.pvpcod;

import com.enc.pvpcod.config.Store;
import com.enc.pvpcod.module.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Friends list, name icons (with admin overrides) and the admin key. */
public final class Social {

    // ------------------------------------------------------------------ icons

    /** An icon drawn next to a name: any sprite from one of the game's atlases, so packs re-texture it too. */
    public record Icon(String id, String label, Identifier atlas, Identifier sprite) {
        MutableComponent component() {
            return Component.object(new AtlasSprite(atlas, sprite));
        }
    }

    private static final Identifier ITEMS = Identifier.withDefaultNamespace("items");
    private static final Identifier GUI = Identifier.withDefaultNamespace("gui");

    public static final List<Icon> ICONS = List.of(
            item("cod", "Cod"), item("cooked_cod", "Cooked cod"), item("salmon", "Salmon"), item("tropical_fish", "Tropical fish"),
            item("pufferfish", "Pufferfish"), item("cod_bucket", "Cod bucket"), item("golden_apple", "Golden apple"),
            item("totem_of_undying", "Totem"), item("nether_star", "Nether star"), item("diamond", "Diamond"),
            item("emerald", "Emerald"), item("netherite_ingot", "Netherite"), item("gold_ingot", "Gold"),
            item("ender_pearl", "Ender pearl"), item("end_crystal", "End crystal"), item("diamond_sword", "Diamond sword"),
            item("netherite_sword", "Netherite sword"), item("mace", "Mace"), item("trident", "Trident"),
            item("heart_of_the_sea", "Heart of the sea"), item("experience_bottle", "XP bottle"), item("blaze_powder", "Blaze powder"),
            item("fire_charge", "Fire charge"), item("tnt_minecart", "TNT cart"), item("music_disc_cat", "Disc"),
            item("cake", "Cake"), item("cookie", "Cookie"), item("slime_ball", "Slime ball"), item("ghast_tear", "Ghast tear"),
            item("echo_shard", "Echo shard"), item("amethyst_shard", "Amethyst"), item("feather", "Feather"),
            new Icon("heart", "Heart", GUI, Identifier.withDefaultNamespace("hud/heart/full")),
            new Icon("golden_heart", "Golden heart", GUI, Identifier.withDefaultNamespace("hud/heart/absorbing_full")),
            new Icon("wither_heart", "Wither heart", GUI, Identifier.withDefaultNamespace("hud/heart/withered_full")),
            new Icon("frozen_heart", "Frozen heart", GUI, Identifier.withDefaultNamespace("hud/heart/frozen_full")));

    private static Icon item(String name, String label) {
        return new Icon(name, label, ITEMS, Identifier.withDefaultNamespace("item/" + name));
    }

    public static Icon icon(String id) {
        if (id == null) return null;
        for (Icon i : ICONS) if (i.id().equals(id)) return i;
        return null;
    }

    public static String defaultIcon() {
        return Store.get().str("icons.default", "cod");
    }

    public static void setDefaultIcon(String id) {
        Store.get().put("icons.default", id);
    }

    /** Admin-given icons, by lower-case player name. "none" means no icon at all. */
    public static Map<String, String> assigned() {
        return Store.get().section("icons.p.");
    }

    public static void assign(String name, String iconId) {
        Store.get().put("icons.p." + name.toLowerCase(), iconId);
    }

    public static void unassign(String name) {
        Store.get().put("icons.p." + name.toLowerCase(), null);
    }

    /** Puts the icon in front of a name and colours friends. Used for nametags and the tab list. */
    public static Component decorate(String name, Component original) {
        return decorate(name, null, original);
    }

    public static Component decorate(String name, java.util.UUID uuid, Component original) {
        return decorate(name, uuid, original, true);
    }

    public static Component decorate(String name, java.util.UUID uuid, Component original, boolean icons) {
        if (name == null || original == null) return original;
        Component withIcon = icons ? decorateIcon(name, original) : original;
        Component tier = Tiers.tag(name, uuid);
        if (tier == null) return withIcon;
        return Modules.TIERS.before.value
                ? Component.empty().append(tier).append(" ").append(withIcon)
                : Component.empty().append(withIcon).append(" ").append(tier);
    }

    private static Component decorateIcon(String name, Component original) {
        Minecraft mc = Minecraft.getInstance();
        String lower = name.toLowerCase();
        boolean self = mc.getUser() != null && mc.getUser().getName().equalsIgnoreCase(name);
        boolean friend = isFriend(name);
        var fr = Modules.FRIENDS;
        var ni = Modules.NAME_ICONS;

        Component base = original;
        if (friend && fr.enabled && fr.colourNames.value) base = original.copy().withColor(fr.nameColour.value & 0xFFFFFF);

        String iconId = null;
        if (ni.enabled) {
            iconId = assigned().get(lower);
            if (iconId == null) {
                boolean show = switch (ni.who.index) {
                    case 0 -> self;
                    case 1 -> self || friend;
                    default -> true;
                };
                if (show) iconId = defaultIcon();
            }
        }
        if (friend && fr.enabled && fr.heart.value && (iconId == null || iconId.equals(defaultIcon())) && !assigned().containsKey(lower)) iconId = "heart";
        Icon icon = icon(iconId);
        if (icon == null) return base;
        return Component.empty().append(icon.component()).append(" ").append(base);
    }

    /** A code you can copy to friends so they see the same icons. */
    public static String exportCode() {
        StringBuilder sb = new StringBuilder("default=").append(defaultIcon());
        for (Map.Entry<String, String> e : assigned().entrySet()) sb.append(';').append(e.getKey()).append('=').append(e.getValue());
        for (Map.Entry<String, String> e : Tiers.overrides().entrySet()) sb.append(";tier.").append(e.getKey()).append('=').append(e.getValue());
        return "pvpcod-icons:" + Base64.getUrlEncoder().withoutPadding().encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Returns how many players were read, or -1 if the code isn't one. */
    public static int importCode(String code) {
        if (code == null) return -1;
        code = code.trim();
        if (!code.startsWith("pvpcod-icons:")) return -1;
        try {
            String body = new String(Base64.getUrlDecoder().decode(code.substring("pvpcod-icons:".length())), StandardCharsets.UTF_8);
            int n = 0;
            Store.get().clearSection("icons.p.");
            Store.get().clearSection("tiers.p.");
            for (String part : body.split(";")) {
                int eq = part.indexOf('=');
                if (eq <= 0) continue;
                String k = part.substring(0, eq), v = part.substring(eq + 1);
                if (k.equals("default")) setDefaultIcon(v);
                else if (k.startsWith("tier.")) Store.get().put("tiers.p." + k.substring(5), v);
                else {
                    assign(k, v);
                    n++;
                }
            }
            Store.get().save();
            return n;
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    // ------------------------------------------------------------------ friends

    private static final Map<String, String> FRIENDS = new LinkedHashMap<>();

    public static void loadFriends() {
        FRIENDS.clear();
        for (String n : Store.get().str("friends.list", "").split(",")) {
            n = n.trim();
            if (!n.isEmpty()) FRIENDS.put(n.toLowerCase(), n);
        }
    }

    private static void saveFriends() {
        Store.get().put("friends.list", String.join(",", FRIENDS.values()));
        Store.get().save();
    }

    public static boolean isFriend(String name) {
        return name != null && FRIENDS.containsKey(name.toLowerCase());
    }

    public static List<String> friends() {
        return new ArrayList<>(FRIENDS.values());
    }

    public static boolean addFriend(String name) {
        name = name.trim();
        if (!name.matches("[A-Za-z0-9_]{1,16}") || isFriend(name)) return false;
        FRIENDS.put(name.toLowerCase(), name);
        saveFriends();
        return true;
    }

    public static void removeFriend(String name) {
        FRIENDS.remove(name.toLowerCase());
        saveFriends();
    }

    public static boolean toggleFriend(String name) {
        if (isFriend(name)) {
            removeFriend(name);
            return false;
        }
        addFriend(name);
        return true;
    }

    // ------------------------------------------------------------------ team (separate from friends)

    private static final Map<String, String> TEAM = new LinkedHashMap<>();

    public static void loadTeam() {
        TEAM.clear();
        for (String n : Store.get().str("team.list", "").split(",")) {
            n = n.trim();
            if (!n.isEmpty()) TEAM.put(n.toLowerCase(), n);
        }
    }

    private static void saveTeam() {
        Store.get().put("team.list", String.join(",", TEAM.values()));
        Store.get().save();
    }

    public static boolean isTeam(String name) {
        return name != null && TEAM.containsKey(name.toLowerCase());
    }

    public static List<String> team() {
        return new ArrayList<>(TEAM.values());
    }

    public static boolean addTeam(String name) {
        name = name.trim();
        if (!name.matches("[A-Za-z0-9_]{1,16}") || isTeam(name)) return false;
        TEAM.put(name.toLowerCase(), name);
        saveTeam();
        return true;
    }

    public static void removeTeam(String name) {
        TEAM.remove(name.toLowerCase());
        saveTeam();
    }

    public static boolean toggleTeam(String name) {
        if (isTeam(name)) {
            removeTeam(name);
            return false;
        }
        addTeam(name);
        return true;
    }

    // ------------------------------------------------------------------ admin key

    public static boolean hasAdminKey() {
        return !Store.get().str("admin.hash", "").isEmpty();
    }

    public static void setAdminKey(String key) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String s = hex(salt);
        Store.get().put("admin.salt", s);
        Store.get().put("admin.hash", hash(s, key));
        Store.get().save();
    }

    public static boolean checkAdminKey(String key) {
        String salt = Store.get().str("admin.salt", "");
        String want = Store.get().str("admin.hash", "");
        return !want.isEmpty() && MessageDigest.isEqual(want.getBytes(StandardCharsets.UTF_8), hash(salt, key).getBytes(StandardCharsets.UTF_8));
    }

    private static String hash(String salt, String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return hex(md.digest((salt + ":" + key).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    /** Players you can see right now (tab list), sorted. */
    public static List<String> onlinePlayers() {
        Minecraft mc = Minecraft.getInstance();
        TreeMap<String, String> out = new TreeMap<>();
        if (mc.getConnection() != null) {
            for (var info : mc.getConnection().getOnlinePlayers()) {
                String n = info.getProfile().name();
                out.put(n.toLowerCase(), n);
            }
        }
        return new ArrayList<>(out.values());
    }

    private Social() {
    }
}
