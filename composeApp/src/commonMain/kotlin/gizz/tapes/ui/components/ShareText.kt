package gizz.tapes.ui.components

import androidx.compose.runtime.Composable

/** Returns a callback that hands [String] text to the platform's share mechanism. */
@Composable
expect fun rememberShareText(): (String) -> Unit
