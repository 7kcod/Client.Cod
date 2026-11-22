package com.enc.pvpcod.module;

import com.enc.pvpcod.config.Store;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;

/**
 * Something drawn on the HUD. It sits at (x, y), stored as a fraction of the screen so it
 * stays in the same spot when the window or GUI scale changes. Drag it on the Edit HUD screen.
 */
public abstract class HudModule extends Module {

    public float x, y;
    private final float defX, defY;

    public final Setting.Num scale;
    public final Setting.Color textColor;
    public final Setting.Bool background;
    public final Setting.Color bgColor;
    public final Setting.Bool shadow;

    protected HudModule(String id, String name, Item icon, boolean defaultOn, float defX, float defY, String description) {
        super(id, name, Category.HUD, icon, defaultOn, description);
        this.defX = this.x = defX;
        this.defY = this.y = defY;
        scale = num("scale", "Size", 0.5f, 3f, 0.05f, 1f, "x");
        textColor = color("text", "Text colour", 0xFFFFFFFF);
        background = bool("bg", "Background", true);
        bgColor = color("bgcolor", "Background colour", 0x80000000);
        shadow = bool("shadow", "Text shadow", true);
    }

    /** Size before scaling. */
    public abstract int width(Font font);

    public abstract int height(Font font);

    /** Draw at (0, 0). editing = the Edit HUD screen is open (show sample values if there's nothing real). */
    public abstract void draw(GuiGraphics g, Font font, boolean editing);

    /** Whether to draw right now (e.g. Reach hides when you haven't hit anything for a while). */
    public boolean visible(Minecraft mc) {
        return true;
    }

    public int screenX(int screenW, Font font) {
        int w = Math.round(width(font) * scale.value);
        return Math.round(Math.max(0, Math.min(1, x)) * Math.max(0, screenW - w));
    }

    public int screenY(int screenH, Font font) {
        int h = Math.round(height(font) * scale.value);
        return Math.round(Math.max(0, Math.min(1, y)) * Math.max(0, screenH - h));
    }

    public void moveTo(int px, int py, int screenW, int screenH, Font font) {
        int w = Math.round(width(font) * scale.value);
        int h = Math.round(height(font) * scale.value);
        x = screenW - w <= 0 ? 0 : Math.max(0, Math.min(1, px / (float) (screenW - w)));
        y = screenH - h <= 0 ? 0 : Math.max(0, Math.min(1, py / (float) (screenH - h)));
    }

    public void resetPosition() {
        x = defX;
        y = defY;
    }

    @Override
    public void load(Store s) {
        super.load(s);
        x = s.num("hud." + id + ".x", defX);
        y = s.num("hud." + id + ".y", defY);
    }

    @Override
    public void save(Store s) {
        super.save(s);
        s.put("hud." + id + ".x", Float.toString(x));
        s.put("hud." + id + ".y", Float.toString(y));
    }

    // ------------------------------------------------------------------ helpers

    protected void box(GuiGraphics g, int w, int h) {
        if (background.value) g.fill(0, 0, w, h, bgColor.value);
    }

    protected void text(GuiGraphics g, Font font, String s, int x, int y, int color) {
        g.drawString(font, s, x, y, color, shadow.value);
    }
}
