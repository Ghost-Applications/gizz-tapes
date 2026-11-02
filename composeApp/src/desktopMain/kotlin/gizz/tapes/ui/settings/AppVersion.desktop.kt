package gizz.tapes.ui.settings

import androidx.compose.runtime.Composable

/** jpackage sets this property in packaged builds; it's absent when run from Gradle. */
@Composable
actual fun rememberAppVersion(): String = System.getProperty("jpackage.app-version") ?: "dev"
