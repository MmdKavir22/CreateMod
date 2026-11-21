package com.sushitriggers.ctjs.minecraft.listeners

import com.sushitriggers.ctjs.triggers.TriggerType
import com.sushitriggers.ctjs.utils.Refl
import com.sushitriggers.ctjs.utils.console.ActivityLog
import com.sushitriggers.ctjs.utils.console.CTLog
import com.sushitriggers.ctjs.utils.console.LogType
import net.minecraft.client.Minecraft

object ChatWatcher {
    private var chatComponent: Any? = null
    private var messageList: java.lang.reflect.Field? = null
    private var lastFirst: Any? = null
    private var warned = false
    private var searchedAt = 0L

    private fun find(mc: Minecraft): Boolean {
        if (messageList != null && chatComponent != null) return true
        if (System.currentTimeMillis() - searchedAt < 5000) return false
        searchedAt = System.currentTimeMillis()

        val roots = ArrayList<Any>()
        val gui = Refl.get(mc, "gui") ?: return false
        roots += gui
        Refl.get(gui, "hud")?.let { roots += it }
        try {
            val f = gui.javaClass.getDeclaredField("hud")
            f.isAccessible = true
            f.get(gui)?.let { roots += it }
        } catch (e: Throwable) {
        }

        for (root in roots) {
            var cls: Class<*>? = root.javaClass
            while (cls != null && cls != Any::class.java) {
                for (f in cls.declaredFields) {
                    if (f.type.simpleName != "ChatComponent") continue
                    try {
                        f.isAccessible = true
                        val chat = f.get(root) ?: continue
                        val list = findList(chat) ?: continue
                        chatComponent = chat
                        messageList = list
                        return true
                    } catch (e: Throwable) {
                    }
                }
                cls = cls.superclass
            }
        }

        if (!warned) {
            warned = true
            CTLog.add(LogType.WARN, "Chat watcher: ChatComponent not found, chat trigger disabled")
        }
        return false
    }

    private fun findList(chat: Any): java.lang.reflect.Field? {
        var cls: Class<*>? = chat.javaClass
        while (cls != null && cls != Any::class.java) {
            for (f in cls.declaredFields) {
                if (!List::class.java.isAssignableFrom(f.type)) continue
                f.isAccessible = true
                val list = f.get(chat) as? List<*> ?: continue
                val first = list.firstOrNull()
                if (first == null || first.javaClass.simpleName == "GuiMessage") return f
            }
            cls = cls.superclass
        }
        return null
    }

    private fun contentOf(message: Any?): String? {
        val component = Refl.call(message, "content", "getContent") ?: return null
        return Refl.text(component)
    }

    fun tick(mc: Minecraft) {
        if (mc.player == null) {
            lastFirst = null
            return
        }
        if (!find(mc)) return

        val list = try {
            messageList!!.get(chatComponent) as? List<*>
        } catch (e: Throwable) {
            null
        } ?: return

        val first = list.firstOrNull()
        if (first == null) return
        if (lastFirst == null) {
            lastFirst = first
            return
        }
        if (first === lastFirst) return

        val fresh = ArrayList<Any>()
        for (m in list) {
            if (m === lastFirst || fresh.size >= 30 || m == null) break
            fresh += m
        }
        lastFirst = first

        fresh.reversed().forEach { m ->
            val text = contentOf(m) ?: return@forEach
            ActivityLog.event("CHAT $text")
            try {
                TriggerType.Chat.triggerAll(text, CancellableEvent())
            } catch (e: Throwable) {
            }
        }
    }
}
