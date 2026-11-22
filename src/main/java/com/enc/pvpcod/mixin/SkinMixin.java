package com.enc.pvpcod.mixin;

import com.enc.pvpcod.OtherCapes;
import com.enc.pvpcod.Wardrobe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skin and cape swapper for you; other clients' capes for players with no Minecraft cape. */
@Mixin(AbstractClientPlayer.class)
public abstract class SkinMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void pvpcod$skin(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        Minecraft mc = Minecraft.getInstance();
        PlayerSkin skin = cir.getReturnValue();
        if (mc.getUser() != null && self.getUUID().equals(mc.getUser().getProfileId())) {
            skin = Wardrobe.apply(skin);
        }
        if (skin.cape() == null) {
            Identifier cape = OtherCapes.get(self.getUUID(), self.getGameProfile().name());
            if (cape != null) {
                var tex = new Wardrobe.Tex(cape);
                skin = new PlayerSkin(skin.body(), tex, skin.elytra() == null ? tex : skin.elytra(), skin.model(), skin.secure());
            }
        }
        cir.setReturnValue(skin);
    }
}
