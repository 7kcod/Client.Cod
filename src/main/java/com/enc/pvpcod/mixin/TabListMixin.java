package com.enc.pvpcod.mixin;

import com.enc.pvpcod.Social;
import com.enc.pvpcod.module.Modules;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Icons and friend colours in the tab list. */
@Mixin(PlayerTabOverlay.class)
public abstract class TabListMixin {

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void pvpcod$icon(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
                cir.setReturnValue(Social.decorate(info.getProfile().name(), info.getProfile().id(), cir.getReturnValue(), Modules.NAME_ICONS.tab.value));
    }
}
