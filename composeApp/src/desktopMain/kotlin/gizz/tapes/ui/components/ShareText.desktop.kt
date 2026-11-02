package gizz.tapes.ui.components

import androidx.compose.runtime.Composable
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

// desktop has no share sheet, so copy the text to the clipboard instead.
@Composable
actual fun rememberShareText(): (String) -> Unit = { text ->
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}
