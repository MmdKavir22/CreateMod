package com.sushitriggers.ctjs.gui

import com.sushitriggers.ctjs.minecraft.libs.GameUtil
import com.sushitriggers.ctjs.minecraft.libs.HudOverlay
import com.sushitriggers.ctjs.utils.Refl
import org.lwjgl.glfw.GLFW
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.util.function.Consumer
import java.util.function.Function

/**
 * A simple menu scripts can open:
 * Menu.open("Title", ["a", "b"], function (id) { ...clicked... }, function (id) { return "label of id"; })
 */
object ScriptMenu {
    private class Request(
        val title: String,
        val ids: List<String>,
        val handler: Consumer<String>,
        val label: Function<String, String>,
    )

    @Volatile
    private var pending: Request? = null

    @JvmStatic
    fun open(title: String, ids: Array<String>, handler: Consumer<String>, label: Function<String, String>) {
        pending = Request(title, ids.toList(), handler, label)
    }

    private class MoveRequest(val id: String, val callback: java.util.function.BiConsumer<Int, Int>)

    @Volatile
    private var pendingMove: MoveRequest? = null

    fun openMover(id: String, callback: java.util.function.BiConsumer<Int, Int>) {
        pendingMove = MoveRequest(id, callback)
    }

    fun tick() {
        val move = pendingMove
        if (move != null) {
            pendingMove = null
            GameUtil.setScreen(MoveScreen(move.id, move.callback))
            return
        }

        val request = pending ?: return
        pending = null
        GameUtil.setScreen(MenuScreen(request.title, request.ids, request.handler, request.label))
    }

    private class MenuScreen(
        private val menuTitle: String,
        private val ids: List<String>,
        private val handler: Consumer<String>,
        private val label: Function<String, String>,
    ) : Screen(Component.literal(menuTitle)) {

        private var panelLeft = 0
        private var panelRight = 0
        private var panelTop = 0
        private var panelBottom = 0

        override fun init() {
            build()
        }

        private fun build() {
            clearWidgets()

            val w = 230
            val x = (width - w) / 2
            val rowHeight = 24
            val total = ids.size * rowHeight + 64
            val top = maxOf(8, (height - total) / 2)

            panelLeft = x - 12
            panelRight = x + w + 12
            panelTop = top
            panelBottom = top + total

            ids.forEachIndexed { i, id ->
                val text = try {
                    label.apply(id)
                } catch (e: Throwable) {
                    id
                }
                addRenderableWidget(
                    Button.builder(Component.literal(text)) {
                        try {
                            handler.accept(id)
                        } catch (e: Throwable) {
                        }
                        build()
                    }.bounds(x, top + 30 + i * rowHeight, w, 20).build()
                )
            }

            addRenderableWidget(
                Button.builder(Component.literal("Close")) { GameUtil.setScreen(null) }
                    .bounds(x, top + 34 + ids.size * rowHeight, w, 20).build()
            )
        }

        override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        }

        override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
            graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0x66140A26)
            graphics.outline(panelLeft, panelTop, panelRight - panelLeft, panelBottom - panelTop, 0x88D9B8FF.toInt())
            graphics.fill(panelLeft + 1, panelTop + 1, panelRight - 1, panelTop + 24, 0x44FFFFFF)
            graphics.text(font, menuTitle, panelLeft + 8, panelTop + 8, 0xFFF0DCFF.toInt(), true)

            super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        }
    }

    private class MoveScreen(
        private val id: String,
        private val onDone: java.util.function.BiConsumer<Int, Int>,
    ) : Screen(Component.literal("Move")) {

        private var dragging = false
        private var offsetX = 0
        private var offsetY = 0
        private var wasDown = false

        override fun init() {
            addRenderableWidget(
                Button.builder(Component.literal("Done")) { GameUtil.setScreen(null) }
                    .bounds(width / 2 - 50, height - 30, 100, 20).build()
            )
        }

        private fun mouseDown(): Boolean {
            return try {
                val window = Refl.call(GameUtil.mc, "getWindow") ?: return false
                val handle = (Refl.call(window, "handle", "getWindow") as? Long) ?: return false
                GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
            } catch (e: Throwable) {
                false
            }
        }

        override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        }

        override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
            val down = mouseDown()
            HudOverlay.moveLockId = id
            HudOverlay.moveLockTime = System.currentTimeMillis()
            val b = HudOverlay.bounds(id)

            if (b != null) {
                if (down && !wasDown && mouseX >= b[0] && mouseX <= b[0] + b[2] && mouseY >= b[1] && mouseY <= b[1] + b[3]) {
                    dragging = true
                    offsetX = mouseX - b[0]
                    offsetY = mouseY - b[1]
                }
                if (dragging && down) {
                    HudOverlay.move(id, maxOf(0, mouseX - offsetX), maxOf(0, mouseY - offsetY))
                }
                if (dragging && !down) {
                    dragging = false
                    val now = HudOverlay.bounds(id)
                    if (now != null) {
                        try {
                            onDone.accept(now[0], now[1])
                        } catch (e: Throwable) {
                        }
                    }
                }

                val cur = HudOverlay.bounds(id) ?: b
                val color = if (dragging) 0xFFFFFF55.toInt() else 0xCC55FFFF.toInt()
                graphics.outline(cur[0] - 2, cur[1] - 2, cur[2] + 4, cur[3] + 4, color)
            }
            wasDown = down

            graphics.text(font, "Drag the panel with the mouse, then press Done", width / 2 - 140, 14, 0xFFFFFFFF.toInt(), true)
            if (b == null) {
                graphics.text(font, "Nothing to move yet (panel is hidden or empty)", width / 2 - 140, 28, 0xFFFF5555.toInt(), true)
            }

            super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        }
    }
}
