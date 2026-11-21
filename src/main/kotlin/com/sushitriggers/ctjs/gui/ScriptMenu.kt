package com.sushitriggers.ctjs.gui

import com.sushitriggers.ctjs.minecraft.libs.GameUtil
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

    fun tick() {
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
}
