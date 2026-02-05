package com.adbtool.commands

import com.adbtool.adb.DadbClient
import com.adbtool.data.models.TransferProgress
import com.adbtool.data.models.TransferState
import com.adbtool.utils.DebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class InstallCommand : BaseCommand() {

    override fun getCommandName(): String = "InstallCommand"

    suspend fun execute(
        apkFile: File,
        onProgress: (TransferProgress) -> Unit
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                DebugLog.i(getCommandName(), "Starting APK installation: ${apkFile.absolutePath}")

                val connectionCheck = checkConnection()
                if (connectionCheck.isFailure) {
                    return@withContext connectionCheck.map { }
                }

                if (!apkFile.exists()) {
                    DebugLog.e(getCommandName(), "APK file not found: ${apkFile.absolutePath}")
                    return@withContext Result.failure(Exception("APK file not found"))
                }

                // Report initial progress
                onProgress(
                    TransferProgress(
                        state = TransferState.TRANSFERRING,
                        bytesTransferred = 0,
                        totalBytes = apkFile.length(),
                        speed = 0,
                        estimatedSecondsRemaining = 0
                    )
                )

                // Use DadbClient's simplified installApk method
                val startTime = System.currentTimeMillis()

                val installResult = dadbClient.installApk(apkFile)

                // Calculate final progress for completion
                val endTime = System.currentTimeMillis()
                val totalTimeSeconds = (endTime - startTime) / 1000.0
                val speed = if (totalTimeSeconds > 0) {
                    (apkFile.length() / totalTimeSeconds).toLong()
                } else {
                    apkFile.length()
                }

                if (installResult.isSuccess) {
                    // Report final completed progress
                    onProgress(
                        TransferProgress(
                            state = TransferState.COMPLETED,
                            bytesTransferred = apkFile.length(),
                            totalBytes = apkFile.length(),
                            speed = speed,
                            estimatedSecondsRemaining = 0
                        )
                    )
                    DebugLog.i(getCommandName(), "APK installation successful")
                    Result.success(Unit)
                } else {
                    // Report error progress
                    val error = installResult.exceptionOrNull()
                    val errorMessage = error?.message ?: "Installation failed"
                    onProgress(
                        TransferProgress(
                            state = TransferState.ERROR,
                            bytesTransferred = 0,
                            totalBytes = apkFile.length(),
                            speed = 0,
                            estimatedSecondsRemaining = 0,
                            errorMessage = errorMessage
                        )
                    )
                    DebugLog.e(getCommandName(), "APK installation failed", error)
                    Result.failure(error!!)
                }
            } catch (e: Exception) {
                DebugLog.e(getCommandName(), "Exception during installation", e)

                // Report exception as error
                onProgress(
                    TransferProgress(
                        state = TransferState.ERROR,
                        bytesTransferred = 0,
                        totalBytes = apkFile.length(),
                        speed = 0,
                        estimatedSecondsRemaining = 0,
                        errorMessage = e.message ?: "Installation failed"
                    )
                )
                Result.failure(e)
            }
        }
    }

    /**
     * Legacy method for backward compatibility.
     * @deprecated Use execute(apkFile: File, onProgress: (TransferProgress) -> Unit) instead
     */
    @Deprecated("Use execute(apkFile: File, onProgress: (TransferProgress) -> Unit) instead")
    suspend fun execute(
        localApkPath: String,
        progressCallback: ((Long, Long) -> Unit)? = null
    ): Result<String> {
        val file = File(localApkPath)
        val result = execute(file) { progress ->
            progressCallback?.invoke(progress.bytesTransferred, progress.totalBytes)
        }

        return if (result.isSuccess) {
            Result.success("Install successful")
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Install failed"))
        }
    }
}
