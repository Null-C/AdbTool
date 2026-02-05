package com.adbtool.adb

import com.adbtool.utils.DebugLog
import com.adbtool.utils.CacheUtils
import dadb.Dadb
import dadb.AdbKeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.io.path.createTempDirectory
import android.content.Context
import java.util.concurrent.Executors
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class DadbClient private constructor() {

    private var dadbInstance: Dadb? = null
    private var _isConnected = false
    private var keyPair: AdbKeyPair? = null

    companion object {
        @Volatile
        private var instance: DadbClient? = null

        fun getInstance(): DadbClient {
            return instance ?: synchronized(this) {
                instance ?: DadbClient().also { instance = it }
            }
        }
    }

    suspend fun connect(host: String, port: Int = 5555, context: Context? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Disconnect any existing connection first
                if (dadbInstance != null) {
                    disconnect()
                }

                // Use ExecutorService for truly interruptible connection creation
                val executor = Executors.newSingleThreadExecutor()
                val future = executor.submit(Callable {
                    val keyPair = getOrCreateAdbKeyPair(context)

                    // Create connection in separate thread for better timeout control
                    val connection = Dadb.create(host, port, keyPair)

                    // Quick validation
                    val validationResult = try {
                        val response = connection.shell("echo 'connection_test'")
                        response.allOutput.trim()
                    } catch (e: Exception) {
                        null
                    }

                    if (validationResult == "connection_test") {
                        connection
                    } else {
                        throw Exception("Connection validation failed")
                    }
                })

                try {
                    // Use 5-second timeout as requested
                    val result = future.get(5, TimeUnit.SECONDS)

                    // Connection successful
                    dadbInstance = result
                    _isConnected = true
                    DebugLog.i("DadbClient", "Connected successfully to $host:$port")
                    Result.success("$host:$port")
                } catch (e: TimeoutException) {
                    DebugLog.e("DadbClient", "Connection timed out after 5 seconds")
                    future.cancel(true)
                    _isConnected = false
                    Result.failure(Exception("Connection timeout - no response within 5 seconds"))
                } catch (e: Exception) {
                    DebugLog.e("DadbClient", "Connection failed", e)
                    _isConnected = false
                    Result.failure(e)
                } finally {
                    executor.shutdown()
                }
            } catch (e: Exception) {
                DebugLog.e("DadbClient", "Connection setup failed", e)
                _isConnected = false
                dadbInstance = null
                Result.failure(e)
            }
        }
    }

    
    suspend fun disconnect(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                dadbInstance?.close()
                dadbInstance = null
                _isConnected = false
                DebugLog.i("DadbClient", "Successfully disconnected")
                Result.success(Unit)
            } catch (e: Exception) {
                DebugLog.e("DadbClient", "DadbClient.disconnect failed", e)
                dadbInstance = null
                _isConnected = false
                Result.failure(e)
            }
        }
    }

    fun isConnected(): Boolean {
        return _isConnected && dadbInstance != null
    }

    suspend fun executeCommand(command: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {

                if (!isConnected()) {
                    return@withContext Result.failure(Exception("Not connected to device"))
                }

                val dadb = dadbInstance!!
                val response = dadb.shell(command)
                val output = response.allOutput

                Result.success(output.trim())
            } catch (e: Exception) {
                DebugLog.e("DadbClient", "Command execution failed", e)
                Result.failure(e)
            }
        }
    }

    suspend fun installApk(apkFile: File): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isConnected()) {
                    return@withContext Result.failure(Exception("Not connected to device"))
                }

                if (!apkFile.exists()) {
                    DebugLog.e("DadbClient", "APK file not found: ${apkFile.absolutePath}")
                    return@withContext Result.failure(Exception("APK file not found: ${apkFile.absolutePath}"))
                }

                val dadb = dadbInstance!!
                dadb.install(apkFile)

                // 保险清理：确保目标设备上的临时APK文件被删除
                CacheUtils.cleanTargetDeviceApkCache(this@DadbClient, "DadbClient")

                Result.success(Unit)
            } catch (e: Exception) {
                DebugLog.e("DadbClient", "APK installation failed", e)

                // 即使安装失败也尝试清理临时文件
                try {
                    CacheUtils.cleanTargetDeviceApkCache(this@DadbClient, "DadbClient")
                } catch (cleanupException: Exception) {
                    DebugLog.e("DadbClient", "Cleanup after failed install also failed", cleanupException)
                }

                Result.failure(e)
            }
        }
    }

    suspend fun uninstallApp(packageName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {

                if (!isConnected()) {
                    return@withContext Result.failure(Exception("Not connected to device"))
                }

                if (packageName.isBlank()) {
                    DebugLog.e("DadbClient", "Package name is empty")
                    return@withContext Result.failure(Exception("Package name cannot be empty"))
                }

                val dadb = dadbInstance!!
                dadb.uninstall(packageName)
                Result.success(Unit)
            } catch (e: Exception) {
                DebugLog.e("DadbClient", "App uninstallation failed", e)
                Result.failure(e)
            }
        }
    }

    suspend fun listThirdPartyApps(): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {

                if (!isConnected()) {
                    return@withContext Result.failure(Exception("Not connected to device"))
                }

                val dadb = dadbInstance!!
                val response = dadb.shell("pm list packages -3")
                val output = response.allOutput

                val packageNames = parsePackageList(output)
                DebugLog.i("DadbClient", "Found ${packageNames.size} third-party packages")
                Result.success(packageNames)
            } catch (e: Exception) {
                DebugLog.e("DadbClient", "Listing apps failed", e)
                Result.failure(e)
            }
        }
    }

    suspend fun pushFile(localFile: File, remotePath: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {

                if (!isConnected()) {
                    return@withContext Result.failure(Exception("Not connected to device"))
                }

                if (!localFile.exists()) {
                    DebugLog.e("DadbClient", "Local file not found: ${localFile.absolutePath}")
                    return@withContext Result.failure(Exception("Local file not found: ${localFile.absolutePath}"))
                }

                if (remotePath.isBlank()) {
                    DebugLog.e("DadbClient", "Remote path is empty")
                    return@withContext Result.failure(Exception("Remote path cannot be empty"))
                }

                val dadb = dadbInstance!!
                dadb.push(localFile, remotePath)
                Result.success(Unit)
            } catch (e: Exception) {
                DebugLog.e("DadbClient", "File push failed", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getAppInfo(packageName: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {

                if (!isConnected()) {
                    return@withContext Result.failure(Exception("Not connected to device"))
                }

                if (packageName.isBlank()) {
                    DebugLog.e("DadbClient", "Package name is empty")
                    return@withContext Result.failure(Exception("Package name cannot be empty"))
                }

                val dadb = dadbInstance!!
                val response = dadb.shell("dumpsys package $packageName")
                val output = response.allOutput

                DebugLog.i("DadbClient", "App info retrieved successfully")
                Result.success(output.trim())
            } catch (e: Exception) {
                DebugLog.e("DadbClient", "Getting app info failed", e)
                Result.failure(e)
            }
        }
    }

    private fun parsePackageList(output: String): List<String> {
        val packageNames = mutableListOf<String>()
        val lines = output.split("\n")

        for (line in lines) {
            if (line.startsWith("package:")) {
                val packageName = line.substringAfter("package:").trim()
                if (packageName.isNotEmpty()) {
                    packageNames.add(packageName)
                } else {
                    DebugLog.w("DadbClient", "Empty package name in line: '$line'")
                }
            }
        }

        return packageNames
    }

    /**
     * Creates or loads an ADB key pair. Uses internal storage for persistence.
     */
    private fun getOrCreateAdbKeyPair(context: Context?): AdbKeyPair {
        synchronized(this) {
            // Return cached key pair if available
            keyPair?.let {
                return it
            }

            return try {
                if (context != null) {
                    // Try to load from internal storage
                    val keyFile = File(context.filesDir, "adbkey")
                    val pubFile = File(context.filesDir, "adbkey.pub")

                    if (keyFile.exists() && pubFile.exists()) {
                        DebugLog.i("DadbClient", "Loading ADB key pair from internal storage")

                        val loadedKeyPair = AdbKeyPair.read(keyFile, pubFile)
                        keyPair = loadedKeyPair
                        DebugLog.i("DadbClient", "ADB key pair loaded from internal storage")
                        loadedKeyPair
                    } else {
                        DebugLog.i("DadbClient", "No existing key pair found, generating new one...")
                        generateAndSaveKeyPair(context)
                    }
                } else {
                    // Fallback to temporary directory generation (for tests or backward compatibility)
                    DebugLog.w("DadbClient", "Context is null, using temporary key generation")
                    generateTemporaryKeyPair()
                }
            } catch (e: Exception) {
                DebugLog.e("DadbClient", "Failed to create/load ADB key pair", e)
                throw RuntimeException("Failed to create ADB key pair: ${e.message}", e)
            }
        }
    }

    private fun generateAndSaveKeyPair(context: Context): AdbKeyPair {
        val keyFile = File(context.filesDir, "adbkey")
        val pubFile = File(context.filesDir, "adbkey.pub")


        // Generate key pair directly to internal storage
        AdbKeyPair.generate(keyFile, pubFile)

        // Read and cache the key pair
        val newKeyPair = AdbKeyPair.read(keyFile, pubFile)
        keyPair = newKeyPair

        return newKeyPair
    }

    private fun generateTemporaryKeyPair(): AdbKeyPair {
        // Fallback method for backward compatibility
        val tempPath = createTempDirectory("adb_keys")
        val tempDir = tempPath.toFile()
        val privateKeyFile = File(tempDir, "adbkey")
        val publicKeyFile = File(tempDir, "adbkey.pub")


        AdbKeyPair.generate(privateKeyFile, publicKeyFile)

        val newKeyPair = AdbKeyPair.read(privateKeyFile, publicKeyFile)
        keyPair = newKeyPair

        // Clean up temporary files
        privateKeyFile.delete()
        publicKeyFile.delete()
        tempDir.delete()
        tempPath.toFile().delete()

        DebugLog.i("DadbClient", "ADB key pair loaded and cached (temporary)")
        return newKeyPair
    }

    }