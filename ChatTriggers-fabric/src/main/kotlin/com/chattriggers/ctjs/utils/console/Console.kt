package com.chattriggers.ctjs.utils.console

import com.chattriggers.ctjs.engine.ILoader
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer

class Console(val loader: ILoader?) {
    private val logger = LoggerFactory.getLogger("ChatTriggers" + (loader?.getLanguage()?.let { "/${it.langName}" } ?: ""))

    val writer = ConsoleWriter(logger)

    fun clearConsole() {}

    fun println(obj: Any, logType: LogType = LogType.INFO, end: String = "\n") {
        val text = obj.toString()
        when (logType) {
            LogType.INFO -> logger.info(text)
            LogType.WARN -> logger.warn(text)
            LogType.ERROR -> logger.error(text)
        }
    }

    fun printStackTrace(error: Throwable) {
        val sw = StringWriter()
        error.printStackTrace(PrintWriter(sw))
        logger.error(sw.toString())
    }

    fun showConsole() {}

    class ConsoleWriter(private val logger: org.slf4j.Logger) {
        private val buffer = StringBuilder()

        val printWriter = PrintWriter(object : Writer() {
            override fun write(cbuf: CharArray, off: Int, len: Int) {
                synchronized(buffer) {
                    buffer.append(cbuf, off, len)
                    var index = buffer.indexOf("\n")
                    while (index >= 0) {
                        logger.info(buffer.substring(0, index))
                        buffer.delete(0, index + 1)
                        index = buffer.indexOf("\n")
                    }
                }
            }

            override fun flush() {}
            override fun close() {}
        }, true)
    }
}
