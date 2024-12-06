package gizz.tapes.ui

import androidx.annotation.OptIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import gizz.tapes.ui.nav.GizzNavController

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun GizzApp() {
    val navController = rememberNavController()
    GizzNavController(navController = navController)
}
