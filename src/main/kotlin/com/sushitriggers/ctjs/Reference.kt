package com.sushitriggers.ctjs

import com.sushitriggers.ctjs.engine.module.ModuleManager
import com.sushitriggers.ctjs.minecraft.libs.ChatLib
import com.sushitriggers.ctjs.triggers.TriggerType
import com.sushitriggers.ctjs.utils.Config
import com.sushitriggers.ctjs.utils.console.Console
import com.sushitriggers.ctjs.utils.console.LogType
import kotlin.concurrent.thread

object Reference {
    const val MODID = "sushitriggers"
    const val MODNAME = "SushiTriggers"
    const val MODVERSION = "2.2.1"

    const val DEFAULT_MODULES_FOLDER = "./config/SushiTriggers/modules"

    var isLoaded = true

    @Deprecated("Does not provide any additional functionality", ReplaceWith("loadCT"))
    @JvmStatic
    fun reloadCT() = loadCT()

    @JvmStatic
    fun unloadCT(asCommand: Boolean = true) {
        TriggerType.WorldUnload.triggerAll()
        TriggerType.GameUnload.triggerAll()

        isLoaded = false

        ModuleManager.teardown()

        com.sushitriggers.ctjs.commands.Command.activeCommands.values.toList().forEach {
            it.unregister()
        }

        if (asCommand) {
            ChatLib.chat("&7Unloaded all of SushiTriggers")
        }
    }

    @JvmStatic
    fun loadCT() {
        unloadCT(false)

        ChatLib.chat("&cReloading SushiTriggers scripts...")

        conditionalThread {
            ModuleManager.setup()
            ModuleManager.entryPass()

            ChatLib.chat("&aDone reloading scripts!")
            isLoaded = true

            TriggerType.GameLoad.triggerAll()
        }
    }

    @JvmStatic
    fun conditionalThread(block: () -> Unit) {
        if (Config.threadedLoading) {
            thread {
                try {
                    block()
                } catch (e: Throwable) {
                    e.printTraceToConsole()
                }
            }
        } else {
            block()
        }
    }
}

fun Any.printToConsole(console: Console = ModuleManager.generalConsole, logType: LogType = LogType.INFO) {
    console.println(this, logType)
}

fun Throwable.printTraceToConsole(console: Console = ModuleManager.generalConsole) = console.printStackTrace(this)
