package com.adbtool.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbtool.R
import com.adbtool.data.models.AppInfo
import com.adbtool.data.models.TransferState
import com.adbtool.ui.components.TransferProgressDialog
import com.adbtool.ui.components.UninstallConfirmDialog
import com.adbtool.ui.components.UninstallResultDialog
import com.adbtool.ui.components.LogViewerDialog
import com.adbtool.ui.components.CommonShapes
import com.adbtool.ui.components.LoadingOutlinedButton
import com.adbtool.viewmodel.AppListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    deviceAddress: String,
    onDisconnect: () -> Unit,
    onInstallApk: () -> Unit
) {
    val appList by viewModel.appList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val transferProgress by viewModel.transferProgress.collectAsState()
    val uninstallResult by viewModel.uninstallResult.collectAsState()
    val showUninstallConfirm by viewModel.showUninstallConfirm.collectAsState()

    // 日志查看器状态
    var showLogViewer by remember { mutableStateOf(false) }

    // 自动加载应用列表 - 只在连接状态变化时加载一次
    LaunchedEffect(deviceAddress) {
        if (deviceAddress.isNotEmpty() && appList.isEmpty()) {
            viewModel.loadAppList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.app_name))
                        Text(
                            text = deviceAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                    onClick = { viewModel.loadAppList() },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh_list)
                        )
                    }
                }

                    // 更多选项菜单
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showMenu = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多选项"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("📋")
                                        Text("查看日志")
                                    }
                                },
                                onClick = {
                                    showLogViewer = true
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("🗑️")
                                        Text("清空日志")
                                    }
                                },
                                onClick = {
                                    com.adbtool.utils.DebugLog.clearLogFile()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CleaningServices,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text("清理远程缓存")
                                    }
                                },
                                onClick = {
                                    viewModel.cleanTargetDeviceCache()
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 操作按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onInstallApk,
                    modifier = Modifier.weight(1f),
                    shape = CommonShapes.ButtonShape,
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.install_apk))
                }

                LoadingOutlinedButton(
                    isLoading = false,
                    text = stringResource(R.string.cancel),
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )
            }

            // 应用列表
            if (isLoading && appList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (appList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_apps_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(appList) { app ->
                        AppItem(
                            app = app,
                            onUninstall = { viewModel.confirmUninstall(app.packageName) },
                            isLoading = isLoading
                        )
                    }
                }
            }
        }
    }

    // 卸载确认对话框
    showUninstallConfirm?.let { packageName ->
        val app = appList.find { it.packageName == packageName }
        if (app != null) {
            UninstallConfirmDialog(
                appInfo = app,
                onConfirm = { viewModel.uninstallApp(packageName) },
                onDismiss = { viewModel.dismissUninstallConfirm() }
            )
        }
    }

    // 卸载结果提示对话框
    uninstallResult?.let { result ->
        UninstallResultDialog(
            result = result,
            onDismiss = {
                viewModel.clearUninstallResult()
                viewModel.dismissUninstallConfirm()
            }
        )
    }

    // 传输进度对话框
    if (transferProgress.state != TransferState.IDLE) {
        TransferProgressDialog(
            progress = transferProgress,
            onClose = {
                viewModel.clearInstallResult()
            }
        )
    }

    // 日志查看器对话框
    if (showLogViewer) {
        LogViewerDialog(
            onDismiss = { showLogViewer = false }
        )
    }
    }

@Composable
fun AppItem(
    app: AppInfo,
    onUninstall: () -> Unit,
    isLoading: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CommonShapes.ButtonShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 显示版本号
                if (app.versionName.isNotEmpty() && app.versionName != "Unknown") {
                    Text(
                        text = "v${app.versionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = onUninstall,
                modifier = Modifier.size(40.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.uninstall),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}