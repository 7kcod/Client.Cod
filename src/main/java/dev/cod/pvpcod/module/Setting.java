package dev.cod.pvpcod.module;

import dev.cod.pvpcod.config.Store;

/** One editable option on a module. The settings screen draws a control for each kind. */
public abstract class Setting {

    public final String key;
    public final String label;

    Setting(String key, String label) {
        this.key = key;
        this.label = label;
    }

    abstract void read(Store s, String prefix);

    abstract void write(Store s, String prefix);

    public abstract void reset();

    // ------------------------------------------------------------------ kinds

    public static final class Bool extends Setting {
        public final boolean def;
        public boolean value;

        public Bool(String key, String label, boolean def) {
            super(key, label);
            this.def = def;
            this.value = def;
        }

        @Override
        void read(Store s, String p) { value = s.bool(p + key, def); }

        @Override
        void write(Store s, String p) { s.put(p + key, Boolean.toString(value)); }

        @Override
        public void reset() { value = def; }
    }

    public static final class Num extends Setting {
        public final float min, max, step, def;
        public final String unit;
        public float value;

        public Num(String key, String label, float min, float max, float step, float def, String unit) {
            super(key, label);
            this.min = min;
            this.max = max;
            this.step = step;
            this.def = def;
            this.unit = unit;
            this.value = def;
        }

        public void set(float v) {
            v = Math.max(min, Math.min(max, v));
            value = Math.round(v / step) * step;
        }

        public String shown() {
            float v = value;
            String s = step >= 1 ? Integer.toString(Math.round(v)) : step >= 0.1f ? String.format("%.1f", v) : String.format("%.2f", v);
            return s + unit;
        }

        @Override
        void read(Store s, String p) { set(s.num(p + key, def)); }

        @Override
        void write(Store s, String p) { s.put(p + key, Float.toString(value)); }

        @Override
        public void reset() { value = def; }
    }

    public static final class Color extends Setting {
        public final int def;
        public int value;

        public Color(String key, String label, int def) {
            super(key, label);
            this.def = def;
            this.value = def;
        }

        @Override
        void read(Store s, String p) { value = s.color(p + key, def); }

        @Override
        void write(Store s, String p) { s.putColor(p + key, value); }

        @Override
        public void reset() { value = def; }
    }

    public static final class Choice extends Setting {
        public final String[] options;
        public final int def;
        public int index;

        public Choice(String key, String label, int def, String... options) {
            super(key, label);
            this.options = options;
            this.def = def;
            this.index = def;
        }

        public String get() { return options[index]; }

        public void next(int dir) { index = Math.floorMod(index + dir, options.length); }

        @Override
        void read(Store s, String p) {
            String v = s.str(p + key, options[def]);
            index = def;
            for (int i = 0; i < options.length; i++) if (options[i].equals(v)) index = i;
        }

        @Override
        void write(Store s, String p) { s.put(p + key, options[index]); }

        @Override
        public void reset() { index = def; }
    }

    /** Text value that isn't drawn on the settings screen (managed by its own screen, like the wardrobe). */
    public static final class Text extends Setting {
        public final String def;
        public String value;

        Text(String key, String def) {
            super(key, key);
            this.def = def;
            this.value = def;
        }

        @Override
        void read(Store s, String p) { value = s.str(p + key, def); }

        @Override
        void write(Store s, String p) { s.put(p + key, value); }

        @Override
        public void reset() { value = def; }
    }
}
