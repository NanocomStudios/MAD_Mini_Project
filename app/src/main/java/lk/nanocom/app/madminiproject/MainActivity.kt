package lk.nanocom.app.madminiproject

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.Alignment
import lk.nanocom.app.madminiproject.ui.theme.MADMiniProjectTheme
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission allowed: App can show notifications
        } else {
            // Permission denied: Inform the user
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MADMiniProjectTheme {
                SmartHomeApp()
            }
        }

        askNotificationPermission()
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get the initial/current FCM registration token
            val token = task.result
            Log.d("FCM", "Device Token: $token")

            // TODO: Send token to your backend server
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
                onFloorClick = { floorId -> navController.navigate("floors/$floorId") },
                onAddFloorClick = {},
                onDeleteFloorClick = {}
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
                onBack = { navController.popBackStack() },
                onAddRoomClick = {},
                onDeleteRoomClick = {}
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
                onToggleDevice = {},
                onAddDeviceClick = {},
                onDeleteDeviceClick = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorListScreen(
    floors: List<Floor>,
    onFloorClick: (String) -> Unit,
    onAddFloorClick: () -> Unit,
    onDeleteFloorClick: (String) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Floors") }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddFloorClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Floor"
                )
            }
        }
    ) { padding ->
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = floor.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        IconButton(
                            onClick = { onDeleteFloorClick(floor.id) },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete Floor",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    floor: Floor,
    onRoomClick: (String) -> Unit,
    onBack: () -> Unit,
    onAddRoomClick: () -> Unit,
    onDeleteRoomClick: (String) -> Unit
) {
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRoomClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Room"
                )
            }
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
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onRoomClick(room.id) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = room.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${room.devices.size} devices",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(
                            onClick = { onDeleteRoomClick(room.id) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete Room",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceGridScreen(
    room: Room,
    onBack: () -> Unit,
    onToggleDevice: (String) -> Unit,
    onAddDeviceClick: () -> Unit,
    onDeleteDeviceClick: (String) -> Unit
) {
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddDeviceClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Device"
                )
            }
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
                val deviceId = deviceIdOf(device)
                DeviceCard(
                    device = device,
                    onToggle = { onToggleDevice(deviceId) },
                    onDelete = { onDeleteDeviceClick(deviceId) }
                )
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
fun DeviceCard(
    device: Device,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(enabled = device !is Device.Camera) { onToggle() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
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
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Device",
                    tint = MaterialTheme.colorScheme.error
                )
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