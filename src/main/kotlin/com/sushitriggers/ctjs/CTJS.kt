package com.sushitriggers.ctjs

import com.sushitriggers.ctjs.commands.CTCommand
import com.sushitriggers.ctjs.commands.Command
import com.sushitriggers.ctjs.gui.ScriptGui
import com.sushitriggers.ctjs.engine.module.ModuleManager
import com.sushitriggers.ctjs.triggers.TriggerType
import com.sushitriggers.ctjs.utils.Config
import com.sushitriggers.ctjs.utils.console.ActivityLog
import com.google.gson.Gson
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import java.io.File
import java.net.URL
import java.net.URLConnection
import kotlin.concurrent.thread

class CTJS : ClientModInitializer {
    override fun onInitializeClient() {
        Config.load()

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            Command.attach(dispatcher)
            CTCommand.register(dispatcher)
        }

        ClientTickEvents.END_CLIENT_TICK.register {
            try {
                ActivityLog.tick()
            } catch (e: Throwable) {
            }

            ScriptGui.tick()
            ticksPassed++
            TriggerType.Tick.triggerAll(ticksPassed)
        }

        ClientLifecycleEvents.CLIENT_STARTED.register {
            Reference.conditionalThread {
                ModuleManager.setup()
                ModuleManager.entryPass()
                TriggerType.GameLoad.triggerAll()
            }
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register {
            TriggerType.GameUnload.triggerAll()
        }
    }

    companion object {
        const val WEBSITE_ROOT = "https://github.com/MmdKavir22/CreateMod"

        init {
            try {
                val old = File("./config/ChatTriggers")
                val new = File("./config/SushiTriggers")
                if (old.isDirectory && !new.exists()) old.renameTo(new)
            } catch (e: Throwable) {
            }
        }

        @JvmField
        val gson = Gson()

        @JvmField
        val configLocation = File("./config")

        @JvmField
        val assetsDir = File(configLocation, "SushiTriggers/images/").apply { mkdirs() }

        private var ticksPassed = 0

        @JvmStatic
        fun makeWebRequest(url: String): URLConnection {
            val connection = URL(url).openConnection()
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (SushiTriggers)")
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            return connection
        }
    }
}
