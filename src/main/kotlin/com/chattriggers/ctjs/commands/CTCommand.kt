package com.chattriggers.ctjs.commands

import com.chattriggers.ctjs.Reference
import com.chattriggers.ctjs.minecraft.libs.ChatLib
import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

object CTCommand {
    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        val root = dispatcher.register(
            ClientCommandManager.literal("chattriggers")
                .executes { ChatLib.chat(getUsage()); 1 }
                .then(ClientCommandManager.literal("load").executes { Reference.loadCT(); 1 })
                .then(ClientCommandManager.literal("reload").executes { Reference.loadCT(); 1 })
                .then(ClientCommandManager.literal("refresh").executes { Reference.loadCT(); 1 })
                .then(ClientCommandManager.literal("unload").executes { Reference.unloadCT(); 1 })
        )

        dispatcher.register(ClientCommandManager.literal("ct").redirect(root))
    }

    private fun getUsage() = """
        &b&m${"-".repeat(40)}
        &2/ct load &7- Reloads all of the ChatTriggers modules
        &2/ct unload &7- Unloads all of the ChatTriggers modules
    """.trimIndent()
}
