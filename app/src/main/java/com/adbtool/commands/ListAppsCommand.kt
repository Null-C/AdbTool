package com.adbtool.commands

import com.adbtool.adb.DadbClient
import com.adbtool.data.models.AppInfo
import com.adbtool.utils.DebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ListAppsCommand : BaseCommand() {

    override fun getCommandName(): String = "ListAppsCommand"

    suspend fun execute(): Result<List<AppInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                DebugLog.i(getCommandName(), "Fetching app list...")

                val connectionCheck = checkConnection()
                if (connectionCheck.isFailure) {
                    return@withContext connectionCheck.map { emptyList() }
                }

                val packagesResult = dadbClient.listThirdPartyApps()
                if (packagesResult.isFailure) {
                    val error = packagesResult.exceptionOrNull()
                    DebugLog.e(getCommandName(), "Failed to list packages", error)
                    return@withContext Result.failure(Exception("Failed to list packages: ${error?.message}"))
                }

                val packageNames = packagesResult.getOrThrow()
                DebugLog.i(getCommandName(), "Found ${packageNames.size} third-party packages")

                if (packageNames.isEmpty()) {
                    DebugLog.w(getCommandName(), "No third-party packages found")
                    return@withContext Result.success(emptyList())
                }

                val appList = packageNames.mapNotNull { packageName ->
                    try {
                        getAppInfo(packageName)
                    } catch (e: Exception) {
                        DebugLog.w(getCommandName(), "Error getting info for $packageName: ${e.message}")
                        null
                    }
                }

                DebugLog.i(getCommandName(), "Successfully loaded ${appList.size} apps")
                Result.success(appList)
            } catch (e: Exception) {
                DebugLog.e(getCommandName(), "Failed to fetch app list", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun getAppInfo(packageName: String): AppInfo? {
        return try {
            val infoResult = dadbClient.getAppInfo(packageName)
            if (infoResult.isFailure) {
                DebugLog.w(getCommandName(), "Failed to get app info for $packageName")
                return null
            }

            val dumpsysOutput = infoResult.getOrThrow()
            parseAppInfo(packageName, dumpsysOutput)
        } catch (e: Exception) {
            DebugLog.w(getCommandName(), "Error getting app info for $packageName: ${e.message}")
            null
        }
    }

    private fun parseAppInfo(packageName: String, dumpsysOutput: String): AppInfo {
        val versionName = extractVersionName(dumpsysOutput)
        val versionCode = extractVersionCode(dumpsysOutput)

        return AppInfo(
            packageName = packageName,
            appName = packageName, // Keep as package name per project convention
            versionName = versionName,
            versionCode = versionCode
        )
    }

    private fun extractVersionName(dumpsysOutput: String): String {
        return dumpsysOutput.lineSequence()
            .find { it.contains("versionName=") }
            ?.substringAfter("versionName=")
            ?.trim() ?: "Unknown"
    }

    private fun extractVersionCode(dumpsysOutput: String): String {
        return dumpsysOutput.lineSequence()
            .find { it.contains("versionCode=") }
            ?.substringAfter("versionCode=")
            ?.trim() ?: "0"
    }
}