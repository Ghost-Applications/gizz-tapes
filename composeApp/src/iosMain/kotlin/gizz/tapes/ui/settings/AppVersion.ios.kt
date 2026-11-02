package gizz.tapes.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSBundle

@Composable
actual fun rememberAppVersion(): String = remember {
    val info = NSBundle.mainBundle.infoDictionary
    "${info?.get("CFBundleShortVersionString")} (${info?.get("CFBundleVersion")})"
}
