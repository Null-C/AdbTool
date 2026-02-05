package com.adbtool.utils

/**
 * 应用常量
 */
object Constants {

    /**
     * 默认ADB端口
     */
    const val DEFAULT_ADB_PORT = 5555

    /**
     * 示例IP地址格式
     */
    const val EXAMPLE_IP_FORMAT = "192.168.1.100:5555"

    /**
     * 日志文件最大大小（字节）
     */
    const val MAX_LOG_FILE_SIZE = 1024 * 1024 // 1MB

    /**
     * 缓存清理阈值（字节）
     */
    const val CACHE_CLEANUP_THRESHOLD = 50 * 1024 * 1024 // 50MB
}