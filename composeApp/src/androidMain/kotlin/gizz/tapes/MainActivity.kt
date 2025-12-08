package gizz.tapes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory

@Inject
class MainActivity(private val metroViewModelFactory: MetroViewModelFactory) : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GizzTapesApp(metroViewModelFactory)
        }
    }
}
