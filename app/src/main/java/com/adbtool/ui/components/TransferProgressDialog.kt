package com.adbtool.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adbtool.R
import com.adbtool.data.models.TransferProgress
import com.adbtool.data.models.TransferState
import com.adbtool.utils.FormatUtils

@Composable
fun TransferProgressDialog(
    progress: TransferProgress,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                text = stringResource(R.string.installing),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 进度条
                LinearProgressIndicator(
                    progress = progress.progress,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 进度百分比
                Text(
                    text = "${progress.progressPercent}%",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.End)
                )

                // 已传输大小
                Text(
                    text = stringResource(
                        R.string.transferred,
                        FormatUtils.formatFileSize(progress.bytesTransferred),
                        FormatUtils.formatFileSize(progress.totalBytes)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )

                // 传输速度
                if (progress.speed > 0) {
                    Text(
                        text = stringResource(
                            R.string.speed,
                            FormatUtils.formatSpeed(progress.speed)
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // 剩余时间
                if (progress.estimatedSecondsRemaining > 0) {
                    Text(
                        text = stringResource(
                            R.string.remaining,
                            FormatUtils.formatTimeRemaining(progress.estimatedSecondsRemaining)
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // 状态信息
                when (progress.state) {
                    TransferState.TRANSFERRING -> {
                        Text(
                            text = stringResource(R.string.transferring),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TransferState.COMPLETED -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.install_success),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = "应用已成功安装到设备",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    TransferState.ERROR -> {
                        Text(
                            text = progress.errorMessage ?: stringResource(R.string.install_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                enabled = progress.state != TransferState.TRANSFERRING
            ) {
                Text(
                    text = when (progress.state) {
                        TransferState.COMPLETED -> stringResource(R.string.confirm)
                        TransferState.ERROR -> stringResource(R.string.close)
                        else -> stringResource(R.string.cancel)
                    }
                )
            }
        },
        shape = RoundedCornerShape(12.dp)
    )
}

