package dev.cod.pvpcod.mixin;

import dev.cod.pvpcod.Social;
import dev.cod.pvpcod.module.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hitbox colours: your box colour, a different one for what you're looking at and for friends. */
@Mixin(EntityHitboxDebugRenderer.class)
public abstract class HitboxMixin {

    @Unique
    private static Entity pvpcod$entity;

    @Inject(method = "showHitboxes", at = @At("HEAD"))
    private void pvpcod$remember(Entity entity, float partialTick, boolean server, CallbackInfo ci) {
        pvpcod$entity = entity;
    }

    /** The main box (white normally, green for the server-side box). */
    @ModifyArg(method = "showHitboxes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/gizmos/GizmoStyle;stroke(I)Lnet/minecraft/gizmos/GizmoStyle;", ordinal = 0))
    private int pvpcod$box(int color) {
        var hb = Modules.HITBOXES;
        if (color != -1) return color;
        Entity e = pvpcod$entity;
        if (e instanceof Player p && Social.isFriend(p.getGameProfile().name())) return hb.friend.value;
        if (e != null && e == Minecraft.getInstance().crosshairPickEntity) return hb.target.value;
        return hb.box.value;
    }

    /** The eye-height line. */
    @ModifyArg(method = "showHitboxes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/gizmos/GizmoStyle;stroke(I)Lnet/minecraft/gizmos/GizmoStyle;", ordinal = 2))
    private int pvpcod$eye(int color) {
        return Modules.HITBOXES.eye.value;
    }

    /** The blue look-direction arrow. */
    @ModifyArg(method = "showHitboxes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/gizmos/Gizmos;arrow(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;I)Lnet/minecraft/gizmos/GizmoProperties;", ordinal = 0), index = 2)
    private int pvpcod$arrow(int color) {
        return Modules.HITBOXES.look.value ? color : 0;
    }
}
