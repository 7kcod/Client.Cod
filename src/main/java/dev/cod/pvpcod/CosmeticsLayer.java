package dev.cod.pvpcod;

import dev.cod.pvpcod.module.Modules;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Draws your cosmetics (halo, hats, ears, wings...) on your own player. They're simple coloured
 * blocks built in code, attached to your head and body so they move with you.
 * Only you see them (other players would need pvp.cod and a server to share them).
 */
public final class CosmeticsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final Identifier WHITE = Identifier.fromNamespaceAndPath("pvpcod", "textures/misc/white.png");
    private static final float P = 1f / 16f; // one model pixel

    public CosmeticsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        var c = Modules.COSMETICS;
        Minecraft mc = Minecraft.getInstance();
        if (!c.enabled || state.isInvisible || mc.player == null || state.id != mc.player.getId()) return;
        float t = state.ageInTicks;
        int glow = 0xF000F0;

        // ---- head things
        pose.pushPose();
        getParentModel().head.translateAndRotate(pose);
        collector.submitCustomGeometry(pose, RenderTypes.entityCutoutNoCull(WHITE), (p, vc) -> {
            if (c.halo.value) {
                float bob = (float) Math.sin(t * 0.1) * 0.4f;
                int col = c.haloColor.value;
                for (int i = 0; i < 16; i++) {
                    double a = Math.PI * 2 * i / 16;
                    float x = (float) Math.cos(a) * 4.5f, z = (float) Math.sin(a) * 4.5f;
                    box(vc, p, x - 0.8f, -12.5f + bob, z - 0.8f, x + 0.8f, -12f + bob, z + 0.8f, col, glow);
                }
            }
            if (c.topHat.value) {
                int black = 0xFF1A1A1A;
                box(vc, p, -6, -8.6f, -6, 6, -8.0f, 6, black, light);              // brim
                box(vc, p, -4, -15, -4, 4, -8.6f, 4, black, light);                 // crown of the hat
                box(vc, p, -4.1f, -10.2f, -4.1f, 4.1f, -9.0f, 4.1f, c.topHatColor.value, light); // band
            }
            if (c.crown.value) {
                int col = c.crownColor.value;
                ring(vc, p, -4.4f, -10f, -8.2f, 4.4f, col, light);
                for (int i = 0; i < 4; i++) {
                    float o = -3.4f + i * 2.3f;
                    box(vc, p, o, -11.4f, -4.4f, o + 1, -10f, -3.6f, col, light);
                    box(vc, p, o, -11.4f, 3.6f, o + 1, -10f, 4.4f, col, light);
                    box(vc, p, -4.4f, -11.4f, o, -3.6f, -10f, o + 1, col, light);
                    box(vc, p, 3.6f, -11.4f, o, 4.4f, -10f, o + 1, col, light);
                }
                box(vc, p, -0.6f, -9.6f, -4.6f, 0.6f, -8.6f, -4.3f, 0xFFE0103A, glow); // a gem at the front
            }
            if (c.ears.value) {
                int col = c.earsColor.value, inner = 0xFFF4A9B8;
                for (int side = -1; side <= 1; side += 2) {
                    float cx = side * 2.6f;
                    box(vc, p, cx - 1.5f, -9.5f, -1, cx + 1.5f, -8, 1, col, light);
                    box(vc, p, cx - 1f, -10.5f, -0.8f, cx + 1f, -9.5f, 0.8f, col, light);
                    box(vc, p, cx - 0.5f, -11.3f, -0.6f, cx + 0.5f, -10.5f, 0.6f, col, light);
                    box(vc, p, cx - 0.8f, -9.6f, -1.05f, cx + 0.8f, -8.4f, -1f, inner, light);
                }
            }
            if (c.horns.value) {
                int col = c.hornsColor.value;
                for (int side = -1; side <= 1; side += 2) {
                    float cx = side * 3f;
                    box(vc, p, cx - 1f, -9.5f, -2.5f, cx + 1f, -8f, -0.5f, col, light);
                    box(vc, p, cx - 0.8f + side * 0.6f, -11f, -2.3f, cx + 0.8f + side * 0.6f, -9.5f, -0.7f, col, light);
                    box(vc, p, cx - 0.5f + side * 1.3f, -12.2f, -2f, cx + 0.5f + side * 1.3f, -11f, -1f, col, light);
                }
            }
            if (c.shades.value) {
                int col = c.shadesColor.value;
                box(vc, p, -3.8f, -5.2f, -4.6f, -0.6f, -3.2f, -4.15f, col, light);
                box(vc, p, 0.6f, -5.2f, -4.6f, 3.8f, -3.2f, -4.15f, col, light);
                box(vc, p, -0.6f, -4.8f, -4.5f, 0.6f, -4.3f, -4.2f, col, light);
                box(vc, p, -4.3f, -4.9f, -4.5f, -3.8f, -4.4f, 2f, col, light);
                box(vc, p, 3.8f, -4.9f, -4.5f, 4.3f, -4.4f, 2f, col, light);
                box(vc, p, -3.2f, -5f, -4.65f, -2.2f, -4.5f, -4.6f, 0x66FFFFFF, light); // shine
            }
            if (c.bandana.value) {
                int col = c.bandanaColor.value;
                ring(vc, p, -4.35f, -6.6f, -5.2f, 4.35f, col, light);
                box(vc, p, -0.8f, -6.4f, 4.3f, 0.8f, -5.2f, 4.9f, col, light);   // knot
                box(vc, p, -1.6f, -5.4f, 4.4f, -0.2f, -3.2f, 4.7f, col, light);  // tails
                box(vc, p, 0.2f, -5.4f, 4.4f, 1.6f, -3.6f, 4.7f, col, light);
            }
            if (c.codHat.value) {
                // a chunky cod lying on your head
                int body = 0xFFA6896B, back = 0xFF7D6650, fin = 0xFF6A5644, eye = 0xFF101010;
                box(vc, p, -3.5f, -10f, -1.6f, 3f, -8f, 1.6f, body, light);
                box(vc, p, -3.5f, -10f, -1.62f, 3f, -9.5f, 1.62f, back, light);
                box(vc, p, 3f, -9.6f, -1.2f, 4.2f, -8.3f, 1.2f, body, light);
                box(vc, p, -5.5f, -11f, -0.4f, -3.5f, -7.4f, 0.4f, fin, light);    // tail
                box(vc, p, 3.4f, -9.6f, -1.65f, 3.9f, -9.1f, 1.65f, eye, light);
            }
        });
        pose.popPose();

        // ---- wings on your back
        if (c.wings.value) {
            pose.pushPose();
            getParentModel().body.translateAndRotate(pose);
            float flap = (float) Math.sin(t * 0.25) * 12f + 18f;
            int col = c.wingsColor.value;
            for (int side = -1; side <= 1; side += 2) {
                pose.pushPose();
                pose.translate(side * 1.5f * P, 3 * P, 2.2f * P);
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(side * -flap));
                int sd = side;
                collector.submitCustomGeometry(pose, RenderTypes.entityCutoutNoCull(WHITE), (p, vc) -> {
                    // three rows of feathers getting longer towards the top
                    for (int i = 0; i < 4; i++) {
                        float len = 7 + i * 2.2f;
                        float y0 = 8 - i * 2.6f;
                        float x0 = sd < 0 ? -len : 0, x1 = sd < 0 ? 0 : len;
                        int shade = Wardrobe.mix(col, 0xFF000000, 0.08f * (3 - i));
                        box(vc, p, x0, y0 - 2.6f, 0, x1, y0, 0.6f, shade, light);
                    }
                });
                pose.popPose();
            }
            pose.popPose();
        }
    }

    /** A hollow square ring around the head at height y0..y1 (model pixels, up is negative). */
    private static void ring(VertexConsumer vc, PoseStack.Pose p, float lo, float y0, float y1, float hi, int col, int light) {
        float yTop = Math.min(y0, y1), yBot = Math.max(y0, y1);
        float th = 0.4f;
        box(vc, p, lo, yTop, lo, hi, yBot, lo + th, col, light);
        box(vc, p, lo, yTop, hi - th, hi, yBot, hi, col, light);
        box(vc, p, lo, yTop, lo, lo + th, yBot, hi, col, light);
        box(vc, p, hi - th, yTop, lo, hi, yBot, hi, col, light);
    }

    /** A solid box between two corners, in model pixels. */
    private static void box(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float z0, float x1, float y1, float z1, int col, int light) {
        x0 *= P; y0 *= P; z0 *= P; x1 *= P; y1 *= P; z1 *= P;
        // -Y and +Y (model y points down)
        quad(vc, p, col, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(vc, p, col, light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        quad(vc, p, col, light, 0, 0, -1, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0);
        quad(vc, p, col, light, 0, 0, 1, x1, y1, z1, x0, y1, z1, x0, y0, z1, x1, y0, z1);
        quad(vc, p, col, light, -1, 0, 0, x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1);
        quad(vc, p, col, light, 1, 0, 0, x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, int col, int light, float nx, float ny, float nz, float... v) {
        // slightly darker on the sides so shapes read in 3D
        int c = ny != 0 ? col : Wardrobe.mix(col, 0xFF000000, nx != 0 ? 0.18f : 0.1f);
        for (int i = 0; i < 4; i++) {
            vc.addVertex(p, v[i * 3], v[i * 3 + 1], v[i * 3 + 2]).setColor(c).setUv(0.5f, 0.5f)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, -ny, nz);
        }
    }
}
