package ir.mmdkavir.testmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TestMod implements ModInitializer {

    public static final String MOD_ID = "testmod";

    public static final Item TEST_WAND = Registry.register(
            Registries.ITEM,
            Identifier.of(MOD_ID, "test_wand"),
            new TestWandItem(new Item.Settings().maxCount(1))
    );

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(TEST_WAND));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("testmod").executes(context -> {
                    context.getSource().sendFeedback(() -> Text.literal("TestMod is loaded and working!"), false);
                    return 1;
                }))
        );
    }
}
