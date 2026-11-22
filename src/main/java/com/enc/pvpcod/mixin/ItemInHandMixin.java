package com.enc.pvpcod.mixin;

import com.enc.pvpcod.module.Modules;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Side shield: moves a held shield down and to the side in first person. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandMixin {

    @Unique
    private boolean pvpcod$moved;

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void pvpcod$start(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swing,
                              ItemStack stack, float equip, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        // Riptide shield fix: no shield in your face while spinning
        if (stack.is(Items.SHIELD) && Modules.RIPTIDE_SHIELD.enabled && player.isAutoSpinAttack()) {
            ci.cancel();
            return;
        }
        // Item scaling
        var is = Modules.ITEM_SCALE;
        boolean main = hand == InteractionHand.MAIN_HAND;
        if (is.enabled && !stack.isEmpty() && !(stack.is(Items.SHIELD) && Modules.SIDE_SHIELD.enabled)
                && (is.which.index == 0 || (is.which.index == 1) == main)) {
            boolean rightSide = main == (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            pose.pushPose();
            pose.translate((rightSide ? 1 : -1) * is.x.value, is.y.value, -is.z.value);
            pvpcod$scaleOrigin(pose, rightSide, is.size.value);
            pvpcod$moved = true;
            return;
        }
        var ss = Modules.SIDE_SHIELD;
        if (!ss.enabled || !stack.is(Items.SHIELD)) return;
        boolean blocking = player.isUsingItem() && player.getUsedItemHand() == hand;
        if (blocking && !ss.keepWhenBlocking.value) return;
        boolean right = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
        pose.pushPose();
        pose.translate((right ? 1 : -1) * ss.x.value, -ss.y.value, ss.z.value);
        float s = ss.size.value;
        pose.scale(s, s, s);
        pvpcod$moved = true;
    }

    /** Scales around where the item sits in your hand, so it shrinks in place instead of towards the middle. */
    @Unique
    private static void pvpcod$scaleOrigin(PoseStack pose, boolean right, float s) {
        float ox = (right ? 1 : -1) * 0.56f, oy = -0.52f, oz = -0.72f;
        pose.translate(ox, oy, oz);
        pose.scale(s, s, s);
        pose.translate(-ox, -oy, -oz);
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void pvpcod$end(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swing,
                            ItemStack stack, float equip, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (pvpcod$moved) {
            pose.popPose();
            pvpcod$moved = false;
        }
    }

    /** Boat map visibility: don't lower your hands (and your map) while rowing a boat. */
    @org.spongepowered.asm.mixin.injection.Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z"))
    private boolean pvpcod$boatMap(net.minecraft.client.player.LocalPlayer player) {
        if (Modules.BOAT_MAP.enabled && (player.getMainHandItem().is(Items.FILLED_MAP) || player.getOffhandItem().is(Items.FILLED_MAP))) return false;
        return player.isHandsBusy();
    }
}
