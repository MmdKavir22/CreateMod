package com.chattriggers.ctjs.gui

import com.chattriggers.ctjs.Reference
import com.chattriggers.ctjs.commands.Command
import com.chattriggers.ctjs.engine.langs.js.JSLoader
import com.chattriggers.ctjs.engine.module.ModuleManager
import com.chattriggers.ctjs.engine.module.ScriptRepo
import com.chattriggers.ctjs.utils.Config
import com.chattriggers.ctjs.utils.Refl
import com.chattriggers.ctjs.utils.console.ActivityLog
import com.chattriggers.ctjs.utils.console.CTLog
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

private enum class Tab(val title: String) {
    SCRIPTS("Scripts"),
    MODULES("Modules"),
    CONSOLE("Console"),
    EVENTS("Events"),
    SETTINGS("Settings"),
    INFO("Info"),
}

private class Row(
    val text: String,
    val onClick: (() -> Unit)? = null,
    val side: String? = null,
    val onSide: (() -> Unit)? = null,
)

class ScriptGui : Screen(Component.literal("ChatTriggers")) {
    override fun init() {
        build()
    }

    fun rebuild() {
        clearWidgets()
        build()
    }

    private fun label(text: String, x: Int, y: Int, w: Int): Button {
        val button = Button.builder(Component.literal(text)) { }.bounds(x, y, w, 20).build()
        button.active = false
        return button
    }

    private fun action(text: String, x: Int, y: Int, w: Int, onPress: () -> Unit): Button {
        return Button.builder(Component.literal(text)) { onPress() }.bounds(x, y, w, 20).build()
    }

    private fun build() {
        val w = minOf(width - 20, 380)
        val x = (width - w) / 2

        val tabs = Tab.values()
        val gap = 2
        val tabWidth = (w - gap * (tabs.size - 1)) / tabs.size
        tabs.forEachIndexed { index, t ->
            val button = action(t.title, x + index * (tabWidth + gap), 6, tabWidth) {
                tab = t
                page = 0
                rebuild()
            }
            button.active = t != tab
            addRenderableWidget(button)
        }

        val rows = when (tab) {
            Tab.SCRIPTS -> scriptRows()
            Tab.MODULES -> moduleRows()
            Tab.CONSOLE -> consoleRows()
            Tab.EVENTS -> eventRows()
            Tab.SETTINGS -> settingRows()
            Tab.INFO -> infoRows()
        }

        val top = 32
        val perPage = maxOf(3, (height - top - 54) / 22)
        val pages = maxOf(1, (rows.size + perPage - 1) / perPage)
        if (page >= pages) page = pages - 1
        if (page < 0) page = 0

        rows.drop(page * perPage).take(perPage).forEachIndexed { index, row ->
            val y = top + index * 22
            val onClick = row.onClick

            if (row.side != null) {
                val mainWidth = w - 74
                addRenderableWidget(if (onClick != null) action(row.text, x, y, mainWidth) { onClick() } else label(row.text, x, y, mainWidth))
                addRenderableWidget(action(row.side, x + mainWidth + 4, y, 70) { row.onSide?.invoke() })
            } else {
                addRenderableWidget(if (onClick != null) action(row.text, x, y, w) { onClick() } else label(row.text, x, y, w))
            }
        }

        addRenderableWidget(label(status, x, height - 48, w))

        val pageWidth = w - 40 - 40 - 70 - 12
        addRenderableWidget(action("<", x, height - 24, 40) {
            page--
            rebuild()
        })
        addRenderableWidget(label("Page ${page + 1}/$pages (${rows.size} rows)", x + 44, height - 24, pageWidth))
        addRenderableWidget(action(">", x + 44 + pageWidth + 4, height - 24, 40) {
            page++
            rebuild()
        })
        addRenderableWidget(action("Close", x + w - 70, height - 24, 70) { show(null) })
    }

    private fun toggle(name: String, get: () -> Boolean, set: (Boolean) -> Unit): Row {
        return Row("$name: " + (if (get()) "§aON" else "§cOFF"), {
            set(!get())
            Config.save()
            rebuild()
        })
    }

    private fun scriptRows(): List<Row> {
        val rows = mutableListOf<Row>()
        val names = ScriptRepo.cached

        rows += Row("§bRefresh list §7(${ScriptRepo.spec()})", { loadList() })
        rows += Row("§7Change repo with: /ct repo owner/repo[@branch]")

        if (names.isEmpty()) rows += Row(if (loading) "Loading..." else "No scripts found in scripts/")

        names.forEach { name ->
            val installed = ScriptRepo.isInstalled(name)
            val text = if (installed) "§a✔ §f$name §7(click to re-import)" else "§f$name §7- click to import"
            rows += Row(text, {
                ScriptRepo.importAsync(name) {
                    status = it
                    refresh()
                }
            })
        }

        return rows
    }

    private fun moduleRows(): List<Row> {
        val rows = mutableListOf<Row>()
        val modules = ModuleManager.modulesFolder.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedBy { it.name.lowercase() }
            ?.map { ModuleManager.parseModule(it) }
            ?: emptyList()

        rows += Row("§bReload all scripts", {
            status = "Reloading scripts..."
            Reference.loadCT()
            rebuild()
        })
        rows += Row("§7${modules.size} modules in ${ModuleManager.modulesFolder.path}")

        modules.forEach { module ->
            val open = module.name in expanded
            val confirming = confirmDelete == module.name

            rows += Row(
                (if (open) "v " else "> ") + "§f${module.name} §7v${module.metadata.version ?: "?"}",
                {
                    if (open) expanded.remove(module.name) else expanded.add(module.name)
                    rebuild()
                },
                if (confirming) "§cSure?" else "Delete",
                {
                    if (confirming) {
                        module.folder.deleteRecursively()
                        confirmDelete = null
                        expanded.remove(module.name)
                        status = "Deleted ${module.name}"
                        Reference.loadCT()
                    } else {
                        confirmDelete = module.name
                    }
                    rebuild()
                },
            )

            if (open) {
                val files = module.folder.walk().filter { it.isFile }.toList()
                val meta = module.metadata
                rows += Row("§7   Entry: §f${meta.entry ?: "none"}")
                if (meta.creator != null) rows += Row("§7   Creator: §f${meta.creator}")
                if (meta.description != null) rows += Row("§7   Description: §f${meta.description}")
                if (!meta.tags.isNullOrEmpty()) rows += Row("§7   Tags: §f${meta.tags.joinToString(", ")}")
                if (!meta.requires.isNullOrEmpty()) rows += Row("§7   Requires: §f${meta.requires.joinToString(", ")}")
                rows += Row("§7   Files: §f${files.size} §7(${files.sumOf { it.length() } / 1024} KB)")
                rows += Row("§7   Path: §f${module.folder.path}")
            }
        }

        return rows
    }

    private fun consoleRows(): List<Row> {
        val rows = mutableListOf<Row>()
        val filterNames = listOf("All", "Errors/Warnings", "Events", "Scripts (no events)")

        rows += Row("§bFilter: §f${filterNames[filter]} §7(click to change)", {
            filter = (filter + 1) % filterNames.size
            page = 0
            rebuild()
        })
        rows += Row("§cClear console", {
            CTLog.clear()
            status = "Console cleared"
            rebuild()
        })

        val lines = CTLog.last(500).reversed().filter {
            when (filter) {
                1 -> "[ERROR]" in it || "[WARN]" in it
                2 -> "[EVENT]" in it
                3 -> "[EVENT]" !in it
                else -> true
            }
        }

        if (lines.isEmpty()) rows += Row("§7Nothing here yet")

        lines.forEach {
            val color = when {
                "[ERROR]" in it -> "§c"
                "[WARN]" in it -> "§e"
                "[EVENT]" in it -> "§3"
                else -> "§7"
            }
            rows += Row(color + it)
        }

        return rows
    }

    private fun eventRows(): List<Row> {
        val rows = mutableListOf<Row>()

        rows += toggle("Event log (file + console)", { Config.logEvents }, { Config.logEvents = it })
        rows += toggle("Show events live in chat", { Config.eventsInChat }, { Config.eventsInChat = it })

        ActivityLog.categories.forEach { category ->
            rows += toggle("  Category: $category", { category !in Config.disabledEventCategories }, {
                if (it) Config.disabledEventCategories.remove(category) else Config.disabledEventCategories.add(category)
            })
        }

        rows += Row("§b--- recent events (newest first) ---")

        val events = CTLog.last(500).reversed().filter { "[EVENT]" in it }.take(40)
        if (events.isEmpty()) rows += Row("§7No events yet")
        events.forEach { rows += Row("§3$it") }

        return rows
    }

    private fun settingRows(): List<Row> {
        val rows = mutableListOf<Row>()

        rows += toggle("Threaded module loading", { Config.threadedLoading }, { Config.threadedLoading = it })
        rows += toggle("Clear console on reload", { Config.clearConsoleOnLoad }, { Config.clearConsoleOnLoad = it })
        rows += toggle("Event log", { Config.logEvents }, { Config.logEvents = it })
        rows += toggle("Events in chat", { Config.eventsInChat }, { Config.eventsInChat = it })

        rows += Row("§bReload scripts", {
            status = "Reloading scripts..."
            Reference.loadCT()
            rebuild()
        })
        rows += Row("§cUnload scripts", {
            Reference.unloadCT()
            status = "Scripts unloaded"
            rebuild()
        })

        rows += Row("§7Script repo: §f${ScriptRepo.spec()}")
        rows += Row("§7Modules folder: §f${ModuleManager.modulesFolder.path}")
        rows += Row("§7Log file: §f${CTLog.file.path}")
        rows += Row("§7Settings are saved in config/ChatTriggers/settings.json")

        return rows
    }

    private fun infoRows(): List<Row> {
        val rows = mutableListOf<Row>()
        val loader = FabricLoader.getInstance()
        val mc = Minecraft.getInstance()
        val runtime = Runtime.getRuntime()
        val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024

        fun info(name: String, value: Any?) {
            rows += Row("§7$name: §f$value")
        }

        rows += Row("§b--- ChatTriggers ---")
        info("Version", Reference.MODVERSION)
        info("Scripts loaded", Reference.isLoaded)
        info("Modules installed", ModuleManager.modulesFolder.listFiles()?.count { it.isDirectory } ?: 0)

        val counts = JSLoader.triggerCounts()
        info("Registered triggers", counts.values.sum())
        counts.forEach { (type, count) -> rows += Row("§7   $type: §f$count") }

        val commands = Command.activeCommands.keys.sorted()
        info("Script commands", if (commands.isEmpty()) "none" else commands.joinToString(", ") { "/$it" })

        rows += Row("§b--- Game ---")
        info("Minecraft", loader.getModContainer("minecraft").map { it.metadata.version.friendlyString }.orElse("?"))
        info("Fabric Loader", loader.getModContainer("fabricloader").map { it.metadata.version.friendlyString }.orElse("?"))
        info("Fabric API", loader.getModContainer("fabric-api").map { it.metadata.version.friendlyString }.orElse("not found"))
        info("Mods loaded", loader.allMods.size)
        info("Java", System.getProperty("java.version"))
        info("Memory", "$usedMb / ${runtime.maxMemory() / 1024 / 1024} MB")

        val player = mc.player
        rows += Row("§b--- Player ---")
        if (player == null) {
            rows += Row("§7Not in a world")
        } else {
            fun fmt(d: Double?) = if (d == null) "?" else String.format("%.1f", d)

            info("Name", Refl.text(Refl.call(player, "getName")) ?: "?")
            info("Health", fmt(Refl.num(player, "health")))
            info("Position", "${fmt(Refl.num(player, "x"))}, ${fmt(Refl.num(player, "y"))}, ${fmt(Refl.num(player, "z"))}")
            info("Dimension", Refl.call(Refl.get(mc, "level"), "dimension")?.toString()?.substringAfterLast("/")?.trimEnd(']')?.trim() ?: "?")
        }

        return rows
    }

    companion object {
        @Volatile
        private var pending = false

        @Volatile
        private var loading = false

        @Volatile
        private var status = "ChatTriggers"

        private var tab = Tab.SCRIPTS
        private var page = 0
        private var filter = 0
        private var confirmDelete: String? = null
        private val expanded = mutableSetOf<String>()

        fun open() {
            pending = true
            loadList()
        }

        fun tick() {
            if (pending) {
                pending = false
                show(ScriptGui())
            }
        }

        private fun loadList() {
            loading = true
            status = "Loading list from ${ScriptRepo.spec()}..."
            refresh()

            ScriptRepo.listAsync { list, error ->
                loading = false
                status = if (list != null) "${list.size} scripts available" else "Failed: $error"
                refresh()
            }
        }

        private fun refresh() {
            val mc = Minecraft.getInstance()
            mc.execute {
                val screen = Refl.get(mc, "screen")
                if (screen is ScriptGui) screen.rebuild()
            }
        }

        private fun show(screen: Screen?) {
            val mc = Minecraft.getInstance()
            mc.execute {
                val onMinecraft = mc.javaClass.methods.firstOrNull { it.name == "setScreen" && it.parameterCount == 1 }
                if (onMinecraft != null) {
                    onMinecraft.invoke(mc, screen)
                } else {
                    val gui = Refl.get(mc, "gui")
                    gui?.javaClass?.methods?.firstOrNull { it.name == "setScreen" && it.parameterCount == 1 }
                        ?.invoke(gui, screen)
                }
            }
        }
    }
}
