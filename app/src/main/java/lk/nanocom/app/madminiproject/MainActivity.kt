package lk.nanocom.app.madminiproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import lk.nanocom.app.madminiproject.ui.theme.MADMiniProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MADMiniProjectTheme {
                SmartHomeApp()
            }
        }
    }
}

object SampleData {
    val floors = listOf(
        Floor(
            id = "floor2",
            name = "First Floor",
            rooms = listOf(
                Room(
                    id = "room3",
                    name = "Bedroom",
                    devices = listOf(
                        Device.Outlet("d5", "Lamp Outlet", isOn = false, status = DeviceStatus.OFF)
                    )
                )
            )
        ),
        Floor(
            id = "floor1",
            name = "Ground Floor",
            rooms = listOf(
                Room(
                    id = "room1",
                    name = "Living Room",
                    devices = listOf(
                        Device.Outlet("d1", "TV Outlet", isOn = true, status = DeviceStatus.ON),
                        Device.MultiSwitch(
                            "d2", "Wall Gang Box",
                            switches = listOf(
                                SwitchNode("s1", true),
                                SwitchNode("s2", false),
                                SwitchNode("s3", false)
                            ),
                            status = DeviceStatus.ON
                        ),
                        Device.Camera("d3", "Living Room Cam", status = DeviceStatus.ON)
                    )
                ),
                Room(
                    id = "room2",
                    name = "Kitchen",
                    devices = listOf(
                        Device.SafetyDevice("d4", "Iron", isOn = false, maxOnDuration = 1800, status = DeviceStatus.OFF)
                    )
                )
            )
        )
    )
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

data class SwitchNode(
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
        val switches: List<SwitchNode>,
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
        var status: DeviceStatus
    ) : Device()
}

enum class DeviceStatus { ON, OFF, ERROR, DISCONNECTED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartHomeApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "floors",
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable("floors") {
            FloorListScreen(
                floors = SampleData.floors,
                onFloorClick = { floorId -> navController.navigate("floors/$floorId") }
            )
        }
        composable(
            "floors/{floorId}",
            arguments = listOf(navArgument("floorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val floorId = backStackEntry.arguments?.getString("floorId") ?: return@composable
            val floor = SampleData.floors.first { it.id == floorId }
            RoomListScreen(
                floor = floor,
                onRoomClick = { roomId -> navController.navigate("floors/$floorId/rooms/$roomId") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "floors/{floorId}/rooms/{roomId}",
            arguments = listOf(
                navArgument("floorId") { type = NavType.StringType },
                navArgument("roomId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val floorId = backStackEntry.arguments?.getString("floorId") ?: return@composable
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val room = SampleData.floors.first { it.id == floorId }.rooms.first { it.id == roomId }
            DeviceGridScreen(
                room = room,
                onBack = { navController.popBackStack() },
                onToggleDevice = { /* wire up later */ }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorListScreen(floors: List<Floor>, onFloorClick: (String) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Floors") }) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(floors) { floor ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .clickable { onFloorClick(floor.id) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(floor.name, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(floor: Floor, onRoomClick: (String) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(floor.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            items(floor.rooms) { room ->
                Card(
                    modifier = Modifier.aspectRatio(1f).clickable { onRoomClick(room.id) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(room.name, style = MaterialTheme.typography.titleMedium)
                            Text("${room.devices.size} devices", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceGridScreen(room: Room, onBack: () -> Unit, onToggleDevice: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(room.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            items(room.devices) { device ->
                DeviceCard(device = device, onToggle = { onToggleDevice(deviceIdOf(device)) })
            }
        }
    }
}

fun deviceIdOf(device: Device): String = when (device) {
    is Device.Outlet -> device.id
    is Device.MultiSwitch -> device.id
    is Device.SafetyDevice -> device.id
    is Device.Camera -> device.id
}

@Composable
fun DeviceCard(device: Device, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.aspectRatio(1f).clickable(enabled = device !is Device.Camera) { onToggle() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            when (device) {
                is Device.Outlet -> {
                    Text(device.name, style = MaterialTheme.typography.titleSmall)
                    StatusBadge(device.status)
                    Switch(checked = device.isOn, onCheckedChange = { onToggle() })
                }
                is Device.MultiSwitch -> {
                    Text(device.name, style = MaterialTheme.typography.titleSmall)
                    StatusBadge(device.status)
                    Text(
                        "${device.switches.count { it.isOn }}/${device.switches.size} on",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                is Device.SafetyDevice -> {
                    Text(device.name, style = MaterialTheme.typography.titleSmall)
                    StatusBadge(device.status)
                    Text("Max ${device.maxOnDuration / 60} min", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = device.isOn, onCheckedChange = { onToggle() })
                }
                is Device.Camera -> {
                    Text(device.name, style = MaterialTheme.typography.titleSmall)
                    StatusBadge(device.status)
                    Icon(Icons.Default.Videocam, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: DeviceStatus) {
    val color = when (status) {
        DeviceStatus.ON -> Color(0xFF4CAF50)
        DeviceStatus.OFF -> Color(0xFF9E9E9E)
        DeviceStatus.ERROR -> Color(0xFFF44336)
        DeviceStatus.DISCONNECTED -> Color(0xFFFF9800)
    }
    Box(
        modifier = Modifier
            .background(color, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(status.name, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Preview(showBackground = true)
@Composable
fun SmartHomeAppPreview() {
    MADMiniProjectTheme {
        SmartHomeApp()
    }
}