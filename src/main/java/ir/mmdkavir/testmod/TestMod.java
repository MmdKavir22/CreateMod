package ir.mmdkavir.testmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TestMod implements ModInitializer {
    public static final String MOD_ID = "testmod";
    public static final Item TEST_WAND = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "test_wand"), new TestWandItem(new Item.Settings().maxCount(1)));
    public static final Item WAND_BOLT_ITEM = Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "wand_bolt"), new Item(new Item.Settings()));
    public static final EntityType<WandBoltEntity> WAND_BOLT_ENTITY = Registry.register(Registries.ENTITY_TYPE, Identifier.of(MOD_ID, "wand_bolt"),
        EntityType.Builder.<WandBoltEntity>create(WandBoltEntity::new, SpawnGroup.MISC).dimensions(0.25f, 0.25f).maxTrackingRange(4).trackingTickInterval(10).build("wand_bolt"));
    @Override public void onInitialize() {
        ModNetworking.register();
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(TEST_WAND));
        ServerTickEvents.END_SERVER_TICK.register(ManaManager::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ManaManager.sync(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ManaManager.forget(handler.player));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(CommandManager.literal("testmod").executes(context -> {
                context.getSource().sendFeedback(() -> Text.literal("TestMod is loaded and working!"), false);
                return 1;
            })));
    }
}
