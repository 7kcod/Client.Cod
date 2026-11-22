package dev.cod.pvpcod.mixin;

import dev.cod.pvpcod.ShulkerTooltip;
import dev.cod.pvpcod.module.Modules;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Shulker box tooltips: a grid of what's inside. */
@Mixin(Item.class)
public abstract class ShulkerMixin {

    @Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
    private void pvpcod$shulker(ItemStack stack, CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        if (!Modules.SHULKER.enabled || cir.getReturnValue().isPresent()) return;
        ShulkerTooltip.Data d = ShulkerTooltip.of(stack);
        if (d != null) cir.setReturnValue(Optional.of(d));
    }
}
