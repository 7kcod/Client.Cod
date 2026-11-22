package dev.cod.pvpcod.mixin;

import dev.cod.pvpcod.Social;
import dev.cod.pvpcod.Tracker;
import dev.cod.pvpcod.module.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops you hitting friends (if you want), and feeds Reach and Combo. */
@Mixin(MultiPlayerGameMode.class)
public abstract class AttackMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void pvpcod$attack(Player player, Entity target, CallbackInfo ci) {
        var fr = Modules.FRIENDS;
        if (fr.enabled && fr.noHit.value && target instanceof Player p && Social.isFriend(p.getGameProfile().name())) {
            ci.cancel();
            return;
        }
        Tracker.onAttack(Minecraft.getInstance(), target);
    }
}
