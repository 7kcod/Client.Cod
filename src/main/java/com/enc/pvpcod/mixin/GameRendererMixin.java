package com.enc.pvpcod.mixin;

import com.enc.pvpcod.module.Modules;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom (divides the field of view) and no hurt cam. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void pvpcod$zoom(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        if (!useFovSetting) return;
        float d = Modules.ZOOM.divisor();
        if (d != 1f) cir.setReturnValue(cir.getReturnValue() / d);
        com.enc.pvpcod.Pings.fov = cir.getReturnValue();
    }

    /** Rescale aspect ratio: the world is drawn for another shape and stretched to your window. */
    @ModifyArg(method = "getProjectionMatrix", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;"), index = 1)
    private float pvpcod$aspect(float aspect) {
        return Modules.ASPECT.enabled ? Modules.ASPECT.value() : aspect;
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void pvpcod$noHurtCam(PoseStack pose, float partialTick, CallbackInfo ci) {
        if (Modules.NO_HURT_CAM.enabled) ci.cancel();
    }
}
