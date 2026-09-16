package dev.algoforge.ide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import dev.algoforge.ide.ui.AlgoForgeApp
import dev.algoforge.ide.ui.theme.AlgoForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlgoForgeTheme {
                Surface {
                    AlgoForgeApp()
                }
            }
        }
    }
}
