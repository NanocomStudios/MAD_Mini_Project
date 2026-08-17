package lk.nanocom.app.madminiproject

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BouncingDotsLoading(
    dotSize: Dp = 16.dp,
    dotColor: Color = MaterialTheme.colorScheme.primary,
    travelDistance: Dp = 20.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bouncing_dots")

    // Create animations for 3 dots with staggered delays
    val dotAnimations = listOf(0, 1, 2).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(index * 150)
            ),
            label = "dot_$index"
        )
    }
    Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)

        ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dotAnimations.forEach { animState ->
                val yOffset = animState.value * travelDistance.value

                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(dotSize)
                        .graphicsLayer { translationY = -yOffset } // Moves the dot up
                        .background(color = dotColor, shape = CircleShape)
                )
            }
        }
    }
}