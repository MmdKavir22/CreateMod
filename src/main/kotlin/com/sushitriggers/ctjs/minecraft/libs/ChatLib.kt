package com.sushitriggers.ctjs.minecraft.libs

import com.sushitriggers.ctjs.utils.console.CTLog
import com.sushitriggers.ctjs.utils.console.LogType
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.lang.reflect.Method

object ChatLib {
    @JvmStatic
    fun chat(message: Any?) {
        val mc = Minecraft.getInstance()
        val text = addColor(message.toString())
        val component = Component.literal(text)

        mc.execute {
            if (!sendToChat(mc, component)) {
                CTLog.add(LogType.INFO, text)
            }
        }
    }

    @JvmStatic
    fun addColor(message: String?): String {
        return message.toString().replace("(?<!\\\\)&(?![^0-9a-fk-or]|$)".toRegex(), "§")
    }

    @JvmStatic
    fun removeFormatting(text: String): String {
        return text.replace("[§&][0-9a-fk-or]".toRegex(), "")
    }

    private fun findMethod(target: Any, name: String, paramCount: Int): Method? {
        return target.javaClass.methods.firstOrNull {
            it.name == name && it.parameterCount == paramCount &&
                it.parameterTypes[0].isAssignableFrom(Component::class.java)
        }
    }

    private fun getMember(target: Any, name: String): Any? {
        val getter = target.javaClass.methods.firstOrNull { it.name == "get" + name.replaceFirstChar(Char::uppercase) && it.parameterCount == 0 }
        if (getter != null) return getter.invoke(target)

        return try {
            target.javaClass.getField(name).get(target)
        } catch (e: Exception) {
            null
        }
    }

    private fun sendToChat(mc: Minecraft, component: Component): Boolean {
        val player = mc.player
        if (player != null) {
            findMethod(player, "sendSystemMessage", 1)?.let {
                it.invoke(player, component)
                return true
            }

            findMethod(player, "displayClientMessage", 2)?.let {
                it.invoke(player, component, false)
                return true
            }
        }

        val gui = getMember(mc, "gui") ?: return false
        val chat = getMember(gui, "chat") ?: return false

        for (name in listOf("addMessage", "addClientSystemMessage")) {
            findMethod(chat, name, 1)?.let {
                it.invoke(chat, component)
                return true
            }
        }

        return false
    }
}
