package com.adbtool.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbtool.R
import com.adbtool.R.string
import com.adbtool.data.models.ConnectionStatus
import com.adbtool.viewmodel.ConnectViewModel
import com.adbtool.ui.components.LogViewerDialog
import com.adbtool.ui.components.LoadingButton
import com.adbtool.ui.components.CommonShapes
import com.adbtool.ui.components.ErrorMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(viewModel: ConnectViewModel) {
    val context = LocalContext.current
    val ipAddress by viewModel.ipAddress.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 日志查看器状态
    var showLogViewer by remember { mutableStateOf(false) }

    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = ipAddress,
            onValueChange = { viewModel.updateIpAddress(it) },
            label = { Text(stringResource(R.string.device_ip_address)) },
            placeholder = { Text("192.168.1.100:5555") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = CommonShapes.ButtonShape,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.default_port),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        LoadingButton(
            isLoading = isLoading,
            text = stringResource(R.string.connect_device),
            onClick = {
                if (ipAddress.isNotBlank()) {
                    // 清除之前的错误状态
                    viewModel.clearError()
                    viewModel.connect(ipAddress, context)
                }
            },
            modifier = Modifier.height(50.dp),
            enabled = ipAddress.isNotBlank()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 错误提示
        connectionState.errorMessage?.let { errorMessage ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = CommonShapes.CardShape
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showLogViewer = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📋 查看详细日志")
                    }
                }
            }
        }
    }

    // 日志查看器对话框
    if (showLogViewer) {
        LogViewerDialog(
            onDismiss = { showLogViewer = false }
        )
    }
}