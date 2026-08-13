package lk.nanocom.app.madminiproject

import android.Manifest
import android.content.Context
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
import lk.nanocom.app.madminiproject.ui.theme.MADMiniProjectTheme
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

data class SessionIDRequest(
    val sessionID: String
)

data class FirebaseTokenUpdateRequest(
    val userID: Int,
    val firebaseToken: String,
    val sessionID: String
)

data class STDResponse(
    val response: String,
    val error: String?
)

data class AppActionRequest(
    val itemID: Int,
    val action: String,
    val value: Int,
    val sessionID: String
)

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

    fun updateFirebaseToken(){

        askNotificationPermission()
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get the initial/current FCM registration token
            val token = task.result
            val sharedPref = getSharedPreferences("Cookies", Context.MODE_PRIVATE)
            sharedPref.edit {
                putString("firebase_token", token)
            }

            Log.d("FCM", "Device Token: $token")

        }

        val sharedPref = getSharedPreferences("Cookies", Context.MODE_PRIVATE)
        val savedSessionID: String? = sharedPref.getString("sessionID", "")

        val req = FirebaseTokenUpdateRequest(
            userID = sharedPref.getInt("userID", 0),
            firebaseToken = sharedPref.getString("firebase_token", "") ?: "",
            sessionID = sharedPref.getString("sessionID", "") ?: ""
        )
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.updateFirebaseTokenPostRequest(req)
                if (response.isSuccessful && response.body()?.response == "success") {
                    Log.d("FCM", "Firebase token updated successfully")
                } else {
                    Log.d("FCM", "Firebase token update failed")
                }
            } catch (e: Exception) {
                Log.d("FCM", "Firebase token update failed")
            }
        }
    }

    fun logoutRequest(){
        val sharedPref = getSharedPreferences("Cookies", Context.MODE_PRIVATE)
        val savedSessionID: String = sharedPref.getString("sessionID", "") ?: ""

        val req = SessionIDRequest(
            sessionID = savedSessionID
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.logoutPostRequest(req)
                if (response.isSuccessful && response.body()?.response == "success") {
                    Log.d("API_MESSAGE", "Logout successfully")

                    sharedPref.edit {
                        remove("sessionID")
                        remove("userID")
                    }


                } else {
                    Log.d("API_MESSAGE", "Logout failed")
                }
            } catch (e: Exception) {
                Log.d("API_ERROR", "Network failed")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isSessionValidated = mutableStateOf<Boolean?>(null)
        val sharedPref = getSharedPreferences("Cookies", Context.MODE_PRIVATE)
        val savedSessionID: String? = sharedPref.getString("sessionID", "")

        if (savedSessionID.isNullOrEmpty()) {
            isSessionValidated.value = false
        } else {
            val req = SessionIDRequest(sessionID = savedSessionID)
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.apiService.validateSessionPostRequest(req)
                    if (response.isSuccessful && response.body()?.response == "success") {
                        isSessionValidated.value = true

                        updateFirebaseToken()

                    } else {
                        isSessionValidated.value = false
                    }
                } catch (e: Exception) {
                    isSessionValidated.value = false
                }
            }
        }

        setContent {
            MADMiniProjectTheme {
                val validated = isSessionValidated.value
                if (validated == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading...")
                    }
                } else {
                    SmartHomeApp(
                        startDestination = if (validated) "floors" else "login",
                        updateFirebaseToken = ::updateFirebaseToken
                    )
                }
            }
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

data class RoomsResponse(
    val response: String,
    val floors: List<Floor>,
    val error: String? = null

)
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

data class Device(
    val id: String,
    val name: String,
    val type: String,
    var isOn: Boolean = false,
    var status: DeviceStatus = DeviceStatus.OFF,
    val switches: MutableList<SwitchNode> = mutableListOf(),
    val maxOnDuration: Int = 0
)

enum class DeviceStatus { ON, OFF, ERROR, DISCONNECTED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartHomeApp(
    startDestination: String = "login",
    updateFirebaseToken: () -> Unit
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val floors = remember { mutableStateOf<List<Floor>>(emptyList()) }

    val loadFloors = {
        val sharedPref = context.getSharedPreferences("Cookies", Context.MODE_PRIVATE)
        val savedSessionID: String = sharedPref.getString("sessionID", "") ?: ""

        if (savedSessionID.isNotEmpty()) {
            val req = SessionIDRequest(sessionID = savedSessionID)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val response = RetrofitClient.apiService.getAppRoomsPostRequest(req)
                    if (response.isSuccessful) {
                        if(response.body()?.response == "success"){
                            withContext(Dispatchers.Main) {
                                floors.value = response.body()?.floors ?: emptyList()
                            }
                        }

                    }
                } catch (e: Exception) {
                    Log.d("API_ERROR", "Failed to fetch floors")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadFloors()
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {



                    navController.navigate("floors") {
                        popUpTo("login") {
                            inclusive = true
                        }
                    }
                    updateFirebaseToken()
                    loadFloors()
                },
                onRegisterClick = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.popBackStack()
                }
            )
        }
        composable("floors") {
            FloorListScreen(
                floors = floors.value,
                onFloorClick = { floorId -> navController.navigate("floors/$floorId") },
                onAddFloorClick = {},
                onDeleteFloorClick = {},
                onLogoutClick = {

                    val sharedPref = context.getSharedPreferences("Cookies", Context.MODE_PRIVATE)
                    val savedSessionID: String = sharedPref.getString("sessionID", "") ?: ""

                    val req = SessionIDRequest(
                        sessionID = savedSessionID
                    )

                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val response = RetrofitClient.apiService.logoutPostRequest(req)
                            if (response.isSuccessful && response.body()?.response == "success") {
                                withContext(Dispatchers.Main) {
                                    Log.d("API_MESSAGE", "Logout successfully")

                                    sharedPref.edit {
                                        remove("sessionID")
                                        remove("userID")
                                    }

                                    navController.navigate("login") {
                                        popUpTo("floors") {
                                            inclusive = true
                                        }
                                    }
                                }

                            } else {
                                Log.d("API_MESSAGE", "Logout failed")
                            }
                        } catch (e: Exception) {
                            Log.d("API_ERROR", "Network failed")
                        }
                    }
                }
            )
        }
        composable(
            "floors/{floorId}",
            arguments = listOf(navArgument("floorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val floorId = backStackEntry.arguments?.getString("floorId") ?: return@composable
            val floor = floors.value.find { it.id == floorId } ?: return@composable
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
            val floor = floors.value.find { it.id == floorId } ?: return@composable
            val room = floor.rooms.find { it.id == roomId } ?: return@composable
            DeviceGridScreen(
                room = room,
                onBack = { navController.popBackStack() },
                onToggleDevice = {deviceID ->
                    val device = room.devices.find { it.id == deviceID }
                    device?.let {
                        when (it.type){
                            "outlet" -> it.isOn = !it.isOn
                            "light" -> {
                                Log.d("APP_MESSAGE", "Light toggled: $deviceID, ${it.isOn}")
                                val sharedPref = context.getSharedPreferences("Cookies", Context.MODE_PRIVATE)
                                val req = AppActionRequest(
                                    itemID = it.id.toInt(),
                                    action = "toggle",
                                    value = if (it.isOn) 0 else 1,
                                    sessionID = sharedPref.getString("sessionID", "") ?: ""
                                )
                                try {
                                    coroutineScope.launch {
                                        val response = RetrofitClient.apiService.actionPostRequest(req)
                                        if (response.isSuccessful && response.body()?.response == "success") {
                                            withContext(Dispatchers.Main) {
                                                it.isOn = !it.isOn
                                            }
                                        }

                                    }
                                }catch (e: Exception){
                                    Log.d("API_ERROR", "Network failed")

                                }

                            }
                        }
                        Log.d("APP_MESSAGE", "Device toggled: $deviceID, ${it.isOn}")
                    }

                },
                onAddDeviceClick = {},
                onDeleteDeviceClick = {},
                onMultiSwitchClick = { deviceId ->
                    navController.navigate("floors/$floorId/rooms/$roomId/multiswitch/$deviceId")
                }
            )
        }

        composable(
            "floors/{floorId}/rooms/{roomId}/multiswitch/{deviceId}",
            arguments = listOf(
                navArgument("floorId") { type = NavType.StringType },
                navArgument("roomId") { type = NavType.StringType },
                navArgument("deviceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val floorId = backStackEntry.arguments?.getString("floorId") ?: return@composable
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable

            val floor = floors.value.find { it.id == floorId } ?: return@composable
            val room = floor.rooms.find { it.id == roomId } ?: return@composable
            val device = room.devices.find { it.id == deviceId }

            if (device != null && device.type == "MULTISWITCH") {
                MultiSwitchDetailScreen(
                    multiSwitch = device,
                    onBack = { navController.popBackStack() },
                    onSwitchToggle = { switchId ->
                        val switch = device.switches.find { it.id == switchId }
                        switch?.let {
                            val index = device.switches.indexOf(it)
                            device.switches[index] = it.copy(isOn = !it.isOn)
                            device.status = if (device.switches.any { it.isOn }) {
                                DeviceStatus.ON
                            } else {
                                DeviceStatus.OFF
                            }
                        }
                    }
                )
            } else {
                navController.popBackStack()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorListScreen(
    floors: List<Floor>,
    onFloorClick: (String) -> Unit,
    onAddFloorClick: () -> Unit,
    onDeleteFloorClick: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Floors") },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
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
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
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
fun MultiSwitchDetailScreen(
    multiSwitch: Device,
    onBack: () -> Unit,
    onSwitchToggle: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${multiSwitch.name} - Switches") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when (multiSwitch.status) {
                            DeviceStatus.ON -> Color(0xFF4CAF50)
                            DeviceStatus.OFF -> Color(0xFF9E9E9E)
                            DeviceStatus.ERROR -> Color(0xFFF44336)
                            DeviceStatus.DISCONNECTED -> Color(0xFFFF9800)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = "Device Status: ${multiSwitch.status.name}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Text(
                text = "${multiSwitch.switches.count { it.isOn }}/${multiSwitch.switches.size} switches ON",
                style = MaterialTheme.typography.titleMedium
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(multiSwitch.switches) { switch ->
                    SwitchItem(
                        switch = switch,
                        onToggle = { onSwitchToggle(switch.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SwitchItem(
    switch: SwitchNode,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Switch ${switch.id.takeLast(1)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (switch.isOn) "ON" else "OFF",
                    color = if (switch.isOn) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = switch.isOn,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
fun LightItem(
    switch: SwitchNode,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Light ${switch.id.takeLast(1)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (switch.isOn) "ON" else "OFF",
                    color = if (switch.isOn) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = switch.isOn,
                onCheckedChange = { onToggle() }
            )
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
    onDeleteDeviceClick: (String) -> Unit,
    onMultiSwitchClick: (String) -> Unit
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
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(room.devices) { device ->
                val deviceId = deviceIdOf(device)
                DeviceCard(
                    device = device,
                    onToggle = { onToggleDevice(deviceId) },
                    onDelete = { onDeleteDeviceClick(deviceId) },
                    onMultiSwitchClick = {
                        if (device.type == "MULTISWITCH") {
                            onMultiSwitchClick(deviceId)
                        }
                    }
                )
            }
        }
    }
}

fun deviceIdOf(device: Device): String = device.id

@Composable
fun DeviceCard(
    device: Device,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onMultiSwitchClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable {
                if (device.type == "MULTISWITCH") {
                    onMultiSwitchClick()
                } else {
                    onToggle()
                }
            },
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
                Text(device.name, style = MaterialTheme.typography.titleSmall)
                StatusBadge(device.status)

                when (device.type) {
                    "outlet" -> {
                        Switch(checked = device.isOn, onCheckedChange = { onToggle() })
                    }
                    "light" -> {
                        Switch(checked = device.isOn, onCheckedChange = { onToggle() })
                    }
                    "MULTISWITCH" -> {
                        Text(
                            "${device.switches.count { it.isOn }}/${device.switches.size} on",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    "safety" -> {
                        Text("Max ${device.maxOnDuration / 60} min", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = device.isOn, onCheckedChange = { onToggle() })
                    }
                    "camera" -> {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                    }
                    else ->{
                        Log.d("DeviceCard", "Unknown device type: ${device.type}")
                    }
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
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

//@Preview(showBackground = true)
//@Composable
//fun SmartHomeAppPreview() {
//    MADMiniProjectTheme {
//        SmartHomeApp()
//    }
//}