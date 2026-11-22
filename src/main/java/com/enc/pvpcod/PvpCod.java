package com.enc.pvpcod;

import com.enc.pvpcod.module.Module;
import com.enc.pvpcod.module.Modules;
import com.enc.pvpcod.ui.MenuScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

/**
 * pvp.cod: a PvP client as a Fabric mod, with pack.cod built in.
 *
 * Ways in: Right Shift in game, the cod on the title screen, or the
 * pvp.cod button on the pause menu.
 */
public final class PvpCod implements ClientModInitializer {

    public static final String VERSION = FabricLoader.getInstance().getModContainer("pvpcod")
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");

    public static KeyMapping menuKey, zoomKey, friendKey, hudKey;

    @Override
    public void onInitializeClient() {
        Modules.load();
        Social.loadFriends();
        Social.loadTeam();
        Wardrobe.ensureFolders();

        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("pvpcod", "main"));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.pvpcod.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.pvpcod.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, cat));
        friendKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.pvpcod.friend", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_MIDDLE, cat));
        hudKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.pvpcod.hud", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), cat));

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("pvpcod", "hud"), (g, delta) -> Hud.render(g));
        // Team pings sit under the HUD boxes (drawn before them).
        HudElementRegistry.addFirst(Identifier.fromNamespaceAndPath("pvpcod", "pings"), (g, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (!mc.options.hideGui) Pings.render(g, mc);
        });

        // Cosmetics on your player, and the shulker box grid tooltip.
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (renderer instanceof AvatarRenderer<?> avatar) {
                @SuppressWarnings("unchecked")
                var parent = (net.minecraft.client.renderer.entity.RenderLayerParent<net.minecraft.client.renderer.entity.state.AvatarRenderState, net.minecraft.client.model.player.PlayerModel>) (Object) avatar;
                helper.register(new CosmeticsLayer(parent));
                helper.register(new ClothCapeLayer(parent));
            }
        });
        // Cloth capes replace the normal stiff cape.
        LivingEntityFeatureRenderEvents.ALLOW_CAPE_RENDER.register(state -> !ClothCapeLayer.active(state));
        TooltipComponentCallback.EVENT.register(data -> data instanceof ShulkerTooltip.Data d ? new ShulkerTooltip.Client(d) : null);
        // Custom crosshair and Clean HUD swap out the game's own pieces.
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, old -> (g, delta) -> {
            var ch = Modules.CROSSHAIR;
            Minecraft mc = Minecraft.getInstance();
            if (!ch.enabled || mc.screen instanceof com.enc.pvpcod.ui.HudEditorScreen) {
                old.render(g, delta);
                return;
            }
            if (!mc.options.getCameraType().isFirstPerson() || mc.debugEntries.isOverlayVisible()) return;
            ch.draw(g, g.guiWidth() / 2, g.guiHeight() / 2, mc.crosshairPickEntity instanceof Player);
        });
        HudElementRegistry.replaceElement(VanillaHudElements.BOSS_BAR, old -> (g, delta) -> {
            if (!(Modules.CLEAN_HUD.enabled && Modules.CLEAN_HUD.bossBar.value)) old.render(g, delta);
        });
        HudElementRegistry.replaceElement(VanillaHudElements.SCOREBOARD, old -> (g, delta) -> {
            if (!(Modules.CLEAN_HUD.enabled && Modules.CLEAN_HUD.scoreboard.value)) old.render(g, delta);
        });
        HudElementRegistry.replaceElement(VanillaHudElements.STATUS_EFFECTS, old -> (g, delta) -> {
            if (!(Modules.CLEAN_HUD.enabled && Modules.CLEAN_HUD.effects.value)) old.render(g, delta);
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Tracker.tick(mc);
            for (Module m : Modules.ALL) {
                m.alwaysTick(mc);
                if (m.enabled) m.tick(mc);
            }
            while (menuKey.consumeClick()) if (mc.screen == null) mc.setScreen(new MenuScreen(null));
            while (hudKey.consumeClick()) if (mc.screen == null) mc.setScreen(new com.enc.pvpcod.ui.HudEditorScreen(null));
            while (friendKey.consumeClick()) {
                if (mc.screen != null || mc.player == null) continue;
                // In survival with Team pings on, middle click pings (sneak + middle click still adds friends).
                if (Modules.PINGS.active() && !mc.player.isShiftKeyDown()) {
                    Pings.ping(mc);
                    continue;
                }
                // Middle-click a player to add or remove them as a friend.
                if (mc.hitResult instanceof EntityHitResult eh && eh.getEntity() instanceof Player p) {
                    String name = p.getGameProfile().name();
                    boolean now = Social.toggleFriend(name);
                    mc.player.displayClientMessage(Component.literal(now ? "Added " + name + " as a friend" : "Removed " + name + " from friends")
                            .withColor(now ? 0x41D97A : 0xD9614C), true);
                }
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> Modules.save());

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
                // The bobbing cod, top left, on the title screen and the pause menu.
                Button b = Button.builder(Component.empty(), btn -> client.setScreen(new MenuScreen(screen)))
                        .bounds(6, 6, 20, 20)
                        .tooltip(Tooltip.create(Component.literal("pvp.cod (pack.cod is inside)")))
                        .build();
                b.setAlpha(0f);
                Screens.getButtons(screen).add(b);
                ItemStack cod = new ItemStack(Items.COD);
                ScreenEvents.afterRender(screen).register((s, g, mouseX, mouseY, delta) -> {
                    double t = System.currentTimeMillis() / 1000.0;
                    boolean hover = b.isHoveredOrFocused();
                    float bob = (float) Math.sin(t * 2.2) * 1.5f - (hover ? 2f : 0f);
                    float sc = hover ? 1.15f : 1f;
                    g.pose().pushMatrix();
                    g.pose().translate(b.getX() + 10f, b.getY() + 10f + bob);
                    g.pose().rotate((float) Math.toRadians(Math.sin(t * 1.6) * 6));
                    g.pose().scale(sc, sc);
                    g.renderItem(cod, -8, -8);
                    g.pose().popMatrix();
                });
            }
        });
    }

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }
}
