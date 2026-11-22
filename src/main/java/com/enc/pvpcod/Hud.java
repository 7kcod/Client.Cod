package com.enc.pvpcod;

import com.enc.pvpcod.module.HudModule;
import com.enc.pvpcod.module.Modules;
import com.enc.pvpcod.ui.HudEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Draws every HUD module that's switched on. */
public final class Hud {

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        Tracker.frame(mc);
        if (mc.options.hideGui || mc.screen instanceof HudEditorScreen) return;
        if (mc.debugEntries != null && mc.debugEntries.isOverlayVisible()) return;
        Font font = mc.font;
        int sw = g.guiWidth(), sh = g.guiHeight();
        for (HudModule m : Modules.hud()) {
            if (!m.enabled || !m.visible(mc)) continue;
            drawAt(g, font, m, m.screenX(sw, font), m.screenY(sh, font), false);
        }
    }

    public static void drawAt(GuiGraphics g, Font font, HudModule m, int x, int y, boolean editing) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(m.scale.value, m.scale.value);
        try {
            m.draw(g, font, editing);
        } catch (RuntimeException ignored) {
            // A HUD box should never crash the game.
        }
        g.pose().popMatrix();
    }

    private Hud() {
    }
}
