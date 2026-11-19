package com.chattriggers.ctjs.commands

import com.chattriggers.ctjs.Reference
import com.chattriggers.ctjs.minecraft.libs.ChatLib
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
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
        )

        dispatcher.register(LiteralArgumentBuilder.literal<FabricClientCommandSource>("ct").redirect(root))
    }

    private fun getUsage() = """
        &b&m${"-".repeat(40)}
        &2/ct load &7- Reloads all of the ChatTriggers modules
        &2/ct unload &7- Unloads all of the ChatTriggers modules
    """.trimIndent()
}
