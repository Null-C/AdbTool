package com.adbtool.commands

import org.junit.Test
import org.junit.Assert.*
import java.io.File

class InstallCommandTest {

    @Test
    fun `InstallCommand should be instantiatable`() {
        // Given
        val installCommand = InstallCommand()

        // When
        val result = installCommand

        // Then
        assertNotNull(result)
        assertTrue(result is InstallCommand)
    }

    @Test
    fun `temporary file creation should work`() {
        // Test File.createTempFile functionality to ensure basic file operations work
        val tempFile = File.createTempFile("test", ".apk")

        try {
            assertNotNull(tempFile)
            assertTrue(tempFile.exists())
        } finally {
            tempFile.delete()
        }
    }
}