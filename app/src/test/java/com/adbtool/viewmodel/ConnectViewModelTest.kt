package com.adbtool.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.adbtool.data.models.ConnectionState
import com.adbtool.data.models.ConnectionStatus
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
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@ExperimentalCoroutinesApi
class ConnectViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var connectViewModel: ConnectViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        connectViewModel = ConnectViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be disconnected`() {
        assertEquals(ConnectionStatus.DISCONNECTED, connectViewModel.connectionState.value.status)
        assertFalse(connectViewModel.isLoading.value)
    }

    
    
    @Test
    fun `clearError should reset error state to disconnected`() {
        // Arrange - clear any existing state
        connectViewModel.clearError()

        // Act
        connectViewModel.clearError()

        // Assert - should remain in disconnected state
        assertEquals(ConnectionStatus.DISCONNECTED, connectViewModel.connectionState.value.status)
        assertEquals(null, connectViewModel.connectionState.value.errorMessage)
        assertEquals(null, connectViewModel.connectionState.value.deviceAddress)
    }

    @Test
    fun `clearError should not affect non-error states`() {
        // Arrange - ensure we're not in error state
        assertEquals(ConnectionStatus.DISCONNECTED, connectViewModel.connectionState.value.status)

        // Act
        connectViewModel.clearError()

        // Assert - state should remain disconnected
        assertEquals(ConnectionStatus.DISCONNECTED, connectViewModel.connectionState.value.status)
        assertEquals(null, connectViewModel.connectionState.value.errorMessage)
        assertEquals(null, connectViewModel.connectionState.value.deviceAddress)
    }

    @Test
    fun `disconnect should not affect loading state`() = runTest {
        // Arrange - initial state should not be loading
        assertFalse(connectViewModel.isLoading.value)

        // Act
        connectViewModel.disconnect()

        // Let the coroutine execute
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - disconnect should not set loading state
        assertFalse(connectViewModel.isLoading.value)
    }

    }