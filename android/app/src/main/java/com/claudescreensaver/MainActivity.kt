package com.claudescreensaver

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.claudescreensaver.data.network.BridgeDiscovery
import com.claudescreensaver.data.network.SseClient
import com.claudescreensaver.ui.screens.SettingsScreen
import com.claudescreensaver.ui.screens.StatusDashboardScreen
import com.claudescreensaver.ui.theme.ClaudeAccent
import com.claudescreensaver.ui.theme.ClaudeScreenSaverTheme
import com.claudescreensaver.viewmodel.StatusViewModel

class MainActivity : ComponentActivity() {

    private lateinit var bridgeDiscovery: BridgeDiscovery

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        bridgeDiscovery = BridgeDiscovery(this)
        val viewModel = StatusViewModel(SseClient())

        setContent {
            ClaudeScreenSaverTheme {
                var showPreview by remember { mutableStateOf(false) }
                val uiState by viewModel.uiState.collectAsState()
                val servers by bridgeDiscovery.servers.collectAsState()

                if (showPreview) {
                    StatusDashboardScreen(
                        uiState = uiState,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column {
                        SettingsScreen(
                            uiState = uiState,
                            discoveredServers = servers,
                            onConnect = { url -> viewModel.connect(url) },
                            onDisconnect = { viewModel.disconnect() },
                            modifier = Modifier.weight(1f),
                        )
                        // Preview button at bottom
                        Button(
                            onClick = { showPreview = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ClaudeAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                        ) {
                            Text("Preview ScreenSaver")
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bridgeDiscovery.startDiscovery()
    }

    override fun onPause() {
        bridgeDiscovery.stopDiscovery()
        super.onPause()
    }
}
