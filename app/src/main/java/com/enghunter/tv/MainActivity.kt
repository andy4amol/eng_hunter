package com.enghunter.tv

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.enghunter.tv.llm.LlmConfigStore
import com.enghunter.tv.ui.EngHunterTvApp
import com.enghunter.tv.viewmodel.EngHunterViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: EngHunterViewModel by viewModels()

    private val adbLlmConfigReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LlmConfigStore.ACTION_ADB_CONFIG_LLM) {
                viewModel.applyAdbLlmConfigIntent(intent)
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val recordGranted = results[Manifest.permission.RECORD_AUDIO] == true ||
            hasRecordAudioPermission()
        viewModel.updatePermissionGranted(recordGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 電視端練習期間保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val granted = hasRecordAudioPermission()
        viewModel.updatePermissionGranted(granted)
        if (!granted) {
            requestAudioAndBluetoothPermissions()
        }

        // 支持啟動時或運行中通過 ADB 命令直接注入大模型配置
        viewModel.applyAdbLlmConfigIntent(intent)
        val filter = IntentFilter(LlmConfigStore.ACTION_ADB_CONFIG_LLM)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(adbLlmConfigReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(adbLlmConfigReceiver, filter)
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            EngHunterTvApp(
                uiState = uiState,
                viewModel = viewModel,
                onRequestPermission = { requestAudioAndBluetoothPermissions() }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(adbLlmConfigReceiver)
        } catch (_: Exception) {
        }
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioAndBluetoothPermissions() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }
}
