package com.sushitriggers.ctjs.engine.module

import com.sushitriggers.ctjs.CTJS
import com.sushitriggers.ctjs.Reference
import com.sushitriggers.ctjs.engine.ILoader
import com.sushitriggers.ctjs.engine.langs.js.JSLoader
import com.sushitriggers.ctjs.minecraft.libs.FileLib
import com.sushitriggers.ctjs.printToConsole
import com.sushitriggers.ctjs.triggers.TriggerType
import com.sushitriggers.ctjs.utils.Config
import com.sushitriggers.ctjs.utils.console.Console
import com.sushitriggers.ctjs.utils.console.LogType
import java.io.File

object ModuleManager {
    private val loaders = listOf(JSLoader)
    val generalConsole = Console(null)
    val cachedModules = mutableListOf<Module>()
    val modulesFolder = File(Reference.DEFAULT_MODULES_FOLDER)

    fun setup() {
        modulesFolder.mkdirs()

        val installedModules = getFoldersInDir(modulesFolder).map(::parseModule).distinctBy {
            it.name.lowercase()
        }

        cachedModules.addAll(installedModules)

        loadAssetsAndJars(cachedModules)
    }

    private fun loadAssetsAndJars(modules: List<Module>) {
        loadAssets(modules)

        modules.forEach {
            it.metadata.entry = it.metadata.entry?.replace('/', File.separatorChar)?.replace('\\', File.separatorChar)
        }

        val jars = modules.map { module ->
            module.folder.walk().filter {
                it.isFile && it.extension == "jar"
            }.map {
                it.toURI().toURL()
            }.toList()
        }.flatten()

        loaders.forEach {
            it.setup(jars)
        }
    }

    fun entryPass(modules: List<Module> = cachedModules, completionListener: (percentComplete: Float) -> Unit = {}) {
        loaders.forEach(ILoader::entrySetup)

        val total = modules.count { it.metadata.entry != null }
        var completed = 0

        loaders.forEach { loader ->
            modules.filter {
                File(it.folder, it.metadata.entry ?: return@filter false).extension == loader.getLanguage().extension
            }.forEach {
                loader.entryPass(it, File(it.folder, it.metadata.entry!!).toURI())

                completed++
                completionListener(completed.toFloat() / total)
            }
        }
    }

    private fun getFoldersInDir(dir: File): List<File> {
        if (!dir.isDirectory) return emptyList()

        return dir.listFiles()?.filter {
            it.isDirectory
        } ?: listOf()
    }

    fun parseModule(directory: File): Module {
        val metadataFile = File(directory, "metadata.json")
        var metadata = ModuleMetadata()

        if (metadataFile.exists()) {
            try {
                metadata = CTJS.gson.fromJson(FileLib.read(metadataFile), ModuleMetadata::class.java)
            } catch (exception: Exception) {
                "Module $directory has invalid metadata.json".printToConsole(logType = LogType.ERROR)
            }
        }

        return Module(directory.name, metadata, directory)
    }

    private fun loadAssets(modules: List<Module>) {
        modules.map {
            File(it.folder, "assets")
        }.filter {
            it.exists() && !it.isFile
        }.map {
            it.listFiles()?.toList() ?: emptyList()
        }.flatten().forEach {
            it.copyTo(File(CTJS.assetsDir, it.name), overwrite = true)
        }
    }

    fun teardown() {
        cachedModules.clear()

        loaders.forEach {
            it.clearTriggers()

            if (Config.clearConsoleOnLoad) {
                it.console.clearConsole()
            }
        }

        if (Config.clearConsoleOnLoad)
            generalConsole.clearConsole()
    }

    fun trigger(type: TriggerType, arguments: Array<out Any?>) {
        loaders.forEach {
            it.exec(type, arguments)
        }
    }

    fun getConsole(language: String): Console {
        return loaders.firstOrNull {
            it.getLanguage().langName == language
        }?.console ?: generalConsole
    }
}
