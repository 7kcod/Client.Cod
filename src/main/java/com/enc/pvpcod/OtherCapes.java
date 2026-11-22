package com.enc.pvpcod;

import com.enc.pvpcod.module.Modules;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Capes other players have from LabyMod and OptiFine,
 * fetched from their public cape APIs and shown on players who have no Minecraft cape.
 *
 * Lunar Client isn't here: Lunar keeps its cosmetics on its own private servers and only
 * the Lunar launcher can get them, so no other mod can show them.
 */
public final class OtherCapes {

    private static final Map<UUID, Optional<Identifier>> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> LOADING = new ConcurrentHashMap<>();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "pvpcod-capes");
        t.setDaemon(true);
        return t;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    private static int counter;

    /** The cape texture for this player, or null (fetches in the background the first time). */
    public static Identifier get(UUID uuid, String name) {
        if (!Modules.OTHER_CAPES.enabled || uuid == null) return null;
        Optional<Identifier> c = CACHE.get(uuid);
        if (c != null) return c.orElse(null);
        if (LOADING.putIfAbsent(uuid, true) == null) {
            POOL.submit(() -> {
                NativeImage img = null;
                try {
                    img = fetch(uuid, name);
                } catch (Exception ignored) {
                }
                NativeImage found = img;
                Minecraft.getInstance().execute(() -> {
                    if (found == null) CACHE.put(uuid, Optional.empty());
                    else {
                        Identifier id = Identifier.fromNamespaceAndPath("pvpcod", "othercape/" + (counter++));
                        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "pvpcod other cape", found));
                        CACHE.put(uuid, Optional.of(id));
                    }
                    LOADING.remove(uuid);
                });
            });
        }
        return null;
    }

    /** Forget everything (after changing which services are on). */
    public static void clear() {
        Minecraft mc = Minecraft.getInstance();
        for (Optional<Identifier> o : CACHE.values()) o.ifPresent(id -> mc.getTextureManager().release(id));
        CACHE.clear();
    }

    private static NativeImage fetch(UUID uuid, String name) throws Exception {
        var m = Modules.OTHER_CAPES;
        NativeImage img = null;
        if (m.labymod.value) img = png("https://dl.labymod.net/capes/" + uuid);
        if (img == null && m.optifine.value && name != null && !name.isEmpty()) img = png("http://s.optifine.net/capes/" + name + ".png");
        return img == null ? null : normalise(img);
    }

    private static NativeImage png(String url) throws Exception {
        HttpResponse<byte[]> res = HTTP.send(req(url), HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200 || res.body().length < 8) return null;
        try {
            return NativeImage.read(new ByteArrayInputStream(res.body()));
        } catch (Exception e) {
            return null; // not a png
        }
    }

    private static HttpRequest req(String url) {
        return HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).header("User-Agent", "pvp.cod").GET().build();
    }

    /**
     * Makes any cape image fit the normal 2:1 cape layout: OptiFine's 46x22 (or bigger multiples)
     * goes into a 64x32 canvas, and animated strips keep their first frame.
     */
    private static NativeImage normalise(NativeImage in) {
        int w = in.getWidth(), h = in.getHeight();
        // OptiFine size: 46x22 * n
        if (w % 46 == 0 && h % 22 == 0 && w / 46 == h / 22) {
            int s = w / 46;
            NativeImage out = new NativeImage(64 * s, 32 * s, true);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) out.setPixel(x, y, in.getPixel(x, y));
            in.close();
            return out;
        }
        // taller than 2:1 = animated: take the first frame
        if (h > w / 2) {
            NativeImage out = new NativeImage(w, w / 2, true);
            for (int y = 0; y < w / 2; y++) for (int x = 0; x < w; x++) out.setPixel(x, y, in.getPixel(x, y));
            in.close();
            return out;
        }
        return in;
    }

    private OtherCapes() {
    }
}
