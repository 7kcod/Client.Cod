package dev.cod.pvpcod.mixin;

import dev.cod.pvpcod.module.Modules;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clear lava: see through lava instead of a wall of orange. */
@Mixin(LavaFogEnvironment.class)
public abstract class LavaFogMixin {

    @Inject(method = "setupFog", at = @At("TAIL"))
    private void pvpcod$clear(FogData data, Camera camera, ClientLevel level, float renderDistance, DeltaTracker delta, CallbackInfo ci) {
        var cl = Modules.CLEAR_LAVA;
        if (!cl.active()) return;
        float end = Math.max(data.environmentalEnd, Math.max(data.renderDistanceEnd, renderDistance) * cl.view.value / 100f);
        data.environmentalStart = Math.max(data.environmentalStart, end * 0.3f);
        data.environmentalEnd = end;
        data.skyEnd = Math.max(data.skyEnd, end);
        data.cloudEnd = Math.max(data.cloudEnd, end);
    }
}
