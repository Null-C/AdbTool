package com.adbtool.data.models

enum class TransferState {
    IDLE,
    TRANSFERRING,
    PAUSED,
    COMPLETED,
    ERROR
}

data class TransferProgress(
    val state: TransferState = TransferState.IDLE,
    val bytesTransferred: Long = 0,
    val totalBytes: Long = 0,
    val speed: Long = 0,  // bytes per second
    val estimatedSecondsRemaining: Int = 0,
    val errorMessage: String? = null
) {
    val progress: Float
        get() = if (totalBytes > 0) {
            bytesTransferred.toFloat() / totalBytes.toFloat()
        } else {
            0f
        }

    val progressPercent: Int
        get() = (progress * 100).toInt()
}