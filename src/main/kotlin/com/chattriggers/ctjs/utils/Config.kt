package com.chattriggers.ctjs.utils

import com.chattriggers.ctjs.Reference

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
}
