package com.adbtool.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 安全执行工具类，统一错误处理逻辑
 */
object SafeExecution {

    /**
     * 安全执行操作，自动处理异常并更新错误状态
     * @param stateFlow 错误状态流
     * @param operation 要执行的操作
     * @param componentName 组件名称，用于日志记录
     * @param operationDescription 操作描述，用于日志记录
     * @return 操作结果
     */
    suspend fun <T> executeWithErrorHandling(
        stateFlow: MutableStateFlow<com.adbtool.data.models.ConnectionState>,
        operation: suspend () -> T,
        componentName: String,
        operationDescription: String
    ): Result<T> {
        return try {
            DebugLog.i(componentName, "$operationDescription - Starting")
            val result = operation()
            DebugLog.i(componentName, "$operationDescription - Success")
            Result.success(result)
        } catch (e: Exception) {
            DebugLog.e(componentName, "$operationDescription - Failed", e)
            val errorMessage = e.message ?: "$operationDescription failed"
            stateFlow.value = com.adbtool.data.models.ConnectionState(
                com.adbtool.data.models.ConnectionStatus.ERROR,
                errorMessage = errorMessage
            )
            Result.failure(e)
        }
    }

    /**
     * 通用版本的状态更新
     */
    suspend fun <T> executeWithErrorHandling(
        updateErrorState: (String) -> Unit,
        operation: suspend () -> T,
        componentName: String,
        operationDescription: String
    ): Result<T> {
        return try {
            DebugLog.i(componentName, "$operationDescription - Starting")
            val result = operation()
            DebugLog.i(componentName, "$operationDescription - Success")
            Result.success(result)
        } catch (e: Exception) {
            DebugLog.e(componentName, "$operationDescription - Failed", e)
            val errorMessage = e.message ?: "$operationDescription failed"
            updateErrorState(errorMessage)
            Result.failure(e)
        }
    }
}

/**
 * Result扩展函数，简化错误处理
 */
fun <T> Result<T>.logOnFailure(componentName: String, operation: String): Result<T> {
    onFailure { exception ->
        DebugLog.e(componentName, "$operation - Failed", exception)
    }
    return this
}

/**
 * Result扩展函数，成功时记录日志
 */
fun <T> Result<T>.logOnSuccess(componentName: String, operation: String): Result<T> {
    onSuccess {
        DebugLog.i(componentName, "$operation - Success")
    }
    return this
}