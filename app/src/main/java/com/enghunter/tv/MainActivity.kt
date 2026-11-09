package com.enghunter.tv

import android.Manifest
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
import com.enghunter.tv.ui.EngHunterTvApp
import com.enghunter.tv.viewmodel.EngHunterViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: EngHunterViewModel by viewModels()

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

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            EngHunterTvApp(
                uiState = uiState,
                viewModel = viewModel,
                onRequestPermission = { requestAudioAndBluetoothPermissions() }
            )
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
