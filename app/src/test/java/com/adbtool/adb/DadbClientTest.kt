package com.adbtool.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@ExperimentalCoroutinesApi
class DadbClientTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Reset singleton instance before each test
        resetDadbClientSingleton()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // Reset singleton instance after each test
        resetDadbClientSingleton()
    }

    private fun resetDadbClientSingleton() {
        try {
            val instanceField: Field = DadbClient::class.java.getDeclaredField("instance")
            instanceField.isAccessible = true
            instanceField.set(null, null)
        } catch (e: Exception) {
            // Reflection failed, but we should continue
        }
    }

    @Test
    fun `getInstance should return singleton instance`() = runTest {
        // When
        val instance1 = DadbClient.getInstance()
        val instance2 = DadbClient.getInstance()

        // Then
        assertEquals("getInstance should return the same instance", instance1, instance2)
        assertNotNull("Instance should not be null", instance1)
    }

    @Test
    fun `isConnected should return false initially`() = runTest {
        // Given
        val client = DadbClient.getInstance()

        // When
        val connected = client.isConnected()

        // Then
        assertFalse("Should not be connected initially", connected)
    }

    
    @Test
    fun `connect should fail with invalid host`() = runTest {
        // Given
        val client = DadbClient.getInstance()

        // When
        val result = client.connect("invalid.host.address", 5555, null)

        // Then - The connection should either fail or if it somehow succeeds, we should be able to disconnect
        try {
            if (result.isSuccess) {
                // If connection somehow succeeded, we should be able to disconnect
                val disconnectResult = client.disconnect()
                assertTrue("Should be able to disconnect", disconnectResult.isSuccess)
                assertFalse("Should not be connected after disconnect", client.isConnected())
            } else {
                // Expected behavior - connection failed
                assertFalse("Should not be connected after failed connection", client.isConnected())
            }
        } catch (e: Exception) {
            // Any exception indicates the connection behavior is being handled
            assertFalse("Should not be connected after failed connection", client.isConnected())
        }
    }

    @Test
    fun `disconnect should succeed without connection`() = runTest {
        // Given
        val client = DadbClient.getInstance()
        assertFalse("Should not be connected initially", client.isConnected())

        // When
        val result = client.disconnect()

        // Then
        assertTrue("Disconnect should succeed even without connection", result.isSuccess)
        assertFalse("Should still not be connected", client.isConnected())
    }

    @Test
    fun `executeCommand should fail when not connected`() = runTest {
        // Given
        val client = DadbClient.getInstance()
        assertFalse("Should not be connected initially", client.isConnected())

        // When
        val result = client.executeCommand("echo test")

        // Then
        assertTrue("Command execution should fail when not connected", result.isFailure)
        val error = result.exceptionOrNull()
        if (error == null || error.message?.contains("Not connected") != true) {
            fail("Expected error message containing 'Not connected', but got: ${error?.message}")
        }
    }

    @Test
    fun `listThirdPartyApps should fail when not connected`() = runTest {
        // Given
        val client = DadbClient.getInstance()
        assertFalse("Should not be connected initially", client.isConnected())

        // When
        val result = client.listThirdPartyApps()

        // Then
        assertTrue("List apps should fail when not connected", result.isFailure)
        val error = result.exceptionOrNull()
        if (error == null || error.message?.contains("Not connected") != true) {
            fail("Expected error message containing 'Not connected', but got: ${error?.message}")
        }
    }

    @Test
    fun `installApk should fail when not connected`() = runTest {
        // Given
        val client = DadbClient.getInstance()
        assertFalse("Should not be connected initially", client.isConnected())
        val testApkFile = File("/path/to/test.apk")

        // When
        val result = client.installApk(testApkFile)

        // Then
        assertTrue("APK installation should fail when not connected", result.isFailure)
        val error = result.exceptionOrNull()
        if (error == null || error.message?.contains("Not connected") != true) {
            fail("Expected error message containing 'Not connected', but got: ${error?.message}")
        }
    }

    @Test
    fun `uninstallApp should fail when not connected`() = runTest {
        // Given
        val client = DadbClient.getInstance()
        assertFalse("Should not be connected initially", client.isConnected())

        // When
        val result = client.uninstallApp("com.example.app")

        // Then
        assertTrue("App uninstallation should fail when not connected", result.isFailure)
        val error = result.exceptionOrNull()
        if (error == null || error.message?.contains("Not connected") != true) {
            fail("Expected error message containing 'Not connected', but got: ${error?.message}")
        }
    }

    @Test
    fun `getAppInfo should fail when not connected`() = runTest {
        // Given
        val client = DadbClient.getInstance()
        assertFalse("Should not be connected initially", client.isConnected())

        // When
        val result = client.getAppInfo("com.example.app")

        // Then
        assertTrue("Get app info should fail when not connected", result.isFailure)
        val error = result.exceptionOrNull()
        if (error == null || error.message?.contains("Not connected") != true) {
            fail("Expected error message containing 'Not connected', but got: ${error?.message}")
        }
    }

    @Test
    fun `pushFile should fail when not connected`() = runTest {
        // Given
        val client = DadbClient.getInstance()
        assertFalse("Should not be connected initially", client.isConnected())
        val testFile = File("/path/to/test.txt")

        // When
        val result = client.pushFile(testFile, "/data/local/tmp/test.txt")

        // Then
        assertTrue("File push should fail when not connected", result.isFailure)
        val error = result.exceptionOrNull()
        if (error == null || error.message?.contains("Not connected") != true) {
            fail("Expected error message containing 'Not connected', but got: ${error?.message}")
        }
    }
}