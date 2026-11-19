package com.chattriggers.ctjs.engine

import com.chattriggers.ctjs.engine.langs.Lang
import com.chattriggers.ctjs.triggers.Trigger
import com.chattriggers.ctjs.triggers.TriggerType
import com.chattriggers.ctjs.utils.console.Console
import java.io.File
import java.net.URI
import java.net.URL

/**
 * @suppress This is internal and should not show in userdocs
 */
interface ILoader {
    val console: Console

    fun setup(jars: List<URL>)

    fun entrySetup()

    fun entryPass(module: com.chattriggers.ctjs.engine.module.Module, entryURI: URI)

    fun exec(type: TriggerType, args: Array<out Any?>)

    fun eval(code: String): String

    fun addTrigger(trigger: Trigger)

    fun clearTriggers()

    fun getLanguage(): Lang

    fun trigger(trigger: Trigger, method: Any, args: Array<out Any?>)

    fun removeTrigger(trigger: Trigger)

    fun saveResource(resourceName: String?, outputFile: File, replace: Boolean): String {
        require(resourceName != null && resourceName != "") {
            "ResourcePath cannot be null or empty"
        }

        val parsedResourceName = resourceName.replace('\\', '/')
        val resource = javaClass.getResourceAsStream(parsedResourceName)
            ?: throw IllegalArgumentException("The embedded resource '$parsedResourceName' cannot be found.")

        val res = resource.bufferedReader().readText()
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(res)
        return res
    }
}
