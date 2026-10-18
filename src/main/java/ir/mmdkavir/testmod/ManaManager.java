package ir.mmdkavir.testmod;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ManaManager {
    public static final int MAX_MANA = 100;
    public static final int REGEN_INTERVAL_TICKS = 80;
    public static final int COST_BOLT = 4;
    public static final int COST_TELEPORT = 12;
    public static final int COST_HEAL = 25;
    private static final Map<UUID, Integer> MANA = new HashMap<>();
    private static final Map<UUID, Integer> TIMERS = new HashMap<>();

    public static int get(ServerPlayerEntity player) { return MANA.getOrDefault(player.getUuid(), MAX_MANA); }
    public static boolean trySpend(ServerPlayerEntity player, int cost) {
        int current = get(player);
        if (current < cost) return false;
        MANA.put(player.getUuid(), current - cost);
        sync(player);
        return true;
    }
    public static void sync(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new ModNetworking.ManaSyncPayload(get(player)));
    }
    public static void forget(ServerPlayerEntity player) {
        MANA.remove(player.getUuid());
        TIMERS.remove(player.getUuid());
    }
    public static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID id = player.getUuid();
            int current = MANA.getOrDefault(id, MAX_MANA);
            if (current >= MAX_MANA) { TIMERS.remove(id); continue; }
            int timer = TIMERS.getOrDefault(id, 0) + 1;
            if (timer >= REGEN_INTERVAL_TICKS) {
                MANA.put(id, current + 1);
                TIMERS.put(id, 0);
                sync(player);
            } else TIMERS.put(id, timer);
        }
    }
}
