package com.sushitriggers.ctjs.commands

import com.sushitriggers.ctjs.Reference
import com.sushitriggers.ctjs.minecraft.libs.ChatLib
import com.sushitriggers.ctjs.utils.Config
import com.sushitriggers.ctjs.utils.console.CTLog
import com.mojang.brigadier.CommandDispatcher
import com.sushitriggers.ctjs.engine.module.ScriptRepo
import com.sushitriggers.ctjs.gui.ScriptGui
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

object CTCommand {
    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        val root = dispatcher.register(
            LiteralArgumentBuilder.literal<FabricClientCommandSource>("sushitriggers")
                .executes { ScriptGui.open(); 1 }
                .then(LiteralArgumentBuilder.literal<FabricClientCommandSource>("help").executes { ChatLib.chat(getUsage()); 1 })
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
                .then(LiteralArgumentBuilder.literal<FabricClientCommandSource>("dump").executes {
                    val f = CTLog.dump()
                    ChatLib.chat(if (f != null) "&aLog dumped: &f${f.path}" else "&cDump failed")
                    1
                })
                .then(recordNode())
                .then(eventsNode())
                .then(guiNode())
                .then(scriptsNode())
                .then(importNode())
                .then(repoNode())
                .then(tokenNode())
                .then(uploadNode())
        )

        dispatcher.register(
            LiteralArgumentBuilder.literal<FabricClientCommandSource>("ct")
                .executes { ScriptGui.open(); 1 }
                .redirect(root)
        )
    }

    private fun eventsNode(): LiteralArgumentBuilder<FabricClientCommandSource> {
        fun status() = ChatLib.chat("&7Event log: &f${if (Config.logEvents) "on" else "off"}&7, in chat: &f${if (Config.eventsInChat) "on" else "off"}")

        return LiteralArgumentBuilder.literal<FabricClientCommandSource>("events")
            .executes { status(); 1 }
            .then(LiteralArgumentBuilder.literal<FabricClientCommandSource>("on").executes { Config.logEvents = true; status(); 1 })
            .then(LiteralArgumentBuilder.literal<FabricClientCommandSource>("off").executes { Config.logEvents = false; Config.eventsInChat = false; status(); 1 })
            .then(LiteralArgumentBuilder.literal<FabricClientCommandSource>("chat").executes { Config.eventsInChat = !Config.eventsInChat; status(); 1 })
    }

    private fun recordNode() = LiteralArgumentBuilder.literal<FabricClientCommandSource>("record")
        .executes { com.sushitriggers.ctjs.utils.console.Recorder.toggle(); 1 }
        .then(
            LiteralArgumentBuilder.literal<FabricClientCommandSource>("key").then(
                RequiredArgumentBuilder.argument<FabricClientCommandSource, String>("key", StringArgumentType.word())
                    .executes { ctx: CommandContext<FabricClientCommandSource> ->
                        val code = com.sushitriggers.ctjs.utils.console.Recorder.parseKey(StringArgumentType.getString(ctx, "key"))
                        if (code == null) ChatLib.chat("&cUse a letter, digit or F1-F12") else {
                            Config.recordKey = code
                            Config.save()
                            ChatLib.chat("&aRecord key: &f${com.sushitriggers.ctjs.utils.console.Recorder.keyName(code)}")
                        }
                        1
                    }
            )
        )

    private fun tokenNode() = LiteralArgumentBuilder.literal<FabricClientCommandSource>("token")
        .executes { ChatLib.chat("&7GitHub token: &f${if (ScriptRepo.hasToken()) "set" else "not set"}"); 1 }
        .then(
            RequiredArgumentBuilder.argument<FabricClientCommandSource, String>("token", StringArgumentType.greedyString())
                .executes { ctx: CommandContext<FabricClientCommandSource> ->
                    ScriptRepo.setToken(StringArgumentType.getString(ctx, "token"))
                    ChatLib.chat("&aGitHub token saved")
                    1
                }
        )

    private fun uploadNode() = LiteralArgumentBuilder.literal<FabricClientCommandSource>("upload")
        .then(
            RequiredArgumentBuilder.argument<FabricClientCommandSource, String>("name", StringArgumentType.word())
                .executes { ctx: CommandContext<FabricClientCommandSource> ->
                    ScriptRepo.uploadAsync(StringArgumentType.getString(ctx, "name"))
                    1
                }
        )

    private fun guiNode() = LiteralArgumentBuilder.literal<FabricClientCommandSource>("gui")
        .executes { ScriptGui.open(); 1 }

    private fun scriptsNode() = LiteralArgumentBuilder.literal<FabricClientCommandSource>("scripts")
        .executes {
            ChatLib.chat("&7Loading script list from ${ScriptRepo.spec()}...")
            ScriptRepo.listAsync { list, error ->
                if (list == null) ChatLib.chat("&c[CT] Failed to load list: $error")
                else if (list.isEmpty()) ChatLib.chat("&e[CT] No scripts found in scripts/ of ${ScriptRepo.spec()}")
                else {
                    ChatLib.chat("&b--- Available scripts (${list.size}) ---")
                    list.forEach { ChatLib.chat((if (ScriptRepo.isInstalled(it)) "&a\u2714 &f" else "&7- &f") + it) }
                }
            }
            1
        }

    private fun importNode() = LiteralArgumentBuilder.literal<FabricClientCommandSource>("import")
        .then(
            RequiredArgumentBuilder.argument<FabricClientCommandSource, String>("name", StringArgumentType.word())
                .suggests { _, builder ->
                    ScriptRepo.cached.forEach(builder::suggest)
                    builder.buildFuture()
                }
                .executes { ctx: CommandContext<FabricClientCommandSource> ->
                    ScriptRepo.importAsync(StringArgumentType.getString(ctx, "name"))
                    1
                }
        )

    private fun repoNode() = LiteralArgumentBuilder.literal<FabricClientCommandSource>("repo")
        .executes { ChatLib.chat("&7Script repo: &f${ScriptRepo.spec()}"); 1 }
        .then(
            RequiredArgumentBuilder.argument<FabricClientCommandSource, String>("repo", StringArgumentType.greedyString())
                .executes { ctx: CommandContext<FabricClientCommandSource> ->
                    ScriptRepo.setSpec(StringArgumentType.getString(ctx, "repo"))
                    ChatLib.chat("&7Script repo set to: &f${ScriptRepo.spec()}")
                    1
                }
        )

    private fun showConsole(count: Int) {
        val lines = CTLog.last(count)
        if (lines.isEmpty()) {
            ChatLib.chat("&7Console is empty")
            return
        }

        ChatLib.chat("&b--- SushiTriggers console (last ${lines.size}) ---")
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
        &2/ct &7- Opens the GUI
        &2/ct load &7- Reloads all of the SushiTriggers modules
        &2/ct unload &7- Unloads all of the SushiTriggers modules
        &2/ct console [lines] &7- Shows the latest console lines in chat
        &2/ct clear &7- Clears the console
        &2/ct token <github token> &7- Saves the token used for uploads
        &2/ct upload <name> &7- Uploads modules/<name> to scripts/<name>/ on GitHub
        &2/ct gui &7- Opens the script manager GUI
        &2/ct scripts &7- Lists scripts in the GitHub repo (scripts/<name>/)
        &2/ct import <name> &7- Imports scripts/<name>/ from GitHub into modules
        &2/ct repo [owner/repo[@branch]] &7- Shows or sets the GitHub repo
        &2/ct events [on|off|chat] &7- Logs damage, knockback, movement and more (chat = show live in chat)
        &2/ct dump &7- Saves the current log to config/SushiTriggers/dumps/
        &2/ct record [key <F8|R>] &7- Starts/stops recording the log (also by key, default F8)
        &2/ct log &7- Shows the log file path (config/SushiTriggers/ctjs.log)
    """.trimIndent()
}
