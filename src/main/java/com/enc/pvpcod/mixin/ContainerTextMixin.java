package com.enc.pvpcod.mixin;

import com.enc.pvpcod.module.Modules;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/** With shulker tooltips on, the text list of items is hidden (the grid shows them). */
@Mixin(ItemContainerContents.class)
public abstract class ContainerTextMixin {

    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private void pvpcod$hide(Item.TooltipContext ctx, Consumer<Component> out, TooltipFlag flag, DataComponentGetter components, CallbackInfo ci) {
        var m = Modules.SHULKER;
        if (m.enabled && m.hideText.value) ci.cancel();
    }
}
