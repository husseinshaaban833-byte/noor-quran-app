package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.configureAppCheck
import com.example.ui.NoorViewModel
import com.example.ui.navigation.NoorAppScaffold
import com.example.ui.theme.NoorAlQuranTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // App Check MUST be configured before Firebase AI / Firestore calls
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            val vm: NoorViewModel = viewModel()
            val prefs by vm.prefs.collectAsStateWithLifecycle()
            NoorAlQuranTheme(darkModeSetting = prefs.darkModeSetting) {
                NoorAppScaffold(vm = vm)
            }
        }
    }
}
