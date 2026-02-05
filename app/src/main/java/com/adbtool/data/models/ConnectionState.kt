package com.adbtool.data.models

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class ConnectionState(
    val status: ConnectionStatus,
    val deviceAddress: String? = null,
    val errorMessage: String? = null
)