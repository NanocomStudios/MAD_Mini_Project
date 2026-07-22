package lk.nanocom.app.madminiproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import lk.nanocom.app.madminiproject.ui.theme.MADMiniProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MADMiniProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}


data class Floor(
    val id: String,
    val name: String,
    val rooms: List<Room>
)

data class Room(
    val id: String,
    val name: String,
    val devices: List<Device>
)

data class Switch(
    val id: String,
    val isOn: Boolean
)

sealed class Device {
    data class Outlet(
        val id: String,
        val name: String,
        var isOn: Boolean,
        var status: DeviceStatus
    ) : Device()

    data class MultiSwitch(
        val id: String,
        val name: String,
        val switches: List<Switch>,
        var status: DeviceStatus
    ) : Device()

    data class SafetyDevice(
        val id: String,
        val name: String,
        var isOn: Boolean,
        val maxOnDuration: Int,
        var status: DeviceStatus
    ) : Device()

    data class Camera(
        val id: String,
        val name: String,
        val streamUri: String,
        var status: DeviceStatus
    ) : Device()
}

enum class DeviceStatus {ON, OFF, ERROR, DISCONNECTED}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MADMiniProjectTheme {
        Greeting("Android")
    }
}