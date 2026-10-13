package gizz.tapes.ui.mycontent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun MyContentScreen(
    modifier: Modifier = Modifier,
    onFavoritesClicked: () -> Unit,
    onHistory: () -> Unit,
    onDownloadsClicked: () -> Unit,
) {
    val rows = listOf(
        MyContentRow("Favorites", Icons.Filled.Favorite, onFavoritesClicked),
        MyContentRow("History", Icons.Filled.History, onHistory),
        MyContentRow("Downloads", Icons.Filled.Download, onDownloadsClicked),
    )

    LazyColumn(modifier = modifier) {
        items(rows, key = { it.title }) { row ->
            ListItem(
                modifier = Modifier.clickable(onClick = row.onClick),
                leadingContent = { Icon(row.icon, contentDescription = null) },
                headlineContent = { Text(row.title) },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                },
            )
            HorizontalDivider()
        }
    }
}

private data class MyContentRow(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)
