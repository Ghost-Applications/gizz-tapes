package gizz.tapes

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import gizz.tapes.playback.LinuxMpris
import gizz.tapes.playback.MacNowPlaying
import gizz.tapes.playback.WindowsMediaControls
import gizz.tapes.ui.player.PlayerState
import gizz_tapes.composeapp.generated.resources.Res
import gizz_tapes.composeapp.generated.resources.gizz_tapes_logo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import org.jetbrains.compose.resources.painterResource

private val logger = Logger.withTag("main")

fun main() = application {
    val appGraph = createGraphFactory<DesktopAppGraph.Factory>().create(AppContext())
    val state = rememberWindowState(width = 1200.dp, height = 800.dp)
    val trayState = rememberTrayState()

    if (isTraySupported) {
        Tray(icon = painterResource(Res.drawable.gizz_tapes_logo), state = trayState, tooltip = "Gizz Tapes")
    }

    LaunchedEffect(Unit) {
        appGraph.mediaPlayer.state
            .filterIsInstance<PlayerState.MediaLoaded.Playing>()
            .distinctUntilChangedBy { it.mediaId }
            .collect { trayState.sendNotification(Notification(it.title, it.albumTitle)) }
    }

    when (OperatingSystem.current) {
        OperatingSystem.MacOs -> LaunchedEffect(Unit) {
            val nowPlaying = MacNowPlaying(appGraph.mediaPlayer)
            nowPlaying.registerRemoteCommands()
            nowPlaying.syncNowPlayingInfo()
        }
        OperatingSystem.Linux -> LaunchedEffect(Unit) {
            // No session bus (e.g. headless or abstract-socket-only setups) just means no MPRIS.
            runCatching { LinuxMpris(appGraph.mediaPlayer).run() }
                .onFailure { if (it is CancellationException) throw it else logger.w(it) { "MPRIS unavailable" } }
        }
        // SMTC needs the window's HWND, so it's hooked up inside Window below.
        OperatingSystem.Windows, is OperatingSystem.Other -> Unit
    }

    Window(
        state = state,
        onCloseRequest = {
            appGraph.mediaPlayer.release()
            exitApplication()
        },
        title = "Gizz Tapes",
    ) {
        if (OperatingSystem.current == OperatingSystem.Windows) {
            LaunchedEffect(Unit) {
                runCatching { WindowsMediaControls(appGraph.mediaPlayer, window.windowHandle).run() }
                    .onFailure { if (it is CancellationException) throw it else logger.w(it) { "SMTC unavailable" } }
            }
        }

        CompositionLocalProvider(LocalMetroViewModelFactory provides appGraph.metroViewModelFactory) {
            GizzTapesApp()
        }
    }
}
