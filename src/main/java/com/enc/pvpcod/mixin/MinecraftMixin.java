package com.enc.pvpcod.mixin;

import com.enc.pvpcod.module.Modules;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Middle click pings instead of picking blocks while Team pings is on (not in creative). */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void pvpcod$noPick(CallbackInfo ci) {
        var p = Modules.PINGS;
        if (p.active() && p.noPick.value) ci.cancel();
    }
}
