package com.chattriggers.ctjs.utils.console

import org.slf4j.LoggerFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

object CTLog {
    private const val MAX_LINES = 500

    private val logger = LoggerFactory.getLogger("ChatTriggers")
    private val lines = ArrayDeque<String>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss")

    val file = File("./config/ChatTriggers/ctjs.log")

    init {
        try {
            file.parentFile.mkdirs()
            file.writeText("")
        } catch (e: Throwable) {
            logger.warn("Could not create ${file.path}", e)
        }
    }

    @Synchronized
    fun add(level: LogType, text: String) {
        val prefix = "[${timeFormat.format(Date())}] [${level.name}] "

        text.split("\n").forEach { part ->
            val line = prefix + part
            lines.addLast(line)
            while (lines.size > MAX_LINES) lines.removeFirst()

            try {
                file.appendText(line + "\n")
            } catch (e: Throwable) {
            }

            when (level) {
                LogType.INFO -> logger.info(part)
                LogType.WARN -> logger.warn(part)
                LogType.ERROR -> logger.error(part)
            }
        }
    }

    fun info(text: String) = add(LogType.INFO, text)

    @Synchronized
    fun last(count: Int): List<String> = lines.toList().takeLast(count)

    @Synchronized
    fun clear() {
        lines.clear()
        try {
            file.writeText("")
        } catch (e: Throwable) {
        }
    }
}
