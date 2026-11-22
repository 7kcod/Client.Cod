package dev.cod.pvpcod.module;

import dev.cod.pvpcod.config.Store;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** A feature you can switch on and off and tweak. */
public abstract class Module {

    public enum Category {
        HUD("HUD"), VISUAL("Visual"), PVP("PvP"), COSMETIC("Cosmetic");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    public final String id;
    public final String name;
    public final String description;
    public final Category category;
    private final boolean defaultOn;
    private final Item icon;
    public boolean enabled;
    public final List<Setting> settings = new ArrayList<>();

    protected Module(String id, String name, Category category, Item icon, boolean defaultOn, String description) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.icon = icon;
        this.defaultOn = defaultOn;
        this.enabled = defaultOn;
        this.description = description;
    }

    public ItemStack icon() {
        return new ItemStack(icon);
    }

    public void setEnabled(boolean on) {
        if (on == enabled) return;
        enabled = on;
        onToggle(on);
    }

    protected void onToggle(boolean on) {
    }

    /** Runs every client tick while the module is on. */
    public void tick(Minecraft mc) {
    }

    /** Runs every client tick even while off (for modules that need to tidy up). */
    public void alwaysTick(Minecraft mc) {
    }

    /** Some modules open their own screen instead of the usual settings list. */
    public boolean hasCustomScreen() {
        return false;
    }

    public void resetSettings() {
        for (Setting s : settings) s.reset();
    }

    public void load(Store s) {
        enabled = s.bool("mod." + id + ".on", defaultOn);
        for (Setting st : settings) st.read(s, "mod." + id + ".");
    }

    public void save(Store s) {
        s.put("mod." + id + ".on", Boolean.toString(enabled));
        for (Setting st : settings) st.write(s, "mod." + id + ".");
    }

    // ------------------------------------------------------------------ builders

    protected Setting.Bool bool(String key, String label, boolean def) {
        Setting.Bool s = new Setting.Bool(key, label, def);
        settings.add(s);
        return s;
    }

    protected Setting.Num num(String key, String label, float min, float max, float step, float def, String unit) {
        Setting.Num s = new Setting.Num(key, label, min, max, step, def, unit);
        settings.add(s);
        return s;
    }

    protected Setting.Color color(String key, String label, int def) {
        Setting.Color s = new Setting.Color(key, label, def);
        settings.add(s);
        return s;
    }

    protected Setting.Choice choice(String key, String label, int def, String... options) {
        Setting.Choice s = new Setting.Choice(key, label, def, options);
        settings.add(s);
        return s;
    }

    protected Setting.Text text(String key, String def) {
        Setting.Text s = new Setting.Text(key, def);
        settings.add(s);
        return s;
    }
}
