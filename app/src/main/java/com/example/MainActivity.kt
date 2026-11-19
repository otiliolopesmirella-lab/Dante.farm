package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FarmDeckScreen
import com.example.ui.FarmViewModel
import com.example.ui.theme.DigiQuadTheme

class MainActivity : ComponentActivity() {

    private var onMemoryTrim: ((Int) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            DigiQuadTheme {
                val farmViewModel: FarmViewModel = viewModel()
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, farmViewModel) {
                    onMemoryTrim = farmViewModel::onTrimMemory
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> farmViewModel.onAppForegrounded()
                            Lifecycle.Event.ON_RESUME -> farmViewModel.onAppResumed()
                            Lifecycle.Event.ON_STOP -> farmViewModel.onAppBackgrounded()
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        onMemoryTrim = null
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }
                FarmDeckScreen(viewModel = farmViewModel)
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        onMemoryTrim?.invoke(level)
    }
}
