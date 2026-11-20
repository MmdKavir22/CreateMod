package com.chattriggers.ctjs.utils

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.Reference
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File

object Config {
    @JvmField
    var modulesFolder: String = Reference.DEFAULT_MODULES_FOLDER

    @JvmField
    var threadedLoading = true

    @JvmField
    var moduleImportHelp = true

    @JvmField
    var moduleChangelog = true

    @JvmField
    var printChatToConsole = true

    @JvmField
    var showUpdatesInChat = true

    @JvmField
    var autoUpdateModules = true

    @JvmField
    var clearConsoleOnLoad = true

    @JvmField
    var openConsoleOnError = false

    @JvmField
    var logEvents = true

    @JvmField
    var eventsInChat = false

    @JvmField
    var eventHideWords = ""

    @JvmField
    val disabledEventCategories = mutableSetOf<String>()

    private val settingsFile = File("./config/ChatTriggers/settings.json")

    fun load() {
        try {
            if (!settingsFile.exists()) return
            val json = CTJS.gson.fromJson(settingsFile.readText(), JsonObject::class.java)
            json.get("logEvents")?.let { logEvents = it.asBoolean }
            json.get("eventHideWords")?.let { eventHideWords = it.asString }
            json.get("eventsInChat")?.let { eventsInChat = it.asBoolean }
            json.get("threadedLoading")?.let { threadedLoading = it.asBoolean }
            json.get("clearConsoleOnLoad")?.let { clearConsoleOnLoad = it.asBoolean }
            json.getAsJsonArray("disabledEventCategories")?.let { array ->
                disabledEventCategories.clear()
                array.forEach { disabledEventCategories.add(it.asString) }
            }
        } catch (e: Throwable) {
        }
    }

    fun save() {
        try {
            val json = JsonObject()
            json.addProperty("logEvents", logEvents)
            json.addProperty("eventHideWords", eventHideWords)
            json.addProperty("eventsInChat", eventsInChat)
            json.addProperty("threadedLoading", threadedLoading)
            json.addProperty("clearConsoleOnLoad", clearConsoleOnLoad)
            val array = JsonArray()
            disabledEventCategories.forEach { array.add(it) }
            json.add("disabledEventCategories", array)
            settingsFile.parentFile.mkdirs()
            settingsFile.writeText(CTJS.gson.toJson(json))
        } catch (e: Throwable) {
        }
    }
}
