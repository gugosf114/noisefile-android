package com.noisefile.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.noisefile.app.ui.NoiseFileRoot
import com.noisefile.app.ui.theme.NoiseFileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoiseFileTheme {
                NoiseFileRoot()
            }
        }
    }
}
