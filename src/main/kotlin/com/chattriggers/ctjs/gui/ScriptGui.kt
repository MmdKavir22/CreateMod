package com.chattriggers.ctjs.gui

import com.chattriggers.ctjs.Reference
import com.chattriggers.ctjs.engine.module.ScriptRepo
import com.chattriggers.ctjs.utils.Refl
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class ScriptGui : Screen(Component.literal("ChatTriggers Scripts")) {
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
        val w = 300
        val x = (width - w) / 2
        val names = ScriptRepo.cached

        val perPage = maxOf(3, (height - 120) / 24)
        val pages = maxOf(1, (names.size + perPage - 1) / perPage)
        if (page >= pages) page = pages - 1
        if (page < 0) page = 0

        addRenderableWidget(label("ChatTriggers Scripts - ${ScriptRepo.spec()}", x, 8, w))

        if (names.isEmpty()) {
            addRenderableWidget(label(if (loading) "Loading..." else "No scripts found", x, 40, w))
        }

        names.drop(page * perPage).take(perPage).forEachIndexed { index, name ->
            val installed = ScriptRepo.isInstalled(name)
            val text = if (installed) "§a✔ §f$name §7(click to re-import)" else "§f$name §7- click to import"
            addRenderableWidget(action(text, x, 36 + index * 24, w) {
                ScriptRepo.importAsync(name) {
                    status = it
                    refresh()
                }
            })
        }

        val bottom = height - 28
        addRenderableWidget(label(status, x, bottom - 48, w))

        addRenderableWidget(action("<", x, bottom - 24, 60) {
            page--
            rebuild()
        })
        addRenderableWidget(label("Page ${page + 1}/$pages", x + 64, bottom - 24, w - 128))
        addRenderableWidget(action(">", x + w - 60, bottom - 24, 60) {
            page++
            rebuild()
        })

        addRenderableWidget(action("Refresh", x, bottom, 96) { loadList() })
        addRenderableWidget(action("Reload scripts", x + 102, bottom, 96) { Reference.loadCT() })
        addRenderableWidget(action("Close", x + 204, bottom, 96) { show(null) })
    }

    companion object {
        @Volatile
        private var pending = false

        @Volatile
        private var loading = false

        @Volatile
        private var status = "Click a script to import it"

        private var page = 0

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
