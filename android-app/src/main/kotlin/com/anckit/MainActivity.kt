package com.anckit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import anc.ui.android.AncKitNavGraph
import anc.ui.android.viewmodel.FrameworkViewModel

private val AncKitDarkColors = darkColorScheme(
    primary          = Color(0xFF58A6FF),
    onPrimary        = Color(0xFF0D1117),
    secondary        = Color(0xFF3FB950),
    onSecondary      = Color(0xFF0D1117),
    tertiary         = Color(0xFFD29922),
    background       = Color(0xFF0D1117),
    surface          = Color(0xFF161B22),
    onBackground     = Color(0xFFE6EDF3),
    onSurface        = Color(0xFFE6EDF3),
    error            = Color(0xFFF85149),
    onError          = Color(0xFF0D1117),
    outline          = Color(0xFF30363D),
    surfaceVariant   = Color(0xFF21262D),
    onSurfaceVariant = Color(0xFF8B949E)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = AncKitDarkColors) {
                val viewModel: FrameworkViewModel = viewModel()
                AncKitNavGraph(viewModel = viewModel)
            }
        }
    }
}
