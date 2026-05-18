package gizz.tapes.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import gizz.tapes.GizzTapesTheme

/**
 * Animated shimmer background — a highlight band sweeps across using a
 * translating linear gradient. Apply to any sized layout; the modifier
 * measures itself so the sweep distance matches the component width.
 */
fun Modifier.shimmer(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offsetX by transition.animateFloat(
        initialValue = -2f * size.width.coerceAtLeast(1),
        targetValue = 2f * size.width.coerceAtLeast(1),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = Color.White.copy(alpha = 0.45f).compositeOver(base)

    this
        .onSizeChanged { size = it }
        .background(
            Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(offsetX, 0f),
                end = Offset(offsetX + size.width.toFloat(), size.height.toFloat())
            )
        )
}

@Composable
fun ShowCardShimmer(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .height(GizzTapesTheme.size.gridCardSize)
                .fillMaxWidth()
                .shimmer()
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun SelectionItemRowShimmer(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(GizzTapesTheme.size.selectionRowCorner))
            .height(GizzTapesTheme.size.selectionRowSize)
            .shimmer()
    )
}

@Composable
fun HomeHeaderShimmer(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth()
            .height(GizzTapesTheme.size.headerSize)
            .shimmer()
    )
}

/**
 * Shimmer placeholder sized to match the rendered width and height of [text]
 * in the given [style]. Useful for skeleton states where you know the
 * eventual text — the bar matches its real footprint, so the layout
 * doesn't shift when content arrives.
 */
@Composable
fun TextShimmer(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val (width, height) = remember(text, style, density) {
        val layout = measurer.measure(text, style)
        with(density) { layout.size.width.toDp() to layout.size.height.toDp() }
    }
    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(4.dp))
            .shimmer()
    )
}

fun LazyGridScope.loadingCards() {
    items(10) {
        ShowCardShimmer()
    }
}
