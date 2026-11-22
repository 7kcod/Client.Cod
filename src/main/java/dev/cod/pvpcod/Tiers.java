package dev.cod.pvpcod;

import dev.cod.pvpcod.config.Store;
import dev.cod.pvpcod.module.Modules;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tier tagger: each player's PvP tier from MCTiers (mctiers.com), shown next to their name
 * in the game mode you pick. Admins can set a tier for any player by hand (with its own game mode),
 * which is shown instead.
 */
public final class Tiers {

    /** MCTiers game modes: key in their API, and the name we show. */
    public static final String[] MODES = {"sword", "axe", "pot", "nethop", "smp", "uhc", "vanilla", "mace"};
    public static final String[] MODE_NAMES = {"Sword", "Axe", "Pot", "Neth Pot", "SMP", "UHC", "Vanilla", "Mace"};
    public static final String[] MODE_SHORT = {"SW", "AX", "PT", "NP", "SMP", "UHC", "VN", "MC"};
    /** All tiers, best first. */
    public static final String[] TIERS = {"HT1", "LT1", "HT2", "LT2", "HT3", "LT3", "HT4", "LT4", "HT5", "LT5"};

    /** One player's tiers: per mode "HT3", or null when unranked. retired modes are marked with an R. */
    record Profile(Map<String, String> tiers, long fetchedAt) {}

    private static final Map<UUID, Profile> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> LOADING = new ConcurrentHashMap<>();
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "pvpcod-tiers");
        t.setDaemon(true);
        return t;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    // ------------------------------------------------------------------ admin overrides

    /** Admin-set tiers, lower-case name -> "mode:HT3". */
    public static Map<String, String> overrides() {
        return Store.get().section("tiers.p.");
    }

    public static void setOverride(String name, String mode, String tier) {
        Store.get().put("tiers.p." + name.toLowerCase(), mode + ":" + tier);
        Store.get().save();
    }

    public static void clearOverride(String name) {
        Store.get().put("tiers.p." + name.toLowerCase(), null);
        Store.get().save();
    }

    // ------------------------------------------------------------------ showing

    /** The coloured tag for a name, e.g. "HT3", or null when there's nothing to show. */
    public static Component tag(String name, UUID uuid) {
        var tt = Modules.TIERS;
        if (!tt.enabled || name == null) return null;
        String mode = MODES[tt.mode.index];
        String tier = null, tierMode = mode;

        String over = overrides().get(name.toLowerCase());
        if (over != null && over.contains(":")) {
            tierMode = over.substring(0, over.indexOf(':'));
            tier = over.substring(over.indexOf(':') + 1);
        } else if (uuid != null && uuid.version() == 4) { // only real (online-mode) accounts are on MCTiers
            Profile p = profile(uuid);
            if (p != null) {
                tier = p.tiers().get(mode);
                if (tier == null && tt.fallback.value) {
                    // unranked in the mode you picked: show their best tier
                    for (String t : TIERS) {
                        for (String m : MODES) {
                            String v = p.tiers().get(m);
                            if (v != null && v.replace("R", "").equals(t)) {
                                tier = v;
                                tierMode = m;
                                break;
                            }
                        }
                        if (tier != null) break;
                    }
                }
            }
        }
        if (tier == null) return null;
        boolean retired = tier.startsWith("R");
        String clean = tier.replace("R", "");
        MutableComponent c = Component.literal(clean).withColor(color(clean));
        if (retired) c = c.withStyle(s -> s.withItalic(true));
        if (tt.showMode.value || !tierMode.equals(mode)) {
            c = Component.empty().append(c).append(Component.literal(" " + shortName(tierMode)).withColor(0xAAAAAA));
        }
        return Component.literal("[").withColor(0x777777).append(c).append(Component.literal("]").withColor(0x777777));
    }

    public static String shortName(String mode) {
        for (int i = 0; i < MODES.length; i++) if (MODES[i].equals(mode)) return MODE_SHORT[i];
        return mode;
    }

    public static String modeName(String mode) {
        for (int i = 0; i < MODES.length; i++) if (MODES[i].equals(mode)) return MODE_NAMES[i];
        return mode;
    }

    /** MCTiers-style colours: tier 1 gold, 2 silver, 3 bronze, 4-5 greys/blues. High tiers brighter. */
    public static int color(String tier) {
        boolean high = tier.startsWith("H");
        return switch (tier.length() > 2 ? tier.charAt(2) : '5') {
            case '1' -> high ? 0xFFD700 : 0xE8C25A;
            case '2' -> high ? 0xD8E2F0 : 0xA9B6C8;
            case '3' -> high ? 0xF0A35E : 0xC98A57;
            case '4' -> high ? 0x7FD3FF : 0x5FA8D3;
            default -> high ? 0xB7B7B7 : 0x8A8A8A;
        };
    }

    // ------------------------------------------------------------------ fetching

    private static Profile profile(UUID uuid) {
        Profile p = CACHE.get(uuid);
        long maxAge = 30 * 60 * 1000L;
        if ((p == null || System.currentTimeMillis() - p.fetchedAt() > maxAge) && LOADING.putIfAbsent(uuid, true) == null) {
            POOL.submit(() -> {
                try {
                    CACHE.put(uuid, fetch(uuid));
                    Thread.sleep(250); // be gentle with MCTiers
                } catch (Exception e) {
                    CACHE.put(uuid, new Profile(Map.of(), System.currentTimeMillis()));
                } finally {
                    LOADING.remove(uuid);
                }
            });
        }
        return p;
    }

    private static final Pattern KIT = Pattern.compile("\"([a-z_]+)\"\\s*:\\s*\\{([^{}]*)\\}");
    private static final Pattern TIER = Pattern.compile("\"tier\"\\s*:\\s*(\\d)");
    private static final Pattern POS = Pattern.compile("\"pos\"\\s*:\\s*(\\d)");
    private static final Pattern RETIRED = Pattern.compile("\"retired\"\\s*:\\s*true");

    private static Profile fetch(UUID uuid) throws Exception {
        String id = uuid.toString().replace("-", "");
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://mctiers.com/api/profile/" + id))
                .timeout(Duration.ofSeconds(10)).header("User-Agent", "pvp.cod").GET().build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        Map<String, String> tiers = new ConcurrentHashMap<>();
        if (res.statusCode() == 200) {
            String body = res.body();
            int r = body.indexOf("\"rankings\"");
            if (r >= 0) {
                Matcher m = KIT.matcher(body.substring(r));
                while (m.find()) {
                    String kit = m.group(1), inner = m.group(2);
                    Matcher t = TIER.matcher(inner), p = POS.matcher(inner);
                    if (!t.find()) continue;
                    String tier = (p.find() && p.group(1).equals("1") ? "LT" : "HT") + t.group(1);
                    if (RETIRED.matcher(inner).find()) tier = "R" + tier;
                    tiers.put(kit, tier);
                }
            }
        }
        return new Profile(tiers, System.currentTimeMillis());
    }

    /** Drops cached tiers so they're fetched again. */
    public static void refresh() {
        CACHE.clear();
    }

    private Tiers() {
    }
}
