package gizz.tapes

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import dagger.hilt.android.AndroidEntryPoint
import gizz.tapes.ui.GizzApp
import gizz.tapes.ui.theme.GizzTheme
import timber.log.Timber

@ExperimentalMaterial3Api
@AndroidEntryPoint
class MainActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("savedInstanceState %s", savedInstanceState)

        enableEdgeToEdge()
        setContent {
            GizzTheme {
                GizzApp()
            }
        }
    }
}
