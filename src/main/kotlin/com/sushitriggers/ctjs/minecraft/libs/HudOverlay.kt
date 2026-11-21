package com.sushitriggers.ctjs.minecraft.libs

import com.sushitriggers.ctjs.utils.Refl
import com.sushitriggers.ctjs.utils.console.CTLog
import com.sushitriggers.ctjs.utils.console.LogType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap

object HudOverlay {
    private class Entry(val text: String, val x: Int, val y: Int, val color: Int)

    private class Panel(val title: String, val titleColor: Int, val lines: List<String>, val colors: List<Int>, val x: Int, val y: Int)

    private val panels = ConcurrentHashMap<String, Panel>()
    private val entries = ConcurrentHashMap<String, Entry>()
    private var drawCalls = 0
    private var error: String? = null
    private var registered = false

    @JvmStatic
    fun set(id: String, text: String, x: Int, y: Int) = setColor(id, text, x, y, -1)

    @JvmStatic
    fun setColor(id: String, text: String, x: Int, y: Int, color: Int) {
        entries[id] = Entry(text, x, y, color)
    }

    @JvmStatic
    fun panel(id: String, title: String, titleColor: Int, lines: Array<String>, colors: IntArray, x: Int, y: Int) {
        panels[id] = Panel(title, titleColor, lines.toList(), colors.toList(), x, y)
    }

    @JvmStatic
    fun remove(id: String) {
        entries.remove(id)
        panels.remove(id)
    }

    @JvmStatic
    fun clear() {
        entries.clear()
        panels.clear()
    }

    @JvmStatic
    fun isRegistered() = registered

    @JvmStatic
    fun status(): String = "registered=$registered drawCalls=$drawCalls error=${error ?: "none"}"

    @JvmStatic
    fun width(): Int = size("getGuiScaledWidth", "guiScaledWidth")

    @JvmStatic
    fun height(): Int = size("getGuiScaledHeight", "guiScaledHeight")

    @JvmStatic
    fun textWidth(text: String): Int = try {
        val font = Minecraft.getInstance().font
        val method = font.javaClass.methods.first { it.name == "width" && it.parameterCount == 1 && it.parameterTypes[0] == String::class.java }
        (method.invoke(font, text) as Number).toInt()
    } catch (e: Throwable) {
        text.length * 6
    }

    private fun size(vararg names: String): Int {
        val window = Refl.call(Minecraft.getInstance(), "getWindow") ?: return 0
        return (Refl.call(window, *names) as? Number)?.toInt() ?: 0
    }

    private fun draw(graphics: GuiGraphicsExtractor) {
        drawCalls++
        if (entries.isEmpty() && panels.isEmpty()) return
        val font = Minecraft.getInstance().font

        panels.values.forEach { panel ->
            var width = textWidth(panel.title)
            panel.lines.forEach { width = maxOf(width, textWidth(it)) }
            width += 12
            val height = 12 + 11 + panel.lines.size * 11 + 3

            graphics.fill(panel.x, panel.y, panel.x + width, panel.y + height, 0x99000000.toInt())
            graphics.outline(panel.x, panel.y, width, height, 0xCC8A2BE2.toInt())
            graphics.text(font, panel.title, panel.x + 6, panel.y + 5, panel.titleColor or (0xFF shl 24), true)
            panel.lines.forEachIndexed { i, line ->
                val color = panel.colors.getOrElse(i) { 0xFFFFFF }
                graphics.text(font, line, panel.x + 6, panel.y + 18 + i * 11, color or (0xFF shl 24), true)
            }
        }
        entries.values.forEach {
            graphics.text(font, it.text, it.x, it.y, it.color or (0xFF shl 24), true)
        }
    }

    fun register() {
        if (registered) return

        try {
            val registry = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry")
            val vanilla = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements")
            val elementClass = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement")

            val idClass = listOf("net.minecraft.resources.Identifier", "net.minecraft.resources.ResourceLocation")
                .firstNotNullOf { runCatching { Class.forName(it) }.getOrNull() }
            val factory = idClass.methods.first {
                java.lang.reflect.Modifier.isStatic(it.modifiers) && it.parameterCount == 2 &&
                        it.parameterTypes.all { p -> p == String::class.java } &&
                        it.name in listOf("fromNamespaceAndPath", "of")
            }
            val id = factory.invoke(null, "sushitriggers", "script_hud")
            val chat = vanilla.getField("CHAT").get(null)

            val element = Proxy.newProxyInstance(elementClass.classLoader, arrayOf(elementClass)) { proxy, method, args ->
                when {
                    method.declaringClass == Any::class.java -> when (method.name) {
                        "hashCode" -> System.identityHashCode(proxy)
                        "equals" -> proxy === args?.firstOrNull()
                        else -> "ScriptHud"
                    }
                    args != null && args.isNotEmpty() && args[0] is GuiGraphicsExtractor -> {
                        try {
                            draw(args[0] as GuiGraphicsExtractor)
                        } catch (e: Throwable) {
                        }
                        null
                    }
                    else -> null
                }
            }

            val attach = registry.methods.firstOrNull { it.name == "attachElementBefore" && it.parameterCount == 3 }
                ?: registry.methods.first { it.name == "attachElementAfter" && it.parameterCount == 3 }
            attach.invoke(null, chat, id, element)
            registered = true
            CTLog.add(LogType.INFO, "HUD overlay registered")
        } catch (e: Throwable) {
            error = e.toString()
            CTLog.add(LogType.WARN, "HUD overlay not available: $e")
        }
    }
}
