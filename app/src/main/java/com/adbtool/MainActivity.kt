package com.adbtool

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adbtool.ui.screens.ConnectScreen
import com.adbtool.ui.screens.AppListScreen
import com.adbtool.ui.theme.AdbToolTheme
import com.adbtool.utils.DebugLog
import com.adbtool.viewmodel.AppListViewModel
import com.adbtool.viewmodel.ConnectViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var onApkSelected: (Uri) -> Unit = {}

    private val apkPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onApkSelected(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        DebugLog.flushToFile()
        DebugLog.i("MainActivity", "ADB Tool shutting down")
    }

    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化调试日志
        DebugLog.init(this)
        DebugLog.i("MainActivity", "ADB Tool starting up")

        setContent {
            AdbToolTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent(
                        context = this@MainActivity,
                        onApkSelected = { onApkSelected = it },
                        launchApkPicker = { apkPickerLauncher.launch("application/vnd.android.package-archive") }
                    )
                }
            }
        }
    }
}

@Composable
fun MainContent(
    context: Context,
    onApkSelected: ((Uri) -> Unit) -> Unit,
    launchApkPicker: () -> Unit
) {
    val connectViewModel: ConnectViewModel = viewModel()
    val appListViewModel: AppListViewModel = viewModel()

    // 注册APK选择回调
    onApkSelected { uri ->
        DebugLog.i("MainContent", "Selected APK URI: $uri")
        appListViewModel.installApkFromUri(context, uri)
    }

    val connectionState by connectViewModel.connectionState.collectAsState()

    if (connectionState.status == com.adbtool.data.models.ConnectionStatus.CONNECTED) {
        AppListScreen(
            viewModel = appListViewModel,
            deviceAddress = connectionState.deviceAddress ?: "",
            onDisconnect = { connectViewModel.disconnect() },
            onInstallApk = { launchApkPicker() }
        )
    } else {
        ConnectScreen(
            viewModel = connectViewModel
        )
    }
}