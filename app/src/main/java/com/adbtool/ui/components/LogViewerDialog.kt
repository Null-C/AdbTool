package com.adbtool.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.adbtool.utils.DebugLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerDialog(
    onDismiss: () -> Unit
) {
    var logContent by remember { mutableStateOf("Loading...") }
    var isRefreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 加载日志内容
    LaunchedEffect(Unit) {
        loadLogContent { logContent = it }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📋 调试日志",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    actions = {
                        // 刷新按钮
                        IconButton(
                            onClick = {
                                isRefreshing = true
                                loadLogContent {
                                    logContent = it
                                    isRefreshing = false
                                }
                            },
                            enabled = !isRefreshing
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "刷新日志",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // 复制按钮
                        IconButton(
                            onClick = {
                                copyToClipboard(context, logContent)
                            }
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "复制日志",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 清空按钮
                        IconButton(
                            onClick = {
                                DebugLog.clearLogFile()
                                logContent = "日志已清空"
                            }
                        ) {
                            Text(
                                text = "🗑️",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 关闭按钮
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                // 日志内容区域
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(logContent.split("\n")) { line ->
                            if (line.isNotBlank()) {
                                LogLineItem(line = line)
                            }
                        }
                    }
                }

                // 底部提示
                Text(
                    text = "💡 提示：日志仅在应用运行期间保存，重启后会清空",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun LogLineItem(line: String) {
    val (color, fontWeight) = when {
        line.contains("ERROR") || line.contains("✗") ->
            MaterialTheme.colorScheme.error to FontWeight.Bold
        line.contains("WARN") ->
            MaterialTheme.colorScheme.tertiary to FontWeight.Medium
        line.contains("✓") ->
            Color(0xFF4CAF50) to FontWeight.Normal
        line.contains("I/") && !line.contains("ERROR") && !line.contains("WARN") ->
            MaterialTheme.colorScheme.onSurface to FontWeight.Normal
        else ->
            MaterialTheme.colorScheme.onSurfaceVariant to FontWeight.Normal
    }

    Text(
        text = line,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontWeight = fontWeight,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun loadLogContent(onComplete: (String) -> Unit) {
    try {
        val content = DebugLog.getLogContent()
        onComplete(content)
    } catch (e: Exception) {
        onComplete("Failed to load logs: ${e.message}")
    }
}

private fun copyToClipboard(context: android.content.Context, text: String) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
        as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("ADB Tool Logs", text)
    clipboard.setPrimaryClip(clip)
}