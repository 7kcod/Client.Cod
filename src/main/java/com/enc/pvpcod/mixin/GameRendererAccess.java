package com.enc.pvpcod.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets Motion blur turn on its post effect (the game keeps setPostEffect private). */
@Mixin(GameRenderer.class)
public interface GameRendererAccess {

    @Invoker("setPostEffect")
    void pvpcod$setPostEffect(Identifier id);
}
