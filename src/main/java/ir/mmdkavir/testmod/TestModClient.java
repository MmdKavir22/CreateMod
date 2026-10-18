package ir.mmdkavir.testmod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class TestModClient implements ClientModInitializer {
    private static final Identifier ICON_FULL = Identifier.of(TestMod.MOD_ID, "textures/gui/mana_full.png");
    private static final Identifier ICON_HALF = Identifier.of(TestMod.MOD_ID, "textures/gui/mana_half.png");
    private static final Identifier ICON_EMPTY = Identifier.of(TestMod.MOD_ID, "textures/gui/mana_empty.png");
    private static int clientMana = ManaManager.MAX_MANA;
    private static KeyBinding healKey;

    @Override public void onInitializeClient() {
        healKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.testmod.heal", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.testmod"));
        EntityRendererRegistry.register(TestMod.WAND_BOLT_ENTITY, FlyingItemEntityRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.ManaSyncPayload.ID, (payload, context) ->
            context.client().execute(() -> clientMana = payload.mana()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (healKey.wasPressed()) {
                if (client.player != null && TestWandItem.isHoldingWand(client.player))
                    ClientPlayNetworking.send(new ModNetworking.HealRequestPayload());
            }
        });
        HudRenderCallback.EVENT.register(TestModClient::renderMana);
    }
    private static void renderMana(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || client.options.hudHidden || player.isSpectator()) return;
        int width = context.getScaledWindowWidth(), height = context.getScaledWindowHeight();
        boolean bars = client.interactionManager != null && client.interactionManager.hasStatusBars();
        int y = bars ? height - 49 : height - 39;
        if (bars && (player.isSubmergedIn(FluidTags.WATER) || player.getAir() < player.getMaxAir())) y -= 10;
        int startX = width / 2 + 82;
        RenderSystem.enableBlend();
        for (int i = 0; i < 10; i++) {
            int remaining = clientMana - i * 10;
            Identifier icon = remaining >= 10 ? ICON_FULL : (remaining >= 5 ? ICON_HALF : ICON_EMPTY);
            context.drawTexture(icon, startX - i * 8, y, 0, 0, 9, 9, 9, 9);
        }
        RenderSystem.disableBlend();
    }
}
