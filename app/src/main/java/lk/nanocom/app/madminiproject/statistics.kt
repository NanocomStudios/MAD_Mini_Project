package lk.nanocom.app.madminiproject

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

@Composable
fun StatScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF252525))
            .padding(top = 48.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            color = Color.White,
            text = "Statistics",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )

        StatisticsChart(
            modifier = Modifier.padding(top = 32.dp)
        )
    }
}

@Composable
fun StatisticsChart(modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(Unit) {
        modelProducer.runTransaction {
            lineSeries { series(1, 4, 3, 7, 5, 15, 4) }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VerticalText(
                text = "Num of units",
                color = Color.White
            )

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis()
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .weight(1f)
                    .height(250.dp)
            )
        }

        Text(
            text = "Day",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun VerticalText(
    text: String,
    color: Color = Color.White
) {
    Layout(
        content = {
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.rotate(-90f)
            )
        }
    ) { measurables, _ ->
        val placeable = measurables.first().measure(Constraints())
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width - placeable.height) / 2,
                y = -(placeable.height - placeable.width) / 2
            )
        }
    }
}