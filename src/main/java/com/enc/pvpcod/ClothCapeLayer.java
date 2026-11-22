package com.enc.pvpcod;

import com.enc.pvpcod.module.Modules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;

/**
 * Cloth capes: the cape is drawn as 16 strips that bend more towards the bottom and ripple
 * as you move, instead of one stiff board. Replaces the normal cape (Cape swapper setting).
 */
public final class ClothCapeLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final float P = 1f / 16f;
    private static final int SEGMENTS = 16;

    public ClothCapeLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    /** True when cloth capes are drawn for this player (then the normal cape is skipped). */
    public static boolean active(AvatarRenderState state) {
        var c = Modules.CAPE;
        if (!c.cloth.value) return false;
        Minecraft mc = Minecraft.getInstance();
        boolean me = mc.player != null && state.id == mc.player.getId();
        return me || c.clothAll.value;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible || !state.showCape || state.skin == null || state.skin.cape() == null || !active(state)) return;
        // no cape while wearing an elytra (same as the game)
        if (state.chestEquipment != null && state.chestEquipment.has(DataComponents.GLIDER)) return;
        Identifier tex = state.skin.cape().texturePath();
        float wave = Modules.CAPE.clothWave.value;
        float t = state.ageInTicks;

        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        if (state.chestEquipment != null && !state.chestEquipment.isEmpty()) pose.translate(0, -0.053f, 0.06f);
        pose.translate(0, 0, 2 * P);
        // how far the cape swings out behind you (the game's own numbers)
        float base = 6f + state.capeLean / 2f + state.capeFlap;
        float side = state.capeLean2 / 2f;
        pose.mulPose(Axis.ZP.rotationDegrees(side));
        float movement = Math.min(1f, Math.abs(state.capeLean) / 40f + 0.15f);
        for (int i = 0; i < SEGMENTS; i++) {
            float f = i / (float) SEGMENTS;
            // stiff at the shoulders, looser at the bottom, rippling along its length
            float bend = i == 0 ? base * 0.75f : base * 0.25f / (SEGMENTS - 1)
                    + (float) Math.sin(t * 0.32f - i * 0.55f) * 2.2f * wave * movement * f;
            pose.mulPose(Axis.XP.rotationDegrees(bend));
            int seg = i;
            collector.submitCustomGeometry(pose, RenderTypes.entityCutoutNoCull(tex), (p, vc) -> strip(vc, p, seg, light));
            pose.translate(0, P, 0);
        }
        pose.popPose();
    }

    /** One 10 x 1 pixel strip of the cape: outside, inside, and its two edges. */
    private static void strip(VertexConsumer vc, PoseStack.Pose p, int row, int light) {
        float x0 = -5 * P, x1 = 5 * P, y0 = 0, y1 = P, zo = 0.5f * P, zi = -0.5f * P;
        float v0 = (1 + row) / 32f, v1 = (2 + row) / 32f;
        // outside (what people behind you see): u 1..11
        quad(vc, p, light, 0, 0, 1,
                x1, y0, zo, 1 / 64f, v0,
                x0, y0, zo, 11 / 64f, v0,
                x0, y1, zo, 11 / 64f, v1,
                x1, y1, zo, 1 / 64f, v1);
        // inside (towards your back): u 12..22
        quad(vc, p, light, 0, 0, -1,
                x0, y0, zi, 12 / 64f, v0,
                x1, y0, zi, 22 / 64f, v0,
                x1, y1, zi, 22 / 64f, v1,
                x0, y1, zi, 12 / 64f, v1);
        // edges: u 0..1 and 11..12
        quad(vc, p, light, -1, 0, 0,
                x0, y0, zi, 11 / 64f, v0, x0, y0, zo, 12 / 64f, v0, x0, y1, zo, 12 / 64f, v1, x0, y1, zi, 11 / 64f, v1);
        quad(vc, p, light, 1, 0, 0,
                x1, y0, zo, 0, v0, x1, y0, zi, 1 / 64f, v0, x1, y1, zi, 1 / 64f, v1, x1, y1, zo, 0, v1);
        if (row == 0) {
            // top edge: u 1..11, v 0..1
            quad(vc, p, light, 0, -1, 0,
                    x0, y0, zi, 1 / 64f, 0, x1, y0, zi, 11 / 64f, 0, x1, y0, zo, 11 / 64f, 1 / 32f, x0, y0, zo, 1 / 64f, 1 / 32f);
        }
        if (row == SEGMENTS - 1) {
            // bottom edge: u 11..21, v 0..1
            quad(vc, p, light, 0, 1, 0,
                    x0, y1, zo, 11 / 64f, 0, x1, y1, zo, 21 / 64f, 0, x1, y1, zi, 21 / 64f, 1 / 32f, x0, y1, zi, 11 / 64f, 1 / 32f);
        }
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, int light, float nx, float ny, float nz, float... v) {
        for (int i = 0; i < 4; i++) {
            int o = i * 5;
            vc.addVertex(p, v[o], v[o + 1], v[o + 2]).setColor(0xFFFFFFFF).setUv(v[o + 3], v[o + 4])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, -ny, nz);
        }
    }
}
