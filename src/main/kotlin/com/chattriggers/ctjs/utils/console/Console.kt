package com.chattriggers.ctjs.utils.console

import com.chattriggers.ctjs.engine.ILoader
import com.chattriggers.ctjs.minecraft.libs.ChatLib
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer

class Console(val loader: ILoader?) {
    val writer = ConsoleWriter()

    fun clearConsole() {}

    fun println(obj: Any, logType: LogType = LogType.INFO, end: String = "\n") {
        val text = obj.toString()
        CTLog.add(logType, text)

        when (logType) {
            LogType.ERROR -> ChatLib.chat("&c[CT] " + text.lineSequence().first())
            LogType.WARN -> ChatLib.chat("&e[CT] " + text.lineSequence().first())
            LogType.INFO -> {}
        }
    }

    fun printStackTrace(error: Throwable) {
        val sw = StringWriter()
        error.printStackTrace(PrintWriter(sw))
        CTLog.add(LogType.ERROR, sw.toString())
        ChatLib.chat("&c[CT] ${error.javaClass.simpleName}: ${error.message}")
    }

    fun showConsole() {}

    class ConsoleWriter {
        private val buffer = StringBuilder()

        val printWriter = PrintWriter(object : Writer() {
            override fun write(cbuf: CharArray, off: Int, len: Int) {
                synchronized(buffer) {
                    buffer.append(cbuf, off, len)
                    var index = buffer.indexOf("\n")
                    while (index >= 0) {
                        val line = buffer.substring(0, index)
                        buffer.delete(0, index + 1)
                        CTLog.add(LogType.INFO, line)

                        if (line.contains("error", ignoreCase = true) || line.contains("exception", ignoreCase = true))
                            ChatLib.chat("&c[CT] $line")

                        index = buffer.indexOf("\n")
                    }
                }
            }

            override fun flush() {}
            override fun close() {}
        }, true)
    }
}
