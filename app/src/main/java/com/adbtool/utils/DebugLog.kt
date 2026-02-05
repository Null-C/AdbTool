package com.adbtool.utils

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import com.adbtool.utils.FormatUtils

object DebugLog {
    private val logs = ConcurrentLinkedQueue<LogEntry>()
    private val maxLogs = 200 // 增加到200条日志
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private var logFile: File? = null
    private var context: Context? = null

    // 初始化日志系统
    fun init(context: Context) {
        this.context = context
        // 改为应用内存储
        logFile = File(context.filesDir, "adb-tool.log")

        // 清空历史日志文件（每次启动时清空）
        clearLogFile()

        // 清理缓存目录中的APK临时文件
        clearCacheFiles(context)

        i("DebugLog", "DebugLog initialized, log file: ${logFile?.absolutePath}")
        i("DebugLog", "=== ADB Tool Session Started ===")
    }

    private fun writeToFile(entry: LogEntry) {
        logFile?.let { file ->
            try {
                val writer = FileWriter(file, true) // 追加模式
                writer.append("[${entry.timestamp}] ${entry.level.prefix}/${entry.tag}: ${entry.message}\n")
                writer.flush()
                writer.close()
            } catch (e: IOException) {
                android.util.Log.e("DebugLog", "Failed to write log to file", e)
            }
        }
    }

    data class LogEntry(
        val timestamp: String,
        val tag: String,
        val message: String,
        val level: Level
    )

    enum class Level(val prefix: String) {
        DEBUG("D"),
        INFO("I"),
        WARN("W"),
        ERROR("E")
    }

    fun d(tag: String, message: String) {
        addLog(Level.DEBUG, tag, message)
        android.util.Log.d(tag, message)
    }

    fun i(tag: String, message: String) {
        addLog(Level.INFO, tag, message)
        android.util.Log.i(tag, message)
    }

    fun w(tag: String, message: String) {
        addLog(Level.WARN, tag, message)
        android.util.Log.w(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        addLog(Level.ERROR, tag, fullMessage)
        android.util.Log.e(tag, message, throwable)
    }

    private fun addLog(level: Level, tag: String, message: String) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            tag = tag,
            message = message,
            level = level
        )

        logs.offer(entry)

        // 写入文件
        if (level != Level.DEBUG || logs.size % 10 == 0) { // 每10条调试日志写一次，其他级别立即写
            writeToFile(entry)
        }

        // 保持日志数量在限制内
        while (logs.size > maxLogs) {
            logs.poll()
        }
    }

    fun getLogs(): List<LogEntry> {
        return logs.toList()
    }

    fun clearLogs() {
        logs.clear()
    }

    fun getLogString(): String {
        return logs.joinToString("\n") { entry ->
            "[${entry.timestamp}] ${entry.level.prefix}/${entry.tag}: ${entry.message}"
        }
    }

    fun getLogFile(): File? = logFile

    // 获取日志文件内容
    fun getLogContent(): String {
        if (logFile == null) {
            return "Log file not initialized"
        }

        val file = logFile!!
        if (!file.exists()) {
            return "No logs available"
        }

        return try {
            file.readText()
        } catch (e: Exception) {
            "Failed to read log file: ${e.message}"
        }
    }

    // 清空日志文件
    fun clearLogFile() {
        val file = logFile ?: return
        try {
            if (file.exists()) {
                file.writeText("")
            }
        } catch (e: Exception) {
            android.util.Log.e("DebugLog", "Failed to clear log file", e)
        }
    }

    fun flushToFile() {
        // 将所有内存中的日志写入文件
        logs.forEach { entry ->
            writeToFile(entry)
        }
        i("DebugLog", "All logs flushed to file")
    }

    // 清理缓存目录中的APK临时文件
    private fun clearCacheFiles(context: Context) {
        val (cleanedCount, cleanedSize) = CacheUtils.clearApkCacheFiles(context, "DebugLog")
        if (cleanedCount > 0) {
            i("DebugLog", "Startup cache cleanup completed: removed $cleanedCount files, freed ${FormatUtils.formatFileSize(cleanedSize)}")
        }
    }

    }