package ir.mmdkavir.testmod;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Registers the /testmod command. */
public final class TestModCommands {
	private TestModCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("testmod").executes(context -> {
			String modVersion = versionOf(TestMod.MOD_ID);
			String mcVersion = versionOf("minecraft");
			context.getSource().sendSuccess(() -> Component.literal(
					"Test Mod is loaded correctly!\nMod version: " + modVersion
							+ "\nMinecraft version: " + mcVersion), false);
			return 1;
		}));
	}

	private static String versionOf(String modId) {
		return FabricLoader.getInstance().getModContainer(modId)
				.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
	}
}
