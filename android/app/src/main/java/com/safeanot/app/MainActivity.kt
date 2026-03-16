/**
 * Single-activity host for the Safe Anot? app.
 * Sets up Jetpack Compose content with the app theme and navigation graph.
 */
package com.safeanot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.safeanot.app.navigation.SafeAnotNavGraph
import com.safeanot.app.ui.theme.SafeAnotTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafeAnotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SafeAnotNavGraph()
                }
            }
        }
    }
}
