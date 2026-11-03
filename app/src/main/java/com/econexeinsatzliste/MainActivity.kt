package com.econexeinsatzliste

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.econexeinsatzliste.ui.EinsatzAppScreen
import com.econexeinsatzliste.ui.EinsatzViewModel
import com.econexeinsatzliste.ui.theme.EconexEinsatzlisteTheme

class MainActivity : ComponentActivity() {

    private val viewModel: EinsatzViewModel by viewModels {
        EinsatzViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestHighRefreshRate()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            EconexEinsatzlisteTheme(
                darkTheme = uiState.isDarkTheme,
                dynamicColor = false
            ) {
                EinsatzAppScreen(viewModel = viewModel)
            }
        }
    }

    /**
     * Explicitly requests the maximum display refresh rate (e.g. 120Hz / 90Hz) from Android's WindowManager.
     */
    private fun requestHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val currentDisplay = display
                if (currentDisplay != null) {
                    val supportedModes = currentDisplay.supportedModes
                    val maxRefreshRateMode = supportedModes.maxByOrNull { it.refreshRate }
                    if (maxRefreshRateMode != null && maxRefreshRateMode.refreshRate >= 90f) {
                        window.attributes = window.attributes.apply {
                            preferredDisplayModeId = maxRefreshRateMode.modeId
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
