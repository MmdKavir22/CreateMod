package com.sushitriggers.ctjs.engine.module

import com.sushitriggers.ctjs.CTJS
import com.sushitriggers.ctjs.Reference
import com.sushitriggers.ctjs.minecraft.libs.ChatLib
import com.sushitriggers.ctjs.utils.console.CTLog
import com.sushitriggers.ctjs.utils.console.LogType
import com.google.gson.JsonObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.Base64
import kotlin.concurrent.thread

object ScriptRepo {
    private const val DEFAULT_REPO = "MmdKavir22/CreateMod"
    private val configFile = File("./config/SushiTriggers/repo.txt")
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

    private val tokenFile = File("./config/SushiTriggers/github-token.txt")

    fun token(): String? {
        return try {
            if (tokenFile.exists()) tokenFile.readText().trim().ifEmpty { null } else null
        } catch (e: Throwable) {
            null
        }
    }

    fun hasToken() = token() != null

    fun setToken(value: String) {
        tokenFile.parentFile.mkdirs()
        tokenFile.writeText(value.trim())
    }

    private fun api(method: String, url: String, body: ByteArray? = null): Pair<Int, ByteArray> {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.setRequestProperty("User-Agent", "SushiTriggers-Fabric")
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        token()?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
        connection.connectTimeout = 15000
        connection.readTimeout = 30000

        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body) }
        }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        return code to (stream?.use { it.readBytes() } ?: ByteArray(0))
    }

    private fun download(url: String): ByteArray {
        val (code, body) = api("GET", url)
        if (code !in 200..299) {
            throw IOException("HTTP $code ($url)")
        }

        return body
    }

    @Synchronized
    fun uploadModule(name: String): Int {
        require(validName.matches(name) && name != "." && name != "..") { "Invalid script name: $name" }
        if (token() == null) throw IllegalStateException("No GitHub token set (Config tab, or /st token <token>)")

        val dir = File(ModuleManager.modulesFolder, name)
        require(dir.isDirectory) { "Module $name was not found in modules/" }

        val files = dir.walk().filter { it.isFile }.toList()
        files.forEach { file ->
            val relative = file.relativeTo(dir).path.replace(File.separatorChar, '/')
            putFile("scripts/$name/$relative", file.readBytes())
        }

        cached = emptyList()
        treePaths = emptyList()
        return files.size
    }

    private fun putFile(path: String, bytes: ByteArray) {
        val url = "https://api.github.com/repos/${repoName()}/contents/${encodePath(path)}"
        val explicitBranch = branch() != "HEAD"
        val refQuery = if (explicitBranch) "?ref=${URLEncoder.encode(branch(), "UTF-8")}" else ""

        val (getCode, getBody) = api("GET", url + refQuery)
        val sha = if (getCode == 200) CTJS.gson.fromJson(String(getBody), JsonObject::class.java).get("sha")?.asString else null

        val json = JsonObject()
        json.addProperty("message", "Update $path via SushiTriggers")
        json.addProperty("content", Base64.getEncoder().encodeToString(bytes))
        if (sha != null) json.addProperty("sha", sha)
        if (explicitBranch) json.addProperty("branch", branch())

        val (code, response) = api("PUT", url, CTJS.gson.toJson(json).toByteArray())
        if (code !in 200..201) {
            throw IOException("HTTP $code while uploading $path: ${String(response).take(200)}")
        }
    }

    fun uploadAsync(name: String, status: (String) -> Unit = {}) {
        thread {
            try {
                status("Uploading $name...")
                val count = uploadModule(name)
                status("Uploaded $name ($count files)")
                ChatLib.chat("&a[CT] Uploaded &f$name &a($count files) to ${spec()}")
            } catch (e: Throwable) {
                CTLog.add(LogType.ERROR, "Upload of $name failed: $e")
                status("Failed: ${e.message}")
                ChatLib.chat("&c[CT] Upload of $name failed: ${e.message}")
            }
        }
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
