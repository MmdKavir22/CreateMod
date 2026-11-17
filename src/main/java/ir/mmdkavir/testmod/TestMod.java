package ir.mmdkavir.testmod;

import java.util.function.Function;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entrypoint of Test Mod: registers the item, the creative tab entry and the command. */
public class TestMod implements ModInitializer {
	public static final String MOD_ID = "testmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Registry key of the Test Item (testmod:test_item). */
	public static final ResourceKey<Item> TEST_ITEM_KEY =
			ResourceKey.create(Registries.ITEM, id("test_item"));

	/** The Test Item itself. */
	public static final Item TEST_ITEM = registerItem(TEST_ITEM_KEY, Item::new, new Item.Properties());

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	private static Item registerItem(ResourceKey<Item> key, Function<Item.Properties, Item> factory,
			Item.Properties properties) {
		// Since 1.21.2 an item must know its own registry key before it is created.
		Item item = factory.apply(properties.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	@Override
	public void onInitialize() {
		// Show the item in the vanilla "Ingredients" creative tab.
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
				.register(output -> output.accept(TEST_ITEM));

		CommandRegistrationCallback.EVENT.register(
				(dispatcher, buildContext, selection) -> TestModCommands.register(dispatcher));

		String version = FabricLoader.getInstance().getModContainer(MOD_ID)
				.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
		LOGGER.info("Test Mod v{} initialized successfully!", version);
	}
}
