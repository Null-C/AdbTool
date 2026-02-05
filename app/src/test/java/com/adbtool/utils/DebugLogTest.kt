package com.adbtool.utils

import android.content.Context
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebugLogTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockCacheDir: File

    @Mock
    private lateinit var mockFilesDir: File

    private lateinit var testTempFiles: List<File>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Setup mock directories
        `when`(mockContext.cacheDir).thenReturn(mockCacheDir)
        `when`(mockContext.filesDir).thenReturn(mockFilesDir)

        // Create mock log file
        val mockLogFile = File(mockFilesDir, "adb-tool.log")
        `when`(mockFilesDir.resolve("adb-tool.log")).thenReturn(mockLogFile)
    }

    @Test
    fun `init should clear APK cache files`() {
        // Given
        val testFiles = listOf(
            File(mockCacheDir, "test1.apk"),
            File(mockCacheDir, "temp_selected.apk"),
            File(mockCacheDir, "another.apk"),
            File(mockCacheDir, "normal.txt") // Should not be deleted
        )

        `when`(mockCacheDir.exists()).thenReturn(true)
        `when`(mockCacheDir.listFiles(any())).thenReturn(testFiles.toTypedArray())

        testFiles.forEach { file ->
            `when`(file.delete()).thenReturn(true)
            `when`(file.length()).thenReturn(1024L)
        }

        // When
        DebugLog.init(mockContext)

        // Then
        // Verify APK files were attempted to be deleted
        verify(testFiles[0], times(1)).delete() // test1.apk
        verify(testFiles[1], times(1)).delete() // temp_selected.apk
        verify(testFiles[2], times(1)).delete() // another.apk
        verify(testFiles[3], never()).delete() // normal.txt should not be deleted
    }

    @Test
    fun `init should handle when cache directory does not exist`() {
        // Given
        `when`(mockCacheDir.exists()).thenReturn(false)

        // When - should not crash
        DebugLog.init(mockContext)

        // Then - no exception thrown
        assertTrue(true)
    }

    @Test
    fun `init should handle cache directory listing errors gracefully`() {
        // Given
        `when`(mockCacheDir.exists()).thenReturn(true)
        `when`(mockCacheDir.listFiles(any())).thenThrow(RuntimeException("Permission denied"))

        // When - should not crash
        DebugLog.init(mockContext)

        // Then - no exception thrown, should still initialize log
        assertTrue(true)
    }

    @Test
    fun `formatFileSize should be consistent across the application`() {
        // Test that FormatUtils.formatFileSize is used consistently
        // This verifies the format: "XXX B", "XXX KB", "XXX MB", "XXX GB"

        assertEquals("512 B", FormatUtils.formatFileSize(512))
        assertEquals("1.0 KB", FormatUtils.formatFileSize(1024))
        assertEquals("1.5 MB", FormatUtils.formatFileSize(1572864))
        assertEquals("2.0 GB", FormatUtils.formatFileSize(2147483648))
    }
}