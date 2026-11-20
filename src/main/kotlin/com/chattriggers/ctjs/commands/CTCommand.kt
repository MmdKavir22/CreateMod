package com.chattriggers.ctjs.commands

import com.chattriggers.ctjs.Reference
import com.chattriggers.ctjs.minecraft.libs.ChatLib
import com.chattriggers.ctjs.utils.Config
import com.chattriggers.ctjs.utils.console.CTLog
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

object CTCommand {
    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        val root = dispatcher.register(
            LiteralArgumentBuilder.literal<FabricClientCommandSource>("chattriggers")
                .executes { ChatLib.chat(getUsage()); 1 }
                .then(LiteralArgumentBuilder.literal<FabricClientCommandSource>("load").executes { Reference.loadCT(); 1 })
                .then(LiteralArgumentBuilder.literal<FabricClientCommandSource>("reload").executes { Reference.loadCT(); 1 })
                .then(LiteralArgumentBuilder.literal<FabricClientCommandSource>("refresh").executes { Reference.loadCT(); 1 })
                .then(LiteralArgumentBuilder.literal<FabricClientCommandSource>("unload").executes { Reference.unloadCT(); 1 })
                .then(
                    LiteralArgumentBuilder.literal<FabricClientCommandSource>("console")
                        .executes { showConsole(15); 1 }
                        .then(
                            RequiredArgumentBuilder.argument<FabricClientCommandSource, Int>("lines", IntegerArgumentType.integer(1, 100))
                                .executes { ctx: CommandContext<FabricClientCommandSource> ->
                                    showConsole(IntegerArgumentType.getInteger(ctx, "lines")); 1
                                }
                        )
                )
                .then(LiteralArgumentBuilder.literal<FabricClientCommandSource>("clear").executes { CTLog.clear(); ChatLib.chat("&7Console cleared"); 1 })
                .then(LiteralArgumentBuilder.literal<FabricClientCommandSource>("log").executes { ChatLib.chat("&7Log file: &f${CTLog.file.absolutePath}"); 1 })
                .then(eventsNode())
        )

        dispatcher.register(LiteralArgumentBuilder.literal<FabricClientCommandSource>("ct").redirect(root))
    }

    private fun eventsNode(): LiteralArgumentBuilder<FabricClientCommandSource> {
        fun status() = ChatLib.chat("&7Event log: &f${if (Config.logEvents) "on" else "off"}&7, in chat: &f${if (Config.eventsInChat) "on" else "off"}")

        return LiteralArgumentBuilder.literal<FabricClientCommandSource>("events")
            .executes { status(); 1 }
            .then(LiteralArgumentBuilder.literal<FabricClientCommandSource>("on").executes { Config.logEvents = true; status(); 1 })
            .then(LiteralArgumentBuilder.literal<FabricClientCommandSource>("off").executes { Config.logEvents = false; Config.eventsInChat = false; status(); 1 })
            .then(LiteralArgumentBuilder.literal<FabricClientCommandSource>("chat").executes { Config.eventsInChat = !Config.eventsInChat; status(); 1 })
    }

    private fun showConsole(count: Int) {
        val lines = CTLog.last(count)
        if (lines.isEmpty()) {
            ChatLib.chat("&7Console is empty")
            return
        }

        ChatLib.chat("&b--- ChatTriggers console (last ${lines.size}) ---")
        lines.forEach {
            val color = when {
                "[ERROR]" in it -> "&c"
                "[WARN]" in it -> "&e"
                else -> "&7"
            }
            ChatLib.chat(color + it.replace("&", "&&"))
        }
    }

    private fun getUsage() = """
        &b&m${"-".repeat(40)}
        &2/ct load &7- Reloads all of the ChatTriggers modules
        &2/ct unload &7- Unloads all of the ChatTriggers modules
        &2/ct console [lines] &7- Shows the latest console lines in chat
        &2/ct clear &7- Clears the console
        &2/ct events [on|off|chat] &7- Logs damage, knockback, movement and more (chat = show live in chat)
        &2/ct log &7- Shows the log file path (config/ChatTriggers/ctjs.log)
    """.trimIndent()
}
