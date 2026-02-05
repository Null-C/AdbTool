package com.adbtool.commands

import com.adbtool.adb.DadbClient
import com.adbtool.utils.DebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UninstallCommand : BaseCommand() {

    override fun getCommandName(): String = "UninstallCommand"

    suspend fun execute(packageName: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                DebugLog.i(getCommandName(), "Starting app uninstallation: $packageName")

                val connectionCheck = checkConnection()
                if (connectionCheck.isFailure) {
                    val error = connectionCheck.exceptionOrNull()
                    return@withContext Result.failure(error ?: Exception("Device not connected"))
                }

                if (packageName.isBlank()) {
                    DebugLog.e(getCommandName(), "Package name is empty")
                    return@withContext Result.failure(Exception("Package name cannot be empty"))
                }

                // Use DadbClient's uninstallApp method
                val uninstallResult = dadbClient.uninstallApp(packageName)

                if (uninstallResult.isSuccess) {
                    DebugLog.i(getCommandName(), "App uninstallation successful: $packageName")
                    Result.success("Uninstall successful")
                } else {
                    val error = uninstallResult.exceptionOrNull()
                    val errorMessage = error?.message ?: "Uninstall failed"
                    DebugLog.e(getCommandName(), "App uninstallation failed", error)
                    Result.failure(error ?: Exception(errorMessage))
                }
            } catch (e: Exception) {
                DebugLog.e(getCommandName(), "Exception during uninstallation", e)
                Result.failure(e)
            }
        }
    }
}