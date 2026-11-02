package gizz.tapes.ui.settings

import androidx.compose.runtime.Composable

/** The installed app's version name and build number, e.g. "Nein (29885238)". */
@Composable
expect fun rememberAppVersion(): String
