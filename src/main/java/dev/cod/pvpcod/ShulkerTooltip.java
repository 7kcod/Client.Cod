package dev.cod.pvpcod;

import dev.cod.pvpcod.module.Modules;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** The 9x3 grid shown when you hover a shulker box. */
public final class ShulkerTooltip {

    public record Data(NonNullList<ItemStack> items, int color) implements TooltipComponent {}

    /** The grid data for a shulker box with something in it, else null. */
    public static Data of(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof ShulkerBoxBlock box)) return null;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null || contents.nonEmptyStream().findAny().isEmpty()) return null;
        NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        contents.copyInto(items);
        DyeColor dye = box.getColor();
        int color = dye == null ? 0xFF8E6B8E : 0xFF000000 | dye.getTextureDiffuseColor();
        return new Data(items, color);
    }

    public static final class Client implements ClientTooltipComponent {
        private final Data data;

        public Client(Data data) {
            this.data = data;
        }

        @Override
        public int getHeight(Font font) {
            return 3 * 18 + 6;
        }

        @Override
        public int getWidth(Font font) {
            return 9 * 18 + 2;
        }

        @Override
        public void renderImage(Font font, int x, int y, int w, int h, GuiGraphics g) {
            int bg = Modules.SHULKER.tint.value ? (0x99000000 | (data.color() & 0xFFFFFF)) : 0x66000000;
            g.fill(x, y, x + 9 * 18 + 2, y + 3 * 18 + 2, bg);
            for (int i = 0; i < data.items().size() && i < 27; i++) {
                int sx = x + 1 + (i % 9) * 18, sy = y + 1 + (i / 9) * 18;
                g.fill(sx, sy, sx + 18, sy + 18, 0x40000000);
                g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x30FFFFFF);
                ItemStack s = data.items().get(i);
                if (s.isEmpty()) continue;
                g.renderItem(s, sx + 1, sy + 1);
                g.renderItemDecorations(font, s, sx + 1, sy + 1);
            }
        }
    }

    private ShulkerTooltip() {
    }
}
