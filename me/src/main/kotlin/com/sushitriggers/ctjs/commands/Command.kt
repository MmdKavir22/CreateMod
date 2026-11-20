package com.sushitriggers.ctjs.commands

import com.sushitriggers.ctjs.printToConsole
import com.sushitriggers.ctjs.triggers.Trigger
import com.sushitriggers.ctjs.utils.console.LogType
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

class Command(
    private val trigger: Trigger,
    private val name: String,
    private val usage: String,
    private val tabCompletionOptions: MutableList<String>,
    private var aliases: MutableList<String>,
    private val overrideExisting: Boolean = false,
    private val callback: ((Array<out String>) -> MutableList<String>)? = null,
) {
    fun getTriggers() = listOf(trigger)

    fun run(args: Array<String>) = trigger.trigger(args)

    fun complete(args: Array<String>): List<String> {
        return callback?.invoke(args)?.toList() ?: tabCompletionOptions
    }

    fun register() {
        if (name in activeCommands && !overrideExisting) {
            ("Command with name $name already exists! " +
                    "This will not override the other command with the same name. " +
                    "To override conflicting commands, " +
                    "set the 2nd argument in setName() to true.").printToConsole(trigger.loader.console, LogType.WARN)

            return
        }

        activeCommands[name] = this
        aliases.forEach { activeCommands[it] = this }
        (listOf(name) + aliases).forEach(::ensureNode)
    }

    fun unregister() {
        activeCommands.remove(name)
        aliases.forEach { activeCommands.remove(it) }
    }

    companion object {
        internal val activeCommands = mutableMapOf<String, Command>()

        private var dispatcher: CommandDispatcher<FabricClientCommandSource>? = null
        private val registeredNodes = mutableSetOf<String>()

        internal fun attach(newDispatcher: CommandDispatcher<FabricClientCommandSource>) {
            dispatcher = newDispatcher
            registeredNodes.clear()
            activeCommands.keys.toList().forEach(::ensureNode)
        }

        private fun ensureNode(commandName: String) {
            val d = dispatcher ?: return
            if (!registeredNodes.add(commandName)) return

            fun exec(ctx: CommandContext<FabricClientCommandSource>, withArgs: Boolean): Int {
                val raw = if (withArgs) StringArgumentType.getString(ctx, "args") else ""
                val args = raw.split(" ").filter { it.isNotEmpty() }.toTypedArray()
                activeCommands[commandName]?.run(args)
                return 1
            }

            d.register(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>(commandName)
                    .executes { exec(it, false) }
                    .then(
                        RequiredArgumentBuilder.argument<FabricClientCommandSource, String>("args", StringArgumentType.greedyString())
                            .suggests { ctx, builder ->
                                val args = ctx.input.substringAfter(' ', "").split(" ").toTypedArray()
                                activeCommands[commandName]?.complete(args)?.forEach(builder::suggest)
                                builder.buildFuture()
                            }
                            .executes { exec(it, true) }
                    )
            )
        }
    }
}
