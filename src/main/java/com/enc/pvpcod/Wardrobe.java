package com.enc.pvpcod;

import com.enc.pvpcod.config.Store;
import com.enc.pvpcod.module.Modules;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Skin and cape swapping. Only you see these (Minecraft doesn't let a mod change what
 * other players see without a server plugin).
 *
 * Skins: any 64x64 skin png in config/pvpcod/skins.
 * Capes: the built-in cod capes, or any 64x32 cape png in config/pvpcod/capes.
 */
public final class Wardrobe {

    public static final Path SKINS = Store.DIR.resolve("skins");
    public static final Path CAPES = Store.DIR.resolve("capes");

    /** A texture we registered ourselves. */
    public record Tex(Identifier id) implements ClientAsset.Texture {
        @Override
        public Identifier texturePath() {
            return id;
        }
    }

    /** One cape you can pick. id is "builtin:<name>" or "file:<file name>". */
    public record CapeChoice(String id, String label, int top, int bottom, int pattern, String emblem) {}

    // patterns
    static final int GRADIENT = 0, STRIPES = 1, CHECKER = 2, STARS = 3, FLAMES = 4, DIAGONAL = 5, SPLIT = 6, WAVES = 7, BORDER = 8, SCALES = 9;

    public static final List<CapeChoice> BUILTIN = List.of(
            new CapeChoice("builtin:cod_orange", "Cod (orange)", 0xFFE8A33D, 0xFF7A531A, GRADIENT, "item/cod"),
            new CapeChoice("builtin:deep_ocean", "Deep Ocean", 0xFF4FC3F7, 0xFF0D1620, WAVES, "item/cod"),
            new CapeChoice("builtin:nether", "Nether", 0xFFFF5A36, 0xFF1A0E0E, FLAMES, "item/blaze_powder"),
            new CapeChoice("builtin:the_end", "The End", 0xFFC08CFF, 0xFF120F1C, STARS, "item/ender_pearl"),
            new CapeChoice("builtin:emerald", "Emerald", 0xFF41D97A, 0xFF0E1712, SCALES, "item/emerald"),
            new CapeChoice("builtin:snow", "Snow", 0xFFFFFFFF, 0xFF97A3B0, GRADIENT, "item/snowball"),
            new CapeChoice("builtin:midnight", "Midnight", 0xFF00E5C3, 0xFF000000, STARS, "item/cod"),
            new CapeChoice("builtin:cherry", "Cherry", 0xFFFF8FB8, 0xFF1C1216, DIAGONAL, "item/tropical_fish"),
            new CapeChoice("builtin:gold", "Gold", 0xFFFFD84A, 0xFF8A6A10, BORDER, "item/golden_apple"),
            new CapeChoice("builtin:salmon", "Salmon", 0xFFFF8A65, 0xFF5A1E12, STRIPES, "item/salmon"),
            new CapeChoice("builtin:inferno", "Inferno", 0xFFFFD84A, 0xFF2A0800, FLAMES, ""),
            new CapeChoice("builtin:checkers", "Checkers", 0xFFE9E1D2, 0xFF161412, CHECKER, "item/cod"),
            new CapeChoice("builtin:heart", "Heart", 0xFFFF5A7A, 0xFF3A0A18, SPLIT, "heart"),
            new CapeChoice("builtin:totem", "Totem", 0xFFFFD84A, 0xFF1C6B3A, GRADIENT, "item/totem_of_undying"),
            new CapeChoice("builtin:duelist", "Duelist", 0xFF4FE3E0, 0xFF0B2230, DIAGONAL, "item/diamond_sword"),
            new CapeChoice("builtin:crystal", "Crystal", 0xFFFF8FD8, 0xFF0A0614, STARS, "item/end_crystal"),
            new CapeChoice("builtin:puffer", "Puffer", 0xFFFFE070, 0xFF3A6A20, SCALES, "item/pufferfish"),
            new CapeChoice("builtin:sea_glass", "Sea glass", 0xFF9FF2D8, 0xFF1F5C66, WAVES, ""),
            new CapeChoice("builtin:frame", "Frame", 0xFFF2F2F2, 0xFF5B7DF5, BORDER, "item/nether_star"),
            new CapeChoice("builtin:barber", "Barber", 0xFFFFFFFF, 0xFFD9614C, STRIPES, ""));

    private static final Map<String, Identifier> LOADED = new HashMap<>();
    private static int counter;

    public static void ensureFolders() {
        try {
            Files.createDirectories(SKINS);
            Files.createDirectories(CAPES);
        } catch (Exception ignored) {
        }
    }

    public static List<String> skinFiles() {
        return pngs(SKINS);
    }

    public static List<String> capeFiles() {
        return pngs(CAPES);
    }

    private static List<String> pngs(Path dir) {
        ensureFolders();
        List<String> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.png")) {
            for (Path p : ds) out.add(p.getFileName().toString());
        } catch (Exception ignored) {
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    /** Forget loaded textures so changed files are read again. */
    public static void reload() {
        Minecraft mc = Minecraft.getInstance();
        for (Identifier id : LOADED.values()) mc.getTextureManager().release(id);
        LOADED.clear();
    }

    public static Identifier skinTexture(String file) {
        if (file == null || file.isEmpty()) return null;
        return LOADED.computeIfAbsent("skin:" + file, k -> load(SKINS.resolve(file), false));
    }

    public static Identifier capeTexture(String id) {
        if (id == null || id.isEmpty()) return null;
        return LOADED.computeIfAbsent("cape:" + id, k -> {
            if (id.startsWith("file:")) return load(CAPES.resolve(id.substring(5)), true);
            for (CapeChoice c : BUILTIN) if (c.id().equals(id)) return register(drawCape(c));
            return null;
        });
    }

    private static Identifier load(Path file, boolean cape) {
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage img = NativeImage.read(in);
            // Old 64x32 skins are fine for the game; capes of any size are scaled by the model.
            return register(img);
        } catch (Exception e) {
            return null;
        }
    }

    private static final Map<Identifier, int[]> SIZES = new HashMap<>();

    /** Width and height of a texture we registered (64x32 if unknown). */
    public static int[] size(Identifier id) {
        return SIZES.getOrDefault(id, new int[]{64, 32});
    }

    private static Identifier register(NativeImage img) {
        if (img == null) return null;
        Identifier id = Identifier.fromNamespaceAndPath("pvpcod", "wardrobe/" + (counter++));
        SIZES.put(id, new int[]{img.getWidth(), img.getHeight()});
        DynamicTexture tex = new DynamicTexture(() -> "pvpcod wardrobe", img);
        Minecraft.getInstance().getTextureManager().register(id, tex);
        return id;
    }

    /**
     * Draws a cape (HD, 128x64): a pattern in two colours over the whole sheet (so the elytra
     * matches), and an emblem on the back. Emblems are item textures from whatever pack you
     * have on, pack.cod mixes included.
     */
    private static NativeImage drawCape(CapeChoice c) {
        int s = 2, W = 64 * s, H = 32 * s;
        NativeImage img = new NativeImage(W, H, true);
        java.util.Random rnd = new java.util.Random(c.id().hashCode());
        boolean[] star = new boolean[W * H];
        for (int i = 0; i < W * H / 22; i++) star[rnd.nextInt(W * H)] = true;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                img.setPixel(x, y, pattern(c, x, y, s, star[y * W + x]));
            }
        }
        // a lighter outline along the shoulders and sides of the back
        for (int x = s; x < 11 * s; x++) for (int y = 0; y < s; y++) img.setPixel(x, y, mix(img.getPixel(x, y), 0xFFFFFFFF, 0.25f));
        emblem(img, c.emblem(), s);
        return img;
    }

    /** Colour of one pixel. Back-panel coordinates: x from 1s, y from 1s (top) to 17s (bottom). */
    private static int pattern(CapeChoice c, int x, int y, int s, boolean star) {
        int top = c.top(), bot = c.bottom();
        float ty = Math.min(1f, Math.max(0f, (y - s) / (16f * s)));
        int lx = x / s, ly = y / s; // in 64x32 pixels
        return switch (c.pattern()) {
            case STRIPES -> (ly / 2) % 2 == 0 ? top : bot;
            case CHECKER -> ((lx / 2) + (ly / 2)) % 2 == 0 ? top : bot;
            case STARS -> {
                int base = mix(bot, mix(top, bot, 0.6f), ty * 0.6f);
                yield star ? mix(top, 0xFFFFFFFF, 0.4f) : base;
            }
            case FLAMES -> {
                // tongues of flame rising from the bottom of the cape
                double fl = 0.45 + 0.18 * Math.sin(x * 0.55) + 0.1 * Math.sin(x * 1.7 + 1.3);
                float up = 1f - ty; // 0 at the bottom, 1 at the top
                if (up < fl) {
                    float k = (float) (up / fl);
                    yield mix(0xFFFFF2A0, top, Math.min(1f, k * 1.4f));
                }
                yield mix(bot, 0xFF000000, 0.2f);
            }
            case DIAGONAL -> ((lx + ly) / 3) % 2 == 0 ? top : bot;
            case SPLIT -> (x % (11 * s)) < (6 * s) ? top : bot;
            case WAVES -> {
                double wave = Math.sin(x * 0.35) * 2 * s;
                int band = (int) Math.floor((y + wave) / (3.0 * s));
                yield band % 2 == 0 ? mix(top, bot, ty * 0.5f) : mix(bot, top, 0.15f);
            }
            case BORDER -> {
                boolean edge = lx <= 1 || lx >= 10 && lx <= 12 || lx >= 21 || ly <= 1 || ly >= 16;
                yield edge ? top : mix(bot, 0xFF000000, ty * 0.3f);
            }
            case SCALES -> {
                // overlapping half circles like fish scales
                int cs = 3 * s;
                int row = y / cs, ox = (row % 2) * cs / 2;
                int cx = ((x + ox) / cs) * cs + cs / 2 - ox, cy = row * cs;
                double d = Math.hypot(x - cx, (y - cy) * 1.2);
                yield d < cs * 0.55 ? mix(top, 0xFFFFFFFF, 0.12f) : d < cs * 0.7 ? mix(top, bot, 0.6f) : mix(top, bot, 0.35f);
            }
            default -> mix(top, bot, ty);
        };
    }

    private static void emblem(NativeImage img, String emblem, int s) {
        if (emblem == null || emblem.isEmpty()) return;
        int size = 10 * s, ox = s, oy = 3 * s;
        if (emblem.equals("heart")) {
            String[] heart = {
                    "..##..##..",
                    ".########.",
                    "##########",
                    "##########",
                    "##########",
                    ".########.",
                    "..######..",
                    "...####...",
                    "....##....",
                    ".........."};
            for (int y = 0; y < 10; y++)
                for (int x = 0; x < 10; x++)
                    if (heart[y].charAt(x) == '#')
                        for (int a = 0; a < s; a++) for (int b = 0; b < s; b++) {
                            int col = (y < 3 && x > 1 && x < 4) ? 0xFFFFB0C0 : 0xFFE0103A;
                            img.setPixel(ox + x * s + a, oy + y * s + b, col);
                        }
            return;
        }
        NativeImage tex = readGameTexture("textures/" + emblem + ".png");
        if (tex == null) return;
        int fw = tex.getWidth(), fh = Math.min(tex.getHeight(), fw); // animated textures: first frame
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int p = tex.getPixel(x * fw / size, y * fh / size);
                if ((p >>> 24) > 0x40) img.setPixel(ox + x, oy + y, p | 0xFF000000);
            }
        }
        tex.close();
    }

    private static NativeImage readGameTexture(String path) {
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(Identifier.withDefaultNamespace(path));
            if (res.isEmpty()) return null;
            try (InputStream in = res.get().open()) {
                return NativeImage.read(in);
            }
        } catch (Exception e) {
            return null;
        }
    }

    static int mix(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16)
                | (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
    }

    /** Swaps in your chosen skin/cape. Called from the getSkin mixin for your own player only. */
    public static PlayerSkin apply(PlayerSkin original) {
        var skin = Modules.SKIN;
        var cape = Modules.CAPE;
        ClientAsset.Texture body = original.body();
        PlayerModelType model = original.model();
        ClientAsset.Texture capeTex = original.cape();
        ClientAsset.Texture elytra = original.elytra();
        boolean changed = false;
        if (skin.enabled) {
            Identifier id = skinTexture(skin.file.value);
            if (id != null) {
                body = new Tex(id);
                model = skin.slim.value ? PlayerModelType.SLIM : PlayerModelType.WIDE;
                changed = true;
            }
        }
        if (cape.enabled) {
            Identifier id = capeTexture(cape.cape.value);
            if (id != null) {
                capeTex = new Tex(id);
                if (cape.elytra.value) elytra = capeTex;
                changed = true;
            }
        }
        return changed ? new PlayerSkin(body, capeTex, elytra, model, original.secure()) : original;
    }

    // ------------------------------------------------------------------ skins by name

    /** What came back from fetching a skin. file = null when it failed (message says why). */
    public record Fetched(String file, boolean slim, String message) {}

    /**
     * Downloads a player's current skin by their name from Mojang and saves it in the skins
     * folder as "<name>.png". Runs off the main thread; done() is called on the main thread.
     */
    public static void fetchSkin(String name, java.util.function.Consumer<Fetched> done) {
        Thread t = new Thread(() -> {
            Fetched r;
            try {
                r = fetch(name.trim());
            } catch (Exception e) {
                r = new Fetched(null, false, "Couldn't reach Mojang (" + e.getClass().getSimpleName() + ")");
            }
            Fetched out = r;
            Minecraft.getInstance().execute(() -> done.accept(out));
        }, "pvpcod-skin-fetch");
        t.setDaemon(true);
        t.start();
    }

    private static Fetched fetch(String name) throws Exception {
        if (!name.matches("[A-Za-z0-9_]{1,16}")) return new Fetched(null, false, "That isn't a Minecraft name");
        java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(8)).followRedirects(java.net.http.HttpClient.Redirect.NORMAL).build();
        var profile = http.send(req("https://api.mojang.com/users/profiles/minecraft/" + name),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        if (profile.statusCode() == 404 || profile.statusCode() == 204) return new Fetched(null, false, "No player called " + name);
        if (profile.statusCode() != 200) return new Fetched(null, false, "Mojang said " + profile.statusCode() + ", try again in a minute");
        String id = find(profile.body(), "\"id\"\\s*:\\s*\"([0-9a-fA-F]{32})\"");
        String realName = find(profile.body(), "\"name\"\\s*:\\s*\"([A-Za-z0-9_]+)\"");
        if (id == null) return new Fetched(null, false, "No player called " + name);
        if (realName == null) realName = name;

        var session = http.send(req("https://sessionserver.mojang.com/session/minecraft/profile/" + id),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        if (session.statusCode() != 200) return new Fetched(null, false, "Mojang said " + session.statusCode() + ", try again in a minute");
        String value = find(session.body(), "\"value\"\\s*:\\s*\"([A-Za-z0-9+/=]+)\"");
        if (value == null) return new Fetched(null, false, realName + " has no skin set");
        String textures = new String(java.util.Base64.getDecoder().decode(value), java.nio.charset.StandardCharsets.UTF_8);
        String url = find(textures, "\"SKIN\"\\s*:\\s*\\{\\s*\"url\"\\s*:\\s*\"([^\"]+)\"");
        if (url == null) return new Fetched(null, false, realName + " uses a default skin");
        boolean slim = textures.matches("(?s).*\"SKIN\"\\s*:\\s*\\{[^}]*\"model\"\\s*:\\s*\"slim\".*");
        if (url.startsWith("http://")) url = "https://" + url.substring(7);

        var png = http.send(req(url), java.net.http.HttpResponse.BodyHandlers.ofByteArray());
        if (png.statusCode() != 200) return new Fetched(null, false, "Couldn't download the skin (" + png.statusCode() + ")");
        ensureFolders();
        String file = realName + ".png";
        Files.write(SKINS.resolve(file), png.body());
        return new Fetched(file, slim, "Wearing " + realName + "'s skin");
    }

    private static java.net.http.HttpRequest req(String url) {
        return java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                .timeout(java.time.Duration.ofSeconds(10)).header("User-Agent", "pvp.cod").GET().build();
    }

    private static String find(String text, String regex) {
        var m = java.util.regex.Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1) : null;
    }

    /** Forget one loaded skin so a re-downloaded file shows up. */
    public static void forgetSkin(String file) {
        Identifier id = LOADED.remove("skin:" + file);
        if (id != null) Minecraft.getInstance().getTextureManager().release(id);
    }

    public static String capeLabel(String id) {
        for (CapeChoice c : BUILTIN) if (c.id().equals(id)) return c.label();
        if (id != null && id.startsWith("file:")) return id.substring(5).replaceAll("(?i)\\.png$", "");
        return "None";
    }

    public static String pretty(String file) {
        return file.replaceAll("(?i)\\.png$", "").replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private Wardrobe() {
    }
}
