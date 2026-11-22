package com.enc.pvpcod.ui;

import com.enc.packcod.ui.PvpTheme;
import com.enc.pvpcod.PvpCod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Credits: who made pvp.cod (same page as pack.cod's). */
public final class CreditsScreen extends UiScreen {

    private static final Identifier HEAD = Identifier.fromNamespaceAndPath("packcod", "textures/gui/credits_head.png");
    private static final ItemStack COD = new ItemStack(Items.COD);

    public CreditsScreen(Screen parent) {
        super("Credits", parent);
    }

    @Override
    protected void draw(GuiGraphics g, int mx, int my) {
        int cx = width / 2;
        double t = System.currentTimeMillis() / 1000.0;

        // logo
        int ly = Math.max(10, height / 2 - 112);
        g.renderItem(COD, cx - 44, ly);
        g.drawString(font, "pvp", cx - 24, ly + 4, PvpTheme.text(), false);
        g.drawString(font, ".cod", cx - 24 + font.width("pvp"), ly + 4, PvpTheme.accent(), false);
        String v = "v" + PvpCod.VERSION;
        g.drawString(font, v, cx - 24 + font.width("pvp.cod") + 4, ly + 4, PvpTheme.faint(), false);

        // the dev's head, bobbing a little
        int size = Math.min(96, height / 3);
        float bob = (float) Math.sin(t * 1.8) * 3f;
        g.pose().pushMatrix();
        g.pose().translate(cx, ly + 30 + size / 2f + bob);
        g.pose().rotate((float) Math.toRadians(Math.sin(t * 1.2) * 3));
        g.blit(RenderPipelines.GUI_TEXTURED, HEAD, -size / 2, -size / 2, 0f, 0f, size, size, 256, 256, 256, 256);
        g.pose().popMatrix();

        // name, big
        int ny = ly + 40 + size;
        String made = "Made by 7kcod";
        g.pose().pushMatrix();
        g.pose().translate(cx - font.width(made) - 11, ny);
        g.pose().scale(2f, 2f);
        g.drawString(font, "Made by ", 0, 0, PvpTheme.text(), false);
        g.drawString(font, "7kcod", font.width("Made by "), 0, PvpTheme.accent(), false);
        // a little Belarusian flag next to the name: red over green, with the white and red ornament at the hoist
        int fx = font.width(made) + 4, fw = 18, fh = 9;
        g.fill(fx - 1, -1, fx + fw + 1, fh + 1, 0x60000000);
        g.fill(fx, 0, fx + fw, 6, 0xFFC8313E);
        g.fill(fx, 6, fx + fw, fh, 0xFF4AA657);
        g.fill(fx, 0, fx + 3, fh, 0xFFFFFFFF);
        // little red diamonds down the white ornament band
        for (int i = 0; i < fh; i++) {
            if (i % 4 == 0) g.fill(fx + 1, i, fx + 2, i + 1, 0xFFC8313E);
            if (i % 4 == 2) {
                g.fill(fx, i, fx + 1, i + 1, 0xFFC8313E);
                g.fill(fx + 2, i, fx + 3, i + 1, 0xFFC8313E);
            }
        }
        g.pose().popMatrix();
        String solo = "solo dev";
        g.drawString(font, solo, cx - font.width(solo) / 2, ny + 22, PvpTheme.dim(), false);
        String pc = "with pack.cod built in";
        g.drawString(font, pc, cx - font.width(pc) / 2, ny + 34, PvpTheme.faint(), false);

        // back
        int bw = 70, bh = 18;
        int by = Math.min(height - bh - 8, ny + 52);
        button(g, cx - bw / 2, by, bw, bh, "← Back", mx, my, false, b -> onClose());
    }
}
