package lk.nanocom.app.madminiproject

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun StatScreen(deviceId: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 48.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            color = MaterialTheme.colorScheme.onBackground,
            text = "Statistics",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )

        if (deviceId != null) {
            StatisticsChart(
                deviceId = deviceId,
                modifier = Modifier.padding(top = 32.dp)
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Please select a device from the room screen to view statistics.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun StatisticsChart(deviceId: String?, modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val context = LocalContext.current

    val xLabels = remember {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("EEE")
        (6 downTo 0).map { offset -> today.minusDays(offset.toLong()).format(formatter) }
    }

    val bottomAxisFormatter = remember(xLabels) {
        CartesianValueFormatter { value, _, _ ->
            xLabels.getOrElse(value.toInt()) { value.toString() }
        }
    }

    LaunchedEffect(deviceId) {
        if (deviceId == null) return@LaunchedEffect

        val sharedPref = context.getSharedPreferences("Cookies", Context.MODE_PRIVATE)
        val sessionID = sharedPref.getString("sessionID", "") ?: ""

        val req = ItemInfoRequest(
            itemID = deviceId.toIntOrNull() ?: -1,
            sessionID = sessionID
        )

        try {
            val response = RetrofitClient.apiService.getItemLogPostRequest(req)
            if (response.isSuccessful && response.body()?.response == "success") {
                val logs = response.body()?.logs ?: emptyList()
                
                withContext(Dispatchers.Default) {
                    val today = LocalDate.now()
                    val dailyOnTime = mutableMapOf<LocalDate, Double>()
                    
                    // Pre-fill last 7 days
                    (0..6).forEach { dailyOnTime[today.minusDays(it.toLong())] = 0.0 }

                    val sortedLogs = logs.sortedBy { it.timestamp }
                    var lastOnTimestamp: LocalDateTime? = null
                    
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    
                    for (log in sortedLogs) {
                        try {
                            val logTime = LocalDateTime.parse(log.timestamp, formatter)
                            
                            if (log.state == "1") {
                                lastOnTimestamp = logTime
                            } else if (log.state == "0" && lastOnTimestamp != null) {
                                val durationSeconds = java.time.Duration.between(lastOnTimestamp, logTime).seconds
                                val durationMinutes = durationSeconds / 60.0
                                
                                // Simplified: add duration to the start day
                                val startDay = lastOnTimestamp.toLocalDate()
                                if (dailyOnTime.containsKey(startDay)) {
                                    dailyOnTime[startDay] = dailyOnTime.getOrDefault(startDay, 0.0) + durationMinutes
                                }
                                lastOnTimestamp = null
                            }
                        } catch (e: Exception) {
                            Log.e("STAT_ERROR", "Error parsing timestamp: ${log.timestamp}")
                        }
                    }

                    // If still ON, count up to now
                    lastOnTimestamp?.let {
                        val durationMinutes = java.time.Duration.between(it, LocalDateTime.now()).seconds / 60.0
                        val startDay = it.toLocalDate()
                        if (dailyOnTime.containsKey(startDay)) {
                            dailyOnTime[startDay] = dailyOnTime.getOrDefault(startDay, 0.0) + durationMinutes
                        }
                    }

                    val dataPoints = (6 downTo 0).map { offset ->
                        dailyOnTime[today.minusDays(offset.toLong())] ?: 0.0
                    }

                    modelProducer.runTransaction {
                        lineSeries { series(dataPoints) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("API_ERROR", "Failed to fetch logs", e)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VerticalText(
                text = "On-time (min)",
                color = MaterialTheme.colorScheme.onBackground
            )

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = bottomAxisFormatter
                    )
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .weight(1f)
                    .height(250.dp)
            )
        }

        Text(
            text = "Last 7 Days",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun VerticalText(
    text: String,
    color: Color = MaterialTheme.colorScheme.onBackground
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