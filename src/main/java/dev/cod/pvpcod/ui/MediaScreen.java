package dev.cod.pvpcod.ui;

import dev.cod.packcod.ui.PvpTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Media: what creators and SMPs get, and an Apply button (not open yet). */
public final class MediaScreen extends UiScreen {

    private long appliedAt = -1;

    public MediaScreen(Screen parent) {
        super("Media", parent);
    }

    @Override
    protected void draw(GuiGraphics g, int mx, int my) {
        topBar(g, "Media");
        String back = "Back";
        button(g, width - M - bw(back), 3, bw(back), 16, back, mx, my, false, b -> onClose());

        int w = Math.min(320, width - M * 2), x = (width - w) / 2;
        int h = 168, y = Math.max(TOP + M, (height - h) / 2);
        PvpTheme.panel(g, x, y, w, h);

        // icons along the top: a cape, a camera-ish spyglass, a banner
        ItemStack[] icons = {new ItemStack(Items.WHITE_BANNER), new ItemStack(Items.SPYGLASS), new ItemStack(Items.ELYTRA)};
        for (int i = 0; i < icons.length; i++) g.renderItem(icons[i], x + w / 2 - 30 + i * 22, y + 10);

        String title = "Creators & SMPs";
        g.pose().pushMatrix();
        g.pose().translate(x + w / 2f - font.width(title), y + 32);
        g.pose().scale(2f, 2f);
        g.drawString(font, title, 0, 0, PvpTheme.accent(), false);
        g.pose().popMatrix();

        List<String> lines = List.of(
                "Being media gives Creators and SMPs the chance to",
                "unlock custom cosmetic packs if they use the cod client.",
                "",
                "This will allow them to make their own cape with us.");
        int ly = y + 58;
        for (String l : lines) {
            String f = PvpTheme.fit(font, l, w - 20);
            g.drawString(font, f, x + (w - font.width(f)) / 2, ly, l.isEmpty() ? 0 : PvpTheme.text(), false);
            ly += 11;
        }

        int bwid = 100, bx = x + (w - bwid) / 2, by = y + h - 40;
        button(g, bx, by, bwid, 20, "Apply", mx, my, true, b -> appliedAt = System.currentTimeMillis());
        if (appliedAt > 0) {
            String cs = "Coming soon!";
            float pop = Math.max(0, 1 - (System.currentTimeMillis() - appliedAt) / 250f);
            g.pose().pushMatrix();
            g.pose().translate(x + w / 2f, by + 30);
            g.pose().scale(1f + pop * 0.4f, 1f + pop * 0.4f);
            g.drawString(font, cs, -font.width(cs) / 2, -4, PvpTheme.accent(), true);
            g.pose().popMatrix();
        }
    }
}
