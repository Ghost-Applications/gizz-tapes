package gizz.tapes

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import gizz.tapes.nav.GizzTapesNavController

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
fun GizzTapesApp() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(imageCacheDirectory(context))
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()
    }

    GizzTapesTheme {
        val navController = rememberNavController()
        GizzTapesNavController(
            navController = navController
        )
    }
}
