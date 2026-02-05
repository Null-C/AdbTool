package com.adbtool.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adbtool.adb.DadbClient
import com.adbtool.data.models.ConnectionState
import com.adbtool.data.models.ConnectionStatus
import com.adbtool.utils.DebugLog
import com.adbtool.utils.SafeExecution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectViewModel : ViewModel() {

    private val _connectionState = MutableStateFlow(ConnectionState(ConnectionStatus.DISCONNECTED))
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _ipAddress = MutableStateFlow("")
    val ipAddress: StateFlow<String> = _ipAddress.asStateFlow()

    private val dadbClient = DadbClient.getInstance()

    fun connect(ipAddress: String, context: Context? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _connectionState.value = ConnectionState(ConnectionStatus.CONNECTING)

            SafeExecution.executeWithErrorHandling(
                stateFlow = _connectionState,
                operation = {
                    val (host, port) = parseAddress(ipAddress)
                    val result = dadbClient.connect(host, port, context)

                    if (result.isSuccess) {
                        val deviceAddress = result.getOrNull() ?: ipAddress
                        _connectionState.value = ConnectionState(
                            ConnectionStatus.CONNECTED,
                            deviceAddress = deviceAddress
                        )
                        deviceAddress
                    } else {
                        val error = result.exceptionOrNull()
                        throw error ?: Exception("Connection failed")
                    }
                },
                componentName = "ConnectViewModel",
                operationDescription = "Connecting to device $ipAddress"
            )

            _isLoading.value = false
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            SafeExecution.executeWithErrorHandling(
                stateFlow = _connectionState,
                operation = {
                    val result = dadbClient.disconnect()
                    if (result.isSuccess) {
                        _connectionState.value = ConnectionState(ConnectionStatus.DISCONNECTED)
                    } else {
                        val error = result.exceptionOrNull()
                        throw error ?: Exception("Disconnect failed")
                    }
                },
                componentName = "ConnectViewModel",
                operationDescription = "Disconnecting device"
            )
        }
    }

    fun clearError() {
        if (_connectionState.value.status == ConnectionStatus.ERROR) {
            _connectionState.value = ConnectionState(ConnectionStatus.DISCONNECTED)
        }
    }

    fun updateIpAddress(address: String) {
        _ipAddress.value = address
    }

    private fun parseAddress(address: String): Pair<String, Int> {
        val parts = address.split(":")
        return if (parts.size == 2) {
            val host = parts[0].trim()
            val port = parts[1].trim().toIntOrNull() ?: 5555
            Pair(host, port)
        } else {
            Pair(address.trim(), 5555)
        }
    }
}