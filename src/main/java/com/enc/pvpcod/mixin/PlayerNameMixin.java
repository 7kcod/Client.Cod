package com.enc.pvpcod.mixin;

import com.enc.pvpcod.Social;
import com.enc.pvpcod.module.Modules;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The cod (or whatever icon) above players' heads, and friend colours. */
@Mixin(Player.class)
public abstract class PlayerNameMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void pvpcod$icon(CallbackInfoReturnable<Component> cir) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide()) return;
        cir.setReturnValue(Social.decorate(self.getGameProfile().name(), self.getUUID(), cir.getReturnValue(), Modules.NAME_ICONS.nametags.value));
    }
}
