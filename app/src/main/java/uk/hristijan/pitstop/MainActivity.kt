
package uk.hristijan.pitstop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import uk.hristijan.pitstop.app.PitStopApp
import uk.hristijan.pitstop.ui.theme.PitStopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PitStopTheme {
                PitStopApp()
            }
        }
    }
}
