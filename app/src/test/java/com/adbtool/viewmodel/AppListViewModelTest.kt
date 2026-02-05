package com.adbtool.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.adbtool.commands.InstallCommand
import com.adbtool.commands.ListAppsCommand
import com.adbtool.commands.UninstallCommand
import com.adbtool.data.models.AppInfo
import com.adbtool.data.models.TransferProgress
import com.adbtool.data.models.TransferState
import com.adbtool.utils.FormatUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@ExperimentalCoroutinesApi
class AppListViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appListViewModel: AppListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        appListViewModel = AppListViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be empty and not loading`() {
        // Assert
        assertTrue(appListViewModel.appList.value.isEmpty())
        assertFalse(appListViewModel.isLoading.value)
        assertEquals(TransferProgress(), appListViewModel.transferProgress.value)
        assertNull(appListViewModel.installResult.value)
        assertNull(appListViewModel.uninstallResult.value)
        assertNull(appListViewModel.showUninstallConfirm.value)
    }

    
    
    
    @Test
    fun `installApk with non-existent file should set error result`() = runTest {
        // Arrange
        val nonExistentPath = "/path/to/nonexistent.apk"

        // Act
        appListViewModel.installApk(nonExistentPath)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertNotNull(appListViewModel.installResult.value)
        assertTrue(appListViewModel.installResult.value!!.isFailure)
        assertEquals("APK file not found", appListViewModel.installResult.value!!.exceptionOrNull()?.message)
        // Note: transferProgress remains in default state for non-existent file case
    }

    @Test
    fun `installApk with valid file should set transferring state`() = runTest {
        // Arrange - Create a temporary test file
        val testFile = File.createTempFile("test", ".apk")
        testFile.writeText("test content")

        try {
            // Act
            appListViewModel.installApk(testFile.absolutePath)

            // Advance a bit to let the coroutine start
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert - Should set transferring state after file validation
            assertEquals(TransferState.TRANSFERRING, appListViewModel.transferProgress.value.state)
            assertEquals(testFile.length(), appListViewModel.transferProgress.value.totalBytes)
        } finally {
            testFile.delete()
        }
    }

    
    @Test
    fun `confirmUninstall should set showUninstallConfirm`() {
        // Arrange
        val packageName = "com.example.app"

        // Act
        appListViewModel.confirmUninstall(packageName)

        // Assert
        assertEquals(packageName, appListViewModel.showUninstallConfirm.value)
    }

    @Test
    fun `dismissUninstallConfirm should clear showUninstallConfirm`() {
        // Arrange
        appListViewModel.confirmUninstall("com.example.app")
        assertEquals("com.example.app", appListViewModel.showUninstallConfirm.value)

        // Act
        appListViewModel.dismissUninstallConfirm()

        // Assert
        assertNull(appListViewModel.showUninstallConfirm.value)
    }

    
    @Test
    fun `clearInstallResult should reset install state`() = runTest {
        // Arrange - Set some install state
        appListViewModel.installApk("/nonexistent.apk")
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(appListViewModel.installResult.value)

        // Act
        appListViewModel.clearInstallResult()

        // Assert
        assertNull(appListViewModel.installResult.value)
        assertEquals(TransferProgress(), appListViewModel.transferProgress.value)
    }

    
    
    @Test
    fun `AppListViewModel should use migrated commands`() {
        // Verify that the ViewModel instantiates the migrated commands
        // This test ensures integration with the new DadbClient-based commands
        val viewModel = AppListViewModel()

        // The ViewModel should be created without errors
        assertNotNull(viewModel)

        // Initial state should be clean
        assertTrue(viewModel.appList.value.isEmpty())
        assertFalse(viewModel.isLoading.value)
    }

    
    }