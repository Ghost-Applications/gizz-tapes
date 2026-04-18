package gizz.tapes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gizz_tapes.composeapp.generated.resources.Res
import gizz_tapes.composeapp.generated.resources.api_error_message
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(
    error: Throwable,
    modifier: Modifier = Modifier,
    message: String = stringResource(Res.string.api_error_message)
) {
    var clickCount by remember { mutableStateOf(0) }

    Box(
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable {
                clickCount++
            }
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()

            if (clickCount > 5) {
                Text(
                    text = error.stackTraceToString()
                )
            }
        }
    }
}

@Preview
@Composable
private fun LoadingScreenPreview() {
    LoadingScreen()
}

@Preview
@Composable
private fun ErrorScreenPreview() {
    ErrorScreen(error = Exception("Test"))
}
