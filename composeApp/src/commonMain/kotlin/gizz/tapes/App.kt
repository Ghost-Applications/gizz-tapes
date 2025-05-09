package gizz.tapes

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import kotlinx.serialization.Serializable

val Primary80 = Color(0xFFB5C4FF)
val Secondary80 = Color(0xFF81D4DC)
val Tertiary80 = Color(0xFFF0B3E8)

val Primary40 = Color(0xFF4B5C92)
val Secondary40 = Color(0xFF625b71)
val Tertiary40 = Color(0xFF7F4D7B)


private val DarkColorScheme = darkColorScheme(
    primary = Primary80,
    secondary = Secondary80,
    tertiary = Tertiary80
)

private val LightColorScheme = lightColorScheme(
    primary = Primary40,
    secondary = Secondary40,
    tertiary = Tertiary40
)


@Composable
fun GizzTapesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}

@Composable
@Preview
fun GizzTapesApp() {
    setSingletonImageLoaderFactory {
        ImageLoader.Builder(it)
            .crossfade(true)
            .build()
    }

    GizzTapesTheme {
        val navController = rememberNavController()
        GizzTapesNavController(navController = navController)
    }
}

@Composable
fun GizzTapesNavController(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = YearSelection
    ) {
        composable<YearSelection> {
            YearSelectionScreen()
        }
    }
}

@Composable
fun YearSelectionScreen(
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Cyan)) {
        Text("It works!")
    }
}

@Serializable data object YearSelection
