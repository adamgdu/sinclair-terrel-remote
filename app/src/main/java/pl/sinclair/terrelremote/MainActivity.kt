package pl.sinclair.terrelremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import pl.sinclair.terrelremote.ui.RemoteScreen
import pl.sinclair.terrelremote.ui.RemoteViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: RemoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1F6FEB),
                    background = Color(0xFFEEF2F6),
                    surface = Color(0xFFFFFFFF)
                )
            ) {
                RemoteScreen(viewModel = viewModel)
            }
        }
    }
}
