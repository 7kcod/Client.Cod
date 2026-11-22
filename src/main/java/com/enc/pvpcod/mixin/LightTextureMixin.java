package com.enc.pvpcod.mixin;

import com.enc.pvpcod.module.Modules;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Fullbright: the light map reads a huge gamma instead of your brightness setting. */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {

    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 2))
    private Object pvpcod$gamma(OptionInstance<?> option) {
        return Modules.FULLBRIGHT.enabled ? (Object) Double.valueOf(15.0) : option.get();
    }
}
