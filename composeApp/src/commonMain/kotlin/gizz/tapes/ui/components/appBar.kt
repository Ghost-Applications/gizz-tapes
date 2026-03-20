package gizz.tapes.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import gizz.tapes.nav.NavigateUp
import gizz_tapes.composeapp.generated.resources.Res
import gizz_tapes.composeapp.generated.resources.navigate_back
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

fun navigationUpIcon(navigateUp: NavigateUp): @Composable () -> Unit = {
    IconButton(onClick = { navigateUp() }) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(Res.string.navigate_back)
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
fun gizzIcon(): @Composable () -> Unit = {
    AsyncImage(
        model = Res.getUri("drawable/gizz_tapes_logo.svg"),
        contentDescription = null,
        modifier = Modifier.size(48.dp)
    )
}
