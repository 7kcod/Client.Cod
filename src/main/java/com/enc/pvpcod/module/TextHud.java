package com.enc.pvpcod.module;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;

/** A HUD box with one line of text like "FPS: 144". */
public abstract class TextHud extends HudModule {

    private static final int PAD = 4;
    public final Setting.Choice layout;
    public final Setting.Color labelColor;

    protected TextHud(String id, String name, Item icon, boolean defaultOn, float defX, float defY, String description) {
        super(id, name, icon, defaultOn, defX, defY, description);
        layout = choice("layout", "Layout", 0, "Label: value", "value label", "[label value]", "value only");
        labelColor = color("labelcolor", "Label colour", 0xFFE8A33D);
    }

    protected abstract String label();

    /** The value to show. editing = show a sample if there's no real value. */
    protected abstract String value(boolean editing);

    private String[] parts(boolean editing) {
        String l = label(), v = value(editing);
        return switch (layout.index) {
            case 1 -> new String[]{"", v + " ", l};
            case 2 -> new String[]{"[", l + " ", v + "]"};
            case 3 -> new String[]{"", "", v};
            default -> new String[]{"", l + ": ", v};
        };
    }

    @Override
    public int width(Font font) {
        String[] p = parts(true);
        return font.width(p[0] + p[1] + p[2]) + PAD * 2;
    }

    @Override
    public int height(Font font) {
        return 9 + PAD * 2 - 1;
    }

    @Override
    public void draw(GuiGraphics g, Font font, boolean editing) {
        String[] p = parts(editing);
        int w = font.width(p[0] + p[1] + p[2]) + PAD * 2;
        box(g, Math.max(w, width(font)), height(font));
        int x = PAD + Math.max(0, (width(font) - w) / 2);
        if (layout.index == 1) {
            text(g, font, p[1], x, PAD, textColor.value);
            text(g, font, p[2], x + font.width(p[1]), PAD, labelColor.value);
            return;
        }
        text(g, font, p[0], x, PAD, labelColor.value);
        x += font.width(p[0]);
        text(g, font, p[1], x, PAD, labelColor.value);
        x += font.width(p[1]);
        text(g, font, p[2], x, PAD, textColor.value);
    }
}
