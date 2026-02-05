package com.adbtool.utils

import android.content.Context
import com.adbtool.adb.DadbClient
import java.io.File

object CacheUtils {

    /**
     * 清理源设备缓存目录中的APK文件
     * @param context 应用上下文
     * @param logTag 日志标签，默认为 "CacheUtils"
     * @return 清理结果 (清理的文件数量, 释放的空间大小)
     */
    fun clearApkCacheFiles(context: Context, logTag: String = "CacheUtils"): Pair<Int, Long> {
        return try {
            val cacheDir = context.cacheDir
            if (!cacheDir.exists()) {
                DebugLog.d(logTag, "Cache directory does not exist")
                return Pair(0, 0L)
            }

            val files = cacheDir.listFiles { file ->
                file.isFile && (file.name.endsWith(".apk") || file.name.contains("temp_selected"))
            }

            var cleanedSize = 0L
            var cleanedCount = 0
            files?.forEach { file ->
                try {
                    val fileSize = file.length()
                    if (file.delete()) {
                        cleanedSize += fileSize
                        cleanedCount++
                        DebugLog.d(logTag, "Deleted cache file: ${file.name} (${FormatUtils.formatFileSize(fileSize)})")
                    }
                } catch (e: Exception) {
                    DebugLog.e(logTag, "Failed to delete cache file: ${file.name}", e)
                }
            }

            if (cleanedCount > 0) {
                DebugLog.i(logTag, "Cleaned $cleanedCount cache APK files, freed ${FormatUtils.formatFileSize(cleanedSize)}")
            } else {
                DebugLog.d(logTag, "No cache APK files to clean")
            }

            Pair(cleanedCount, cleanedSize)
        } catch (e: Exception) {
            DebugLog.e(logTag, "Failed to clear cache files", e)
            Pair(0, 0L)
        }
    }

    /**
     * 清理目标设备的APK缓存文件
     * @param dadbClient ADB客户端实例
     * @param logTag 日志标签，默认为 "CacheUtils"
     * @return 是否成功执行清理
     */
    suspend fun cleanTargetDeviceApkCache(dadbClient: DadbClient, logTag: String = "CacheUtils"): Boolean {
        return try {
            // 检查连接状态
            if (!dadbClient.isConnected()) {
                DebugLog.w(logTag, "Cannot clean remote cache: no device connected")
                return false
            }

            // 先列出当前文件
            val listResult = dadbClient.executeCommand("ls -la /data/local/tmp/*.apk 2>/dev/null || echo 'No APK files found'")
            if (!listResult.isSuccess) {
                DebugLog.w(logTag, "Failed to list APK files")
                return false
            }

            val fileList = listResult.getOrNull() ?: ""
            if (fileList.contains("No APK files found")) {
                DebugLog.i(logTag, "Remote device: No APK files to clean")
                return true
            }

            DebugLog.i(logTag, "Remote device APK files to be cleaned:\n$fileList")

            // 执行清理
            val cleanupResult = dadbClient.executeCommand("rm -f /data/local/tmp/*.apk")
            if (!cleanupResult.isSuccess) {
                DebugLog.w(logTag, "Failed to execute cleanup command")
                return false
            }

            DebugLog.i(logTag, "Remote device: APK cache cleanup completed")

            // 验证清理结果
            val verifyResult = dadbClient.executeCommand("ls /data/local/tmp/*.apk 2>/dev/null | wc -l")
            if (verifyResult.isSuccess) {
                val remaining = verifyResult.getOrNull()?.trim()?.toIntOrNull() ?: 0
                if (remaining == 0) {
                    DebugLog.i(logTag, "Remote device: All APK files successfully cleaned")
                } else {
                    DebugLog.w(logTag, "Remote device: $remaining APK files still remaining")
                }
            }

            true
        } catch (e: Exception) {
            DebugLog.e(logTag, "Remote device cache cleanup failed", e)
            false
        }
    }
}