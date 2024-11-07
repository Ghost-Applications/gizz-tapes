package gizz.tapes.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import gizz.tapes.R

fun navigationUpIcon(upClick: () -> Unit): @Composable () -> Unit = {
    IconButton(onClick = upClick) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.navigate_back)
        )
    }
}

fun gizzIcon(): @Composable () -> Unit = {
    AsyncImage(
        model = R.raw.gizz_tapes_logo,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
    )
}
