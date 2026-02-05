package com.adbtool.commands

import com.adbtool.adb.DadbClient
import com.adbtool.utils.DebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 基础命令类，提供通用的连接检查和错误处理
 */
abstract class BaseCommand {

    protected val dadbClient = DadbClient.getInstance()

    /**
     * 检查设备连接状态
     */
    protected suspend fun checkConnection(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            if (!dadbClient.isConnected()) {
                DebugLog.e(getCommandName(), "Device not connected")
                Result.failure(Exception("Device not connected"))
            } else {
                Result.success(Unit)
            }
        }
    }

    /**
     * 获取命令名称，用于日志输出
     */
    protected abstract fun getCommandName(): String
}