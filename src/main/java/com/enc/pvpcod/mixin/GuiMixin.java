package com.enc.pvpcod.mixin;

import com.enc.pvpcod.module.Modules;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pumpkin blur: skip the carved pumpkin overlay (and the frost, if you want). */
@Mixin(Gui.class)
public abstract class GuiMixin {

    @Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
    private void pvpcod$overlay(GuiGraphics g, Identifier texture, float alpha, CallbackInfo ci) {
        var np = Modules.NO_PUMPKIN;
        if (!np.enabled || texture == null) return;
        String path = texture.getPath();
        if (path.contains("pumpkinblur") || (np.powderSnow.value && path.contains("powder_snow_outline"))) ci.cancel();
    }
}
