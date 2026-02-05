package com.adbtool.utils

import android.content.Context
import com.adbtool.adb.DadbClient
import org.junit.Test
import org.junit.Before
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class CacheUtilsTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockCacheDir: File

    @Mock
    private lateinit var mockDadbClient: DadbClient

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.cacheDir).thenReturn(mockCacheDir)
    }

    @Test
    fun `clearApkCacheFiles should clean APK and temp_selected files`() {
        // Given
        val testFiles = listOf(
            File(mockCacheDir, "test1.apk"),
            File(mockCacheDir, "temp_selected.apk"),
            File(mockCacheDir, "normal.txt"), // Should not be deleted
            File(mockCacheDir, "app.apk")
        )

        `when`(mockCacheDir.exists()).thenReturn(true)
        `when`(mockCacheDir.listFiles(any())).thenReturn(testFiles.toTypedArray())

        // Mock file operations
        testFiles.forEach { file ->
            `when`(file.delete()).thenReturn(true)
            `when`(file.length()).thenReturn(1024L)
            `when`(file.isFile).thenReturn(true)
        }

        // When
        val result = CacheUtils.clearApkCacheFiles(mockContext, "TestCacheUtils")

        // Then
        assertEquals(3, result.first) // Should delete 3 APK files
        assertEquals(4096L, result.second) // Total cleaned size

        // Verify APK files were deleted
        verify(testFiles[0]).delete() // test1.apk
        verify(testFiles[1]).delete() // temp_selected.apk
        verify(testFiles[3]).delete() // app.apk
        verify(testFiles[2], never()).delete() // normal.txt should not be deleted
    }

    @Test
    fun `clearApkCacheFiles should handle non-existent cache directory`() {
        // Given
        `when`(mockCacheDir.exists()).thenReturn(false)

        // When
        val result = CacheUtils.clearApkCacheFiles(mockContext, "TestCacheUtils")

        // Then
        assertEquals(0, result.first)
        assertEquals(0L, result.second)
    }

    @Test
    fun `clearApkCacheFiles should handle file deletion failures gracefully`() {
        // Given
        val testFile = File(mockCacheDir, "test.apk")
        `when`(mockCacheDir.exists()).thenReturn(true)
        `when`(mockCacheDir.listFiles(any())).thenReturn(arrayOf(testFile))
        `when`(testFile.delete()).thenReturn(false) // Deletion fails
        `when`(testFile.length()).thenReturn(1024L)
        `when`(testFile.isFile).thenReturn(true)

        // When
        val result = CacheUtils.clearApkCacheFiles(mockContext, "TestCacheUtils")

        // Then
        assertEquals(0, result.first) // No files cleaned
        assertEquals(0L, result.second)
    }

    @Test
    fun `cleanTargetDeviceApkCache should return false when no device connected`() = runTest {
        // Given
        `when`(mockDadbClient.isConnected()).thenReturn(false)

        // When
        val result = CacheUtils.cleanTargetDeviceApkCache(mockDadbClient, "TestCacheUtils")

        // Then
        assertFalse(result)
        verify(mockDadbClient, never()).executeCommand(any())
    }

    @Test
    fun `cleanTargetDeviceApkCache should handle no APK files case`() = runTest {
        // Given
        `when`(mockDadbClient.isConnected()).thenReturn(true)
        `when`(mockDadbClient.executeCommand(any()))
            .thenReturn(Result.success("No APK files found"))

        // When
        val result = CacheUtils.cleanTargetDeviceApkCache(mockDadbClient, "TestCacheUtils")

        // Then
        assertTrue(result) // Still successful since no files to clean
    }

    @Test
    fun `cleanTargetDeviceApkCache should clean APK files successfully`() = runTest {
        // Given
        `when`(mockDadbClient.isConnected()).thenReturn(true)
        `when`(mockDadbClient.executeCommand(any()))
            .thenReturn(Result.success("file1.apk\nfile2.apk")) // List files first
            .thenReturn(Result.success("")) // Clean command succeeds
            .thenReturn(Result.success("0")) // Verify shows 0 files

        // When
        val result = CacheUtils.cleanTargetDeviceApkCache(mockDadbClient, "TestCacheUtils")

        // Then
        assertTrue(result)
        verify(mockDadbClient, times(3)).executeCommand(any())
    }
}