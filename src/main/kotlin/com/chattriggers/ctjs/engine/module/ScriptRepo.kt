package com.chattriggers.ctjs.engine.module

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.Reference
import com.chattriggers.ctjs.minecraft.libs.ChatLib
import com.chattriggers.ctjs.utils.console.CTLog
import com.chattriggers.ctjs.utils.console.LogType
import com.google.gson.JsonObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import kotlin.concurrent.thread

object ScriptRepo {
    private const val DEFAULT_REPO = "MmdKavir22/CreateMod"
    private val configFile = File("./config/ChatTriggers/repo.txt")
    private val validName = Regex("^[A-Za-z0-9._-]+$")

    @Volatile
    var cached: List<String> = emptyList()

    @Volatile
    private var treePaths: List<String> = emptyList()

    fun spec(): String {
        return try {
            if (configFile.exists()) configFile.readText().trim().ifEmpty { DEFAULT_REPO } else DEFAULT_REPO
        } catch (e: Throwable) {
            DEFAULT_REPO
        }
    }

    fun setSpec(value: String) {
        configFile.parentFile.mkdirs()
        configFile.writeText(value.trim())
        cached = emptyList()
        treePaths = emptyList()
    }

    private fun repoName() = spec().substringBefore('@')

    private fun branch() = spec().substringAfter('@', "HEAD")

    fun isInstalled(name: String) = File(ModuleManager.modulesFolder, name).isDirectory

    private fun download(url: String): ByteArray {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "ChatTriggers-Fabric")
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.connectTimeout = 15000
        connection.readTimeout = 30000

        val code = connection.responseCode
        if (code !in 200..299) {
            throw IOException("HTTP $code ($url)")
        }

        return connection.inputStream.use { it.readBytes() }
    }

    @Synchronized
    fun fetchList(): List<String> {
        val json = String(download("https://api.github.com/repos/${repoName()}/git/trees/${branch()}?recursive=1"))
        val tree = CTJS.gson.fromJson(json, JsonObject::class.java).getAsJsonArray("tree")

        val paths = tree.map { it.asJsonObject }
            .filter { it.get("type").asString == "blob" }
            .map { it.get("path").asString }
            .filter { it.startsWith("scripts/") }

        treePaths = paths
        cached = paths.map { it.removePrefix("scripts/") }
            .filter { it.contains('/') }
            .map { it.substringBefore('/') }
            .distinct()
            .sorted()

        return cached
    }

    private fun encodePath(path: String) = path.split('/').joinToString("/") {
        URLEncoder.encode(it, "UTF-8").replace("+", "%20")
    }

    @Synchronized
    fun importModule(name: String): Int {
        require(validName.matches(name) && name != "." && name != "..") { "Invalid script name: $name" }

        val prefix = "scripts/$name/"
        if (treePaths.none { it.startsWith(prefix) }) fetchList()

        val files = treePaths.filter { it.startsWith(prefix) }
        if (files.isEmpty()) throw IllegalArgumentException("Script '$name' was not found in ${spec()} (scripts/$name/)")

        val dir = File(ModuleManager.modulesFolder, name)
        dir.mkdirs()
        val root = dir.canonicalPath + File.separator

        files.forEach { path ->
            val relative = path.removePrefix(prefix)
            val out = File(dir, relative)
            require(out.canonicalPath.startsWith(root)) { "Unsafe path: $path" }

            out.parentFile.mkdirs()
            out.writeBytes(download("https://raw.githubusercontent.com/${repoName()}/${branch()}/${encodePath(path)}"))
        }

        val metadata = File(dir, "metadata.json")
        if (!metadata.exists()) {
            val entry = listOf("$name.js", "index.js").firstOrNull { File(dir, it).exists() }
                ?: dir.listFiles()?.firstOrNull { it.isFile && it.extension == "js" }?.name

            val data = mutableMapOf<String, Any>("name" to name, "version" to "1.0.0")
            if (entry != null) data["entry"] = entry
            metadata.writeText(CTJS.gson.toJson(data))
        }

        return files.size
    }

    fun listAsync(onDone: (List<String>?, String?) -> Unit) {
        thread {
            try {
                onDone(fetchList(), null)
            } catch (e: Throwable) {
                CTLog.add(LogType.ERROR, "Script list failed: $e")
                onDone(null, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun importAsync(name: String, status: (String) -> Unit = {}) {
        thread {
            try {
                status("Importing $name...")
                val count = importModule(name)
                status("Imported $name ($count files)")
                ChatLib.chat("&a[CT] Imported script &f$name &a($count files) into modules")
                Reference.loadCT()
            } catch (e: Throwable) {
                CTLog.add(LogType.ERROR, "Import of $name failed: $e")
                status("Failed: ${e.message}")
                ChatLib.chat("&c[CT] Import of $name failed: ${e.message}")
            }
        }
    }
}
