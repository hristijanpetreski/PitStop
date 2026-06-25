
package uk.hristijan.pitstop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.android.gms.maps.MapsInitializer
import uk.hristijan.pitstop.app.PitStopApp
import uk.hristijan.pitstop.app.PitStopApplication
import uk.hristijan.pitstop.ui.theme.PitStopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Pre-initialize Google Maps SDK for MapDescriptorFactory usage
        MapsInitializer.initialize(applicationContext)
        
        enableEdgeToEdge()
        val container = (application as PitStopApplication).container
        setContent {
            val theme by container.userSettingsPreferences.theme.collectAsState(initial = "system")
            val darkTheme = when (theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            PitStopTheme(darkTheme = darkTheme) {
                PitStopApp()
            }
        }
    }
}
