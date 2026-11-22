package com.enc.pvpcod.mixin;

import com.enc.pvpcod.module.Modules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Block outline: your colour and thickness, and an optional see-through fill. */
@Mixin(LevelRenderer.class)
public abstract class BlockOutlineMixin {

    @ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int pvpcod$color(int color) {
        var bo = Modules.BLOCK_OUTLINE;
        return bo.enabled ? bo.outline() : color;
    }

    @ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float pvpcod$width(float width) {
        var bo = Modules.BLOCK_OUTLINE;
        return bo.enabled ? bo.thickness.value : width;
    }

    @Inject(method = "renderBlockOutline", at = @At("TAIL"))
    private void pvpcod$fill(MultiBufferSource.BufferSource buffers, PoseStack pose, boolean translucent, LevelRenderState state, CallbackInfo ci) {
        var bo = Modules.BLOCK_OUTLINE;
        BlockOutlineRenderState o = state.blockOutlineRenderState;
        if (!bo.enabled || !bo.fill.value || o == null || o.isTranslucent() != translucent || state.cameraRenderState == null) return;
        Vec3 cam = state.cameraRenderState.pos;
        int c = bo.fillNow();
        VertexConsumer vc = buffers.getBuffer(RenderTypes.debugQuads());
        PoseStack.Pose p = pose.last();
        for (AABB b : o.shape().toAabbs()) {
            AABB a = b.move(o.pos()).move(-cam.x, -cam.y, -cam.z).inflate(0.003);
            float x0 = (float) a.minX, y0 = (float) a.minY, z0 = (float) a.minZ, x1 = (float) a.maxX, y1 = (float) a.maxY, z1 = (float) a.maxZ;
            quad(vc, p, c, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
            quad(vc, p, c, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
            quad(vc, p, c, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
            quad(vc, p, c, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1);
            quad(vc, p, c, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
            quad(vc, p, c, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
        }
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, int c, float... v) {
        for (int i = 0; i < 4; i++) vc.addVertex(p, v[i * 3], v[i * 3 + 1], v[i * 3 + 2]).setColor(c);
    }
}
