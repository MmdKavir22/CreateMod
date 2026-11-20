package com.chattriggers.ctjs.mixin;

import com.chattriggers.ctjs.minecraft.listeners.PacketHooks;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true)
    private void ctjs$onVelocityPacket(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        if (PacketHooks.onPacketReceived(packet)) {
            ci.cancel();
        }
    }
}
