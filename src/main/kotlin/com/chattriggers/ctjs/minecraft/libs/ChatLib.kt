package com.chattriggers.ctjs.minecraft.libs

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object ChatLib {
    @JvmStatic
    fun chat(message: Any?) {
        val mc = Minecraft.getInstance()
        val component = Component.literal(addColor(message.toString()))
        mc.execute { mc.gui.getChat().addMessage(component) }
    }

    @JvmStatic
    fun addColor(message: String?): String {
        return message.toString().replace("(?<!\\\\)&(?![^0-9a-fk-or]|$)".toRegex(), "§")
    }

    @JvmStatic
    fun removeFormatting(text: String): String {
        return text.replace("[§&][0-9a-fk-or]".toRegex(), "")
    }
}
