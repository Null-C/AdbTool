package com.adbtool.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbtool.commands.InstallCommand
import com.adbtool.commands.ListAppsCommand
import com.adbtool.commands.UninstallCommand
import com.adbtool.data.models.AppInfo
import com.adbtool.data.models.TransferProgress
import com.adbtool.data.models.TransferState
import com.adbtool.utils.DebugLog
import com.adbtool.utils.FormatUtils
import com.adbtool.utils.CacheUtils
import com.adbtool.utils.SafeExecution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class AppListViewModel : ViewModel() {

    private val _appList = MutableStateFlow<List<AppInfo>>(emptyList())
    val appList: StateFlow<List<AppInfo>> = _appList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _transferProgress = MutableStateFlow(TransferProgress())
    val transferProgress: StateFlow<TransferProgress> = _transferProgress.asStateFlow()

    private val _installResult = MutableStateFlow<Result<String>?>(null)
    val installResult: StateFlow<Result<String>?> = _installResult.asStateFlow()

    private val _uninstallResult = MutableStateFlow<Result<String>?>(null)
    val uninstallResult: StateFlow<Result<String>?> = _uninstallResult.asStateFlow()

    private val _showUninstallConfirm = MutableStateFlow<String?>(null)
    val showUninstallConfirm: StateFlow<String?> = _showUninstallConfirm.asStateFlow()

    private val listAppsCommand = ListAppsCommand()
    private val installCommand = InstallCommand()
    private val uninstallCommand = UninstallCommand()
    private val dadbClient = com.adbtool.adb.DadbClient.getInstance()

    private var lastLoadTime = 0L
    private val LOAD_COOLDOWN = 1000L // 1秒防抖动，允许更频繁刷新

    fun loadAppList() {
        val currentTime = System.currentTimeMillis()

        // 防止重复加载
        if (_isLoading.value) {
            DebugLog.d("AppListViewModel", "App list loading - Skipping duplicate load")
            return
        }

        // 防抖动检查
        if (currentTime - lastLoadTime < LOAD_COOLDOWN) {
            val remainingTime = (LOAD_COOLDOWN - (currentTime - lastLoadTime)) / 1000
            DebugLog.d("AppListViewModel", "Load cooldown, wait ${remainingTime}s")
            return
        }

        lastLoadTime = currentTime

        viewModelScope.launch {
            _isLoading.value = true
            SafeExecution.executeWithErrorHandling(
                updateErrorState = { _ ->
                    // 可以在这里添加错误状态更新逻辑
                },
                operation = {
                    val result = listAppsCommand.execute()
                    if (result.isSuccess) {
                        val apps = result.getOrNull() ?: emptyList()
                        _appList.value = apps
                        DebugLog.i("AppListViewModel", "App list loaded - ${apps.size} apps")
                        apps.size
                    } else {
                        val error = result.exceptionOrNull()
                        throw error ?: Exception("Failed to load app list")
                    }
                },
                componentName = "AppListViewModel",
                operationDescription = "Loading device app list"
            ) // 在成功时记录额外日志

            _isLoading.value = false
        }
    }

    fun installApk(apkPath: String, context: Context) {
        viewModelScope.launch {
            DebugLog.i("AppListViewModel", "Starting APK installation - Path: $apkPath")
            val file = File(apkPath)
            if (!file.exists()) {
                val error = Exception("APK file not found")
                DebugLog.e("AppListViewModel", "APK file not found: $apkPath")
                _installResult.value = Result.failure(error)
                return@launch
            }
            DebugLog.d("AppListViewModel", "APK file info - Size: ${file.length()} bytes")

            _transferProgress.value = TransferProgress(
                state = TransferState.TRANSFERRING,
                totalBytes = file.length()
            )

            try {
                val result = installCommand.execute(file) { progress ->
                    _transferProgress.value = progress

                    // 每传输50%记录一次日志
                    val progressPercent = progress.progressPercent
                    if (progressPercent == 50 || progressPercent % 50 == 0) {
                        DebugLog.d("AppListViewModel", "Transfer progress: $progressPercent%")
                    }
                }

                if (result.isSuccess) {
                    DebugLog.i("AppListViewModel", "APK installation completed successfully")

                    // 清理本机缓存中的临时APK文件
                    DebugLog.d("AppListViewModel", "Cleaning cache files - Post-install cleanup")
                    clearCacheFilesInternal(context)

                    _transferProgress.value = _transferProgress.value.copy(
                        state = TransferState.COMPLETED
                    )
                    _installResult.value = Result.success("Installation successful")
                    loadAppList() // 刷新应用列表
                } else {
                    val error = result.exceptionOrNull()
                    DebugLog.e("AppListViewModel", "APK installation failed", error)

                    // 即使安装失败也清理本机缓存
                    DebugLog.d("AppListViewModel", "Cleaning cache files - Post-failure cleanup")
                    clearCacheFilesInternal(context)

                    _transferProgress.value = _transferProgress.value.copy(
                        state = TransferState.ERROR,
                        errorMessage = error?.message ?: "Installation failed"
                    )
                    _installResult.value = Result.failure(error ?: Exception("Installation failed"))
                }
            } catch (e: Exception) {
                DebugLog.e("AppListViewModel", "APK installation exception", e)

                // 异常情况下也清理本机缓存
                clearCacheFilesInternal(context)

                _transferProgress.value = _transferProgress.value.copy(
                    state = TransferState.ERROR,
                    errorMessage = e.message ?: "Install failed"
                )
                _installResult.value = Result.failure(e)
            }
        }
    }

    fun confirmUninstall(packageName: String) {
        _showUninstallConfirm.value = packageName
    }

    fun dismissUninstallConfirm() {
        _showUninstallConfirm.value = null
    }

    fun uninstallApp(packageName: String) {
        viewModelScope.launch {
            DebugLog.i("AppListViewModel", "Starting app uninstall - Package: $packageName")
            SafeExecution.executeWithErrorHandling(
                updateErrorState = { errorMessage ->
                    _uninstallResult.value = Result.failure(Exception(errorMessage))
                },
                operation = {
                    val result = uninstallCommand.execute(packageName)
                    _uninstallResult.value = result
                    _showUninstallConfirm.value = null

                    if (result.isSuccess) {
                        DebugLog.i("AppListViewModel", "App uninstall completed - $packageName")
                        loadAppList() // 刷新应用列表
                        true
                    } else {
                        val error = result.exceptionOrNull()
                        throw error ?: Exception("Uninstall failed")
                    }
                },
                componentName = "AppListViewModel",
                operationDescription = "Uninstalling app $packageName"
            )
        }
    }

    fun clearInstallResult() {
        _installResult.value = null
        _transferProgress.value = TransferProgress()
    }

    fun clearUninstallResult() {
        _uninstallResult.value = null
    }

    /**
     * 清理目标设备的缓存APK文件
     */
    fun cleanTargetDeviceCache() {
        viewModelScope.launch {
            DebugLog.i("AppListViewModel", "Manual remote device cache cleanup - Starting")
            CacheUtils.cleanTargetDeviceApkCache(dadbClient, "AppListViewModel")
        }
    }

    /**
     * 手动清理源设备缓存目录中的APK文件
     */
    fun clearCacheFiles(context: Context) {
        viewModelScope.launch {
            DebugLog.i("AppListViewModel", "Manual local cache cleanup - Starting")
            CacheUtils.clearApkCacheFiles(context, "AppListViewModel")
        }
    }

    /**
     * 内部缓存清理方法（用于安装完成后自动清理）
     */
    private fun clearCacheFilesInternal(context: Context) {
        val (cleanedCount, cleanedSize) = CacheUtils.clearApkCacheFiles(context, "AppListViewModel")
        if (cleanedCount > 0) {
            DebugLog.i("AppListViewModel", "Post-install cache cleanup completed - Cleaned $cleanedCount files, freed ${FormatUtils.formatFileSize(cleanedSize)}")
        }
    }

    
    
    /**
     * 从SAF URI安装APK文件
     */
    fun installApkFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            DebugLog.i("AppListViewModel", "Starting APK installation from URI - URI: $uri")

            try {
                val path = getRealPathFromUri(context, uri)
                DebugLog.i("AppListViewModel", "APK path resolved - Path: $path")

                if (path.isNotEmpty()) {
                    installApk(path, context)
                } else {
                    val error = Exception("Unable to resolve URI to file path")
                    DebugLog.e("AppListViewModel", "URI resolution failed - URI: $uri", error)
                    _installResult.value = Result.failure(error)
                }
            } catch (e: Exception) {
                DebugLog.e("AppListViewModel", "Exception during URI processing", e)
                _installResult.value = Result.failure(e)
            }
        }
    }

    /**
     * 从SAF URI获取实际文件路径，如果无法获取则复制到临时文件并返回临时文件路径
     */
    private fun getRealPathFromUri(context: Context, uri: Uri): String {
        return try {
            // 首先尝试直接获取文件路径
            uri.path?.let { path ->
                // 检查是否是实际的文件路径
                if (path.startsWith("/")) {
                    val file = File(path)
                    if (file.exists()) {
                        DebugLog.i("AppListViewModel", "Found existing file at path: $path")
                        return path
                    }
                }
            }

            // 如果直接路径不可用，尝试从ContentResolver获取显示名称
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val fileName = it.getString(nameIndex)
                    DebugLog.i("AppListViewModel", "Retrieved file name: $fileName")

                    // 复制文件到应用缓存目录
                    val tempFile = File(context.cacheDir, fileName)
                    copyUriToFile(context, uri, tempFile)

                    val realPath = tempFile.absolutePath
                    DebugLog.i("AppListViewModel", "Copied file to temporary path: $realPath")
                    return realPath
                }
            }

            // 最后尝试：复制到默认临时文件名
            val tempFile = File(context.cacheDir, "temp_selected.apk")
            copyUriToFile(context, uri, tempFile)

            val realPath = tempFile.absolutePath
            DebugLog.i("AppListViewModel", "Copied to fallback temporary path: $realPath")
            realPath
        } catch (e: Exception) {
            DebugLog.e("AppListViewModel", "Failed to resolve URI to file path", e)
            ""
        }
    }

    /**
     * 将URI内容复制到文件
     */
    private fun copyUriToFile(context: Context, uri: Uri, targetFile: File) {
        context.contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
            FileOutputStream(targetFile).use { outputStream ->
                inputStream.copyTo(outputStream)
                outputStream.flush()
            }
        } ?: throw Exception("Failed to open input stream for URI: $uri")
    }
}