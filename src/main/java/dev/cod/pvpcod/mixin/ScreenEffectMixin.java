package dev.cod.pvpcod.mixin;

import dev.cod.pvpcod.module.Modules;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Low fire and the underwater tint. */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectMixin {

    @Unique
    private static boolean pvpcod$pushed;

    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void pvpcod$fireStart(PoseStack pose, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo ci) {
        var lf = Modules.LOW_FIRE;
        if (!lf.enabled) return;
        if (lf.hide.value) {
            ci.cancel();
            return;
        }
        pose.pushPose();
        pose.translate(0f, -lf.lower.value, 0f);
        pvpcod$pushed = true;
    }

    @Inject(method = "renderFire", at = @At("RETURN"))
    private static void pvpcod$fireEnd(PoseStack pose, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (pvpcod$pushed) {
            pose.popPose();
            pvpcod$pushed = false;
        }
    }

    @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
    private static void pvpcod$water(Minecraft mc, PoseStack pose, MultiBufferSource buffers, CallbackInfo ci) {
        if (Modules.CLEAR_WATER.enabled && Modules.CLEAR_WATER.overlay.value) ci.cancel();
    }
}
