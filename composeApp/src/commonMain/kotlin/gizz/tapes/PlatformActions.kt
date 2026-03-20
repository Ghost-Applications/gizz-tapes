package gizz.tapes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

val LocalPlatformActions = compositionLocalOf<@Composable () -> Unit> { {} }
