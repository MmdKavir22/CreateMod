package ir.mmdkavir.testmod;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ModNetworking {
    public record ManaSyncPayload(int mana) implements CustomPayload {
        public static final CustomPayload.Id<ManaSyncPayload> ID = new CustomPayload.Id<>(Identifier.of(TestMod.MOD_ID, "mana_sync"));
        public static final PacketCodec<RegistryByteBuf, ManaSyncPayload> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, ManaSyncPayload::mana, ManaSyncPayload::new);
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }
    public record HealRequestPayload() implements CustomPayload {
        public static final CustomPayload.Id<HealRequestPayload> ID = new CustomPayload.Id<>(Identifier.of(TestMod.MOD_ID, "heal_request"));
        public static final PacketCodec<RegistryByteBuf, HealRequestPayload> CODEC = PacketCodec.unit(new HealRequestPayload());
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }
    public static void register() {
        PayloadTypeRegistry.playS2C().register(ManaSyncPayload.ID, ManaSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HealRequestPayload.ID, HealRequestPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HealRequestPayload.ID, (payload, context) ->
            context.server().execute(() -> TestWandItem.tryHeal(context.player())));
    }
}
