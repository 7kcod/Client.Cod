package dev.cod.pvpcod.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Every pvp.cod setting lives in one plain text file: config/pvpcod/pvpcod.properties.
 * (Skins and capes you add go in config/pvpcod/skins and config/pvpcod/capes.)
 */
public final class Store {

    public static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("pvpcod");
    private static final Path FILE = DIR.resolve("pvpcod.properties");
    private static final Store INSTANCE = new Store();

    private final Properties props = new Properties();

    public static Store get() {
        return INSTANCE;
    }

    private Store() {
        try {
            Files.createDirectories(DIR);
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                    props.load(r);
                }
            }
        } catch (IOException ignored) {
        }
    }

    public void save() {
        try {
            Files.createDirectories(DIR);
            // Sorted so the file is easy to read by hand.
            Properties sorted = new Properties() {
                @Override
                public synchronized java.util.Set<java.util.Map.Entry<Object, Object>> entrySet() {
                    return new TreeMap<>(props).entrySet();
                }
            };
            try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                sorted.store(w, "pvp.cod settings");
            }
        } catch (IOException ignored) {
        }
    }

    public String str(String key, String def) {
        return props.getProperty(key, def);
    }

    public void put(String key, String value) {
        if (value == null) props.remove(key);
        else props.setProperty(key, value);
    }

    public boolean bool(String key, boolean def) {
        String v = props.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v);
    }

    public float num(String key, float def) {
        try {
            String v = props.getProperty(key);
            return v == null ? def : Float.parseFloat(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public int color(String key, int def) {
        try {
            String v = props.getProperty(key);
            return v == null ? def : (int) Long.parseLong(v, 16);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public void putColor(String key, int argb) {
        put(key, String.format("%08X", argb));
    }

    /** All keys starting with a prefix, prefix removed. */
    public java.util.Map<String, String> section(String prefix) {
        java.util.Map<String, String> out = new TreeMap<>();
        for (String k : props.stringPropertyNames()) if (k.startsWith(prefix)) out.put(k.substring(prefix.length()), props.getProperty(k));
        return out;
    }

    public void clearSection(String prefix) {
        props.keySet().removeIf(k -> k.toString().startsWith(prefix));
    }
}
