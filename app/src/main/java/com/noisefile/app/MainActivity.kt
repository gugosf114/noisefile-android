package com.noisefile.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.noisefile.app.ui.NoiseFileRoot
import com.noisefile.app.ui.theme.NoiseFileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // One look, day or night: the app is a night instrument. Light icons on a dark sky.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            NoiseFileTheme {
                NoiseFileRoot()
            }
        }
    }
}
