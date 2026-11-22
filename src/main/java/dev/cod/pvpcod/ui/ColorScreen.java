package dev.cod.pvpcod.ui;

import dev.cod.packcod.ui.PvpTheme;
import dev.cod.pvpcod.module.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Pick a colour: presets, hue / saturation / brightness / opacity sliders, or type a hex code. */
public final class ColorScreen extends UiScreen {

    private static final int[] PRESETS = {
            0xFFFFFFFF, 0xFFC7C7C7, 0xFF7F7F7F, 0xFF3A3A3A, 0xFF000000, 0x80000000, 0x40000000, 0x00000000,
            0xFFE8A33D, 0xFFFFD84A, 0xFF8DBF6A, 0xFF41D97A, 0xFF4FC3F7, 0xFF5B7DF5, 0xFFC08CFF, 0xFFFF8FB8,
            0xFFFF5A36, 0xFFD9614C, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFF00FFFF, 0xFFFF00FF};

    private final String what;
    private final Setting.Color target;
    private final int original;
    private final Setting.Num hue = new Setting.Num("h", "Hue", 0, 360, 1, 0, "");
    private final Setting.Num sat = new Setting.Num("s", "Saturation", 0, 100, 1, 0, "%");
    private final Setting.Num bri = new Setting.Num("b", "Brightness", 0, 100, 1, 0, "%");
    private final Setting.Num alpha = new Setting.Num("a", "Opacity", 0, 100, 1, 100, "%");
    private EditBox hex;
    private boolean syncing;

    public ColorScreen(Screen parent, String what, Setting.Color target) {
        super("Colour", parent);
        this.what = what;
        this.target = target;
        this.original = target.value;
        fromColor(target.value);
    }

    @Override
    protected void init() {
        int w = Math.min(300, width - M * 2);
        int x = (width - w) / 2;
        hex = new EditBox(font, x + 52, TOP + M + 66, 70, 10, Component.literal("Hex"));
        searchBox(hex);
        hex.setMaxLength(9);
        hex.setValue(String.format("#%08X", target.value));
        hex.setResponder(s -> {
            if (syncing) return;
            String t = s.trim().replace("#", "");
            try {
                if (t.length() == 6) t = "FF" + t;
                if (t.length() == 8) {
                    target.value = (int) Long.parseLong(t, 16);
                    fromColor(target.value);
                }
            } catch (NumberFormatException ignored) {
            }
        });
        addRenderableWidget(hex);
    }

    private void setHex(int argb) {
        syncing = true;
        hex.setValue(String.format("#%08X", argb));
        syncing = false;
    }

    private void fromColor(int argb) {
        float[] hsb = rgbToHsb((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, null);
        hue.value = hsb[0] * 360;
        sat.value = hsb[1] * 100;
        bri.value = hsb[2] * 100;
        alpha.value = Math.round(((argb >>> 24) & 0xFF) / 2.55f);
    }

    private void fromSliders() {
        int rgb = hsbToRgb(hue.value / 360f, sat.value / 100f, bri.value / 100f) & 0xFFFFFF;
        int a = Math.round(alpha.value * 2.55f);
        target.value = (a << 24) | rgb;
        if (hex != null && !hex.isFocused()) setHex(target.value);
    }

    @Override
    protected void draw(GuiGraphics g, int mx, int my) {
        topBar(g, "Colour: " + what);
        int w = Math.min(300, width - M * 2);
        int x = (width - w) / 2;
        int y = TOP + M;
        PvpTheme.panel(g, x, y, w, height - y - M);

        swatch(g, x + 8, y + 8, 36, 36, target.value);
        swatch(g, x + 8, y + 46, 36, 10, original);
        g.drawString(font, "New", x + 50, y + 10, PvpTheme.text(), false);
        g.drawString(font, "Old (click to go back)", x + 50, y + 47, PvpTheme.faint(), false);
        hits.add(new Hit(x + 8, y + 46, 36, 10, b -> {
            target.value = original;
            fromColor(original);
            setHex(original);
        }, null));
        PvpTheme.slot(g, hex.getX() - 4, hex.getY() - 3, hex.getWidth() + 8, 15);
        g.drawString(font, "Hex", x + 8, hex.getY(), PvpTheme.dim(), false);

        int sy = y + 86;
        Setting.Num[] sl = {hue, sat, bri, alpha};
        for (Setting.Num n : sl) {
            g.drawString(font, n.label, x + 8, sy + 1, PvpTheme.text(), false);
            int slx = x + 80, slw = w - 80 - 44;
            // a strip showing the range of the slider
            for (int i = 0; i < slw; i++) {
                float t = i / (float) slw;
                int c;
                if (n == hue) c = 0xFF000000 | hsbToRgb(t, 1, 1);
                else if (n == sat) c = 0xFF000000 | hsbToRgb(hue.value / 360f, t, bri.value / 100f);
                else if (n == bri) c = 0xFF000000 | hsbToRgb(hue.value / 360f, sat.value / 100f, t);
                else c = (Math.round(t * 255) << 24) | (target.value & 0xFFFFFF);
                g.fill(slx + i, sy + 11, slx + i + 1, sy + 13, c);
            }
            slider(g, slx, sy, slw, n, mx, my, this::fromSliders);
            g.drawString(font, n.shown(), x + w - 8 - font.width(n.shown()), sy + 1, PvpTheme.dim(), false);
            sy += 20;
        }

        g.drawString(font, "Presets", x + 8, sy + 2, PvpTheme.dim(), false);
        sy += 14;
        int cs = 14, per = Math.max(1, (w - 16) / (cs + 3));
        for (int i = 0; i < PRESETS.length; i++) {
            int px = x + 8 + (i % per) * (cs + 3), py = sy + (i / per) * (cs + 3);
            swatch(g, px, py, cs, cs, PRESETS[i]);
            if (PRESETS[i] == target.value) PvpTheme.outline(g, px - 1, py - 1, cs + 2, cs + 2, PvpTheme.accent());
            int col = PRESETS[i];
            hits.add(new Hit(px, py, cs, cs, b -> {
                target.value = col;
                fromColor(col);
                setHex(col);
            }, null));
        }

        int by = height - M - 22;
        button(g, x + w - 8 - bw("Done"), by, bw("Done"), 16, "Done", mx, my, true, b -> onClose());
        button(g, x + w - 12 - bw("Done") - bw("Cancel"), by, bw("Cancel"), 16, "Cancel", mx, my, false, b -> {
            target.value = original;
            onClose();
        });
    }

    static float[] rgbToHsb(int r, int g, int b, Object unused) {
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float bri = max / 255f, sat = max == 0 ? 0 : (max - min) / (float) max, hue = 0;
        if (max != min) {
            float rc = (max - r) / (float) (max - min), gc = (max - g) / (float) (max - min), bc = (max - b) / (float) (max - min);
            if (r == max) hue = bc - gc;
            else if (g == max) hue = 2f + rc - bc;
            else hue = 4f + gc - rc;
            hue /= 6f;
            if (hue < 0) hue += 1f;
        }
        return new float[]{hue, sat, bri};
    }

    public static int hsbToRgb(float h, float s, float v) {
        float r, g, b;
        if (s == 0) {
            r = g = b = v;
        } else {
            float hh = (h - (float) Math.floor(h)) * 6f;
            int i = (int) hh;
            float f = hh - i, p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
            switch (i) {
                case 0 -> { r = v; g = t; b = p; }
                case 1 -> { r = q; g = v; b = p; }
                case 2 -> { r = p; g = v; b = t; }
                case 3 -> { r = p; g = q; b = v; }
                case 4 -> { r = t; g = p; b = v; }
                default -> { r = v; g = p; b = q; }
            }
        }
        return 0xFF000000 | (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }
}
