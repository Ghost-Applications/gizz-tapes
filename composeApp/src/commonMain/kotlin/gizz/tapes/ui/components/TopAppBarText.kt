package gizz.tapes.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import gizz.tapes.data.Title

@Composable
fun TopAppBarText(
    title: Title,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.value,
        maxLines = 1,
        overflow = TextOverflow.Visible,
        modifier = modifier.basicMarquee(
            iterations = Int.MAX_VALUE
        ),
        style = MaterialTheme.typography.titleLarge
    )
}
