package dev.cod.pvpcod.mixin;

import dev.cod.pvpcod.Pings;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reads pings from team members out of chat (they still show in chat as coords). */
@Mixin(ChatComponent.class)
public abstract class ChatMixin {

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("HEAD"))
    private void pvpcod$ping(Component message, MessageSignature sig, GuiMessageTag tag, CallbackInfo ci) {
        try {
            Pings.onChat(message);
        } catch (RuntimeException ignored) {
        }
    }
}
