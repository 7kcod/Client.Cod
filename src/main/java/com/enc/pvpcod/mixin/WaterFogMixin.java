package com.enc.pvpcod.mixin;

import com.enc.pvpcod.module.Modules;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clear water: push the underwater fog out to (a share of) your render distance. */
@Mixin(WaterFogEnvironment.class)
public abstract class WaterFogMixin {

    @Inject(method = "setupFog", at = @At("TAIL"))
    private void pvpcod$clear(FogData data, Camera camera, ClientLevel level, float renderDistance, DeltaTracker delta, CallbackInfo ci) {
        var cw = Modules.CLEAR_WATER;
        if (!cw.enabled) return;
        float end = Math.max(data.environmentalEnd, Math.max(data.renderDistanceEnd, renderDistance) * cw.view.value / 100f);
        data.environmentalStart = Math.max(data.environmentalStart, end * 0.6f);
        data.environmentalEnd = end;
        data.skyEnd = Math.max(data.skyEnd, end);
        data.cloudEnd = Math.max(data.cloudEnd, end);
    }
}
