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
import androidx.compose.material3.FloatingActionButton
import lk.nanocom.app.madminiproject.ui.theme.MADMiniProjectTheme
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.FabPosition
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

data class DeviceToggleEvent(
    val deviceId: String,
    val isOn: Boolean
)

object FCMEventManager {
    private val _deviceEvents = MutableSharedFlow<DeviceToggleEvent>(extraBufferCapacity = 10)
    val deviceEvents = _deviceEvents.asSharedFlow()

    fun emitEvent(event: DeviceToggleEvent) {
        _deviceEvents.tryEmit(event)
    }
}

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

data class FloorRequest(
    val floorName: String,
    val sessionID: String
)

data class RoomRequest(
    val floorName: String,
    val roomName: String,
    val sessionID: String
)

data class DeviceRequest(
    val floorName: String,
    val roomName: String,
    val itemID: Int,
    val sessionID: String
)


fun isAppInBackground(): Boolean {
    val currentState = ProcessLifecycleOwner.get().lifecycle.currentState
    return !currentState.isAtLeast(Lifecycle.State.STARTED)
}

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
    val isOn: Boolean = false,
    val status: DeviceStatus = DeviceStatus.OFF,
    val switches: List<SwitchNode>? = emptyList(),
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

        FCMEventManager.deviceEvents.collect { event ->
            Log.d("FCM_EVENT", "Updating UI for device ${event.deviceId} to ${event.isOn}")
            floors.value = floors.value.map { floor ->
                floor.copy(rooms = floor.rooms.map { room ->
                    room.copy(devices = room.devices.map { device ->
                        if (device.id == event.deviceId) {
                            device.copy(
                                isOn = event.isOn,
                                status = if (event.isOn) DeviceStatus.ON else DeviceStatus.OFF
                            )
                        } else device
                    })
                })
            }
        }
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
                onRefresh = loadFloors,
                onStatsClick = { navController.navigate("statistics") },
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
            "floors/new",
        ) {
            AddNewFloorPopup()
        }

        composable (
            route = "statistics",
        ) {
            StatScreen()
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
                onRefresh = loadFloors
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
                floor = floor,
                room = room,
                onBack = { navController.popBackStack() },
                onRefresh = { loadFloors() },
                onToggleDevice = { deviceID ->
                    Log.d("APP_DEBUG", "onToggleDevice called for ID: $deviceID")
                    val device = room.devices.find { it.id == deviceID }
                    device?.let { d ->
                        val sharedPref = context.getSharedPreferences("Cookies", Context.MODE_PRIVATE)
                        val sessionID = sharedPref.getString("sessionID", "") ?: ""

                        val updateState = {
                            Log.d("APP_DEBUG", "Performing state update for $deviceID")
                            floors.value = floors.value.map { f ->
                                if (f.id == floorId) {
                                    f.copy(rooms = f.rooms.map { r ->
                                        if (r.id == roomId) {
                                            r.copy(devices = r.devices.map { dev ->
                                                if (dev.id == deviceID) {
                                                    val newIsOn = !dev.isOn
                                                    Log.d("APP_DEBUG", "Found device, toggling to: $newIsOn")
                                                    dev.copy(
                                                        isOn = newIsOn,
                                                        status = if (newIsOn) DeviceStatus.ON else DeviceStatus.OFF
                                                    )
                                                } else dev
                                            })
                                        } else r
                                    })
                                } else f
                            }
                        }

                        when (d.type) {
                            "outlet" -> {
                                updateState()
                            }
                            "light" -> {
                                Log.d("APP_DEBUG", "Sending toggle request for light: $deviceID")
                                val req = AppActionRequest(
                                    itemID = d.id.toIntOrNull() ?: 0,
                                    action = "toggle",
                                    value = if (d.isOn) 0 else 1,
                                    sessionID = sessionID
                                )
                                coroutineScope.launch {
                                    try {
                                        val response = RetrofitClient.apiService.actionPostRequest(req)
                                        Log.d("APP_DEBUG", "API Response: ${response.code()}, Body: ${response.body()}")
                                        if (response.isSuccessful && response.body()?.response?.lowercase() == "success") {
                                            withContext(Dispatchers.Main) {
                                                updateState()
                                            }
                                        } else {
                                            Log.e("APP_DEBUG", "API Toggle Failed: ${response.body()?.error}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("APP_DEBUG", "Network Exception: ${e.message}")
                                    }
                                }
                            }
                            else -> {
                                // Default behavior for other types if toggle is needed
                                updateState()
                            }
                        }
                    } ?: Log.e("APP_DEBUG", "Device $deviceID not found in room $roomId")
                },
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
                        Log.d("APP_DEBUG", "onSwitchToggle called for switch: $switchId on device: $deviceId")
                        floors.value = floors.value.map { f ->
                            if (f.id == floorId) {
                                f.copy(rooms = f.rooms.map { r ->
                                    if (r.id == roomId) {
                                        r.copy(devices = r.devices.map { dev ->
                                            if (dev.id == deviceId) {
                                                val newSwitches = dev.switches?.map { s ->
                                                    if (s.id == switchId) s.copy(isOn = !s.isOn) else s
                                                }
                                                Log.d("APP_DEBUG", "Updated switch $switchId")
                                                dev.copy(
                                                    switches = newSwitches,
                                                    status = if (newSwitches?.any { it.isOn } == true) DeviceStatus.ON else DeviceStatus.OFF
                                                )
                                            } else dev
                                        })
                                    } else r
                                })
                            } else f
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
    onRefresh: () -> Unit,
    onStatsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedFloor by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val sharedPref = LocalContext.current.getSharedPreferences("Cookies", Context.MODE_PRIVATE)
    val saved_sessionID: String = sharedPref.getString("sessionID", "") ?: ""

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showAddDialog) {
        var textInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = "Add New Floor") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Floor Name:")
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Floor") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                        )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if(textInput.isEmpty()) return@Button
                        val req = FloorRequest(
                            floorName = textInput,
                            sessionID = saved_sessionID
                        )
                        coroutineScope.launch {
                            try {
                                val response = RetrofitClient.apiService.newFloorPostRequest(req)
                                if (response.isSuccessful && response.body()?.response == "success") {
                                    onRefresh()
                                    showAddDialog = false
                                }else{
                                    errorMessage = response.body()?.error ?: "Unknown error"
                                }
                            } catch (e: Exception) {
                                Log.d("API_ERROR", "Failed to add new floor")
                                errorMessage = "Failed to add new floor"
                            }
                        }

                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Are you sure?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val req = FloorRequest(
                        floorName = selectedFloor,
                        sessionID = saved_sessionID
                    )
                    coroutineScope.launch {
                        try {
                            val response = RetrofitClient.apiService.deleteFloorPostRequest(req)
                            if (response.isSuccessful && response.body()?.response == "success") {
                                onRefresh()
                            }
                        } catch (e: Exception) {
                            Log.d("API_ERROR", "Failed to delete floor")
                        }
                    }
                    showDeleteDialog = false
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("No")
                }
            }
        )
    }
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
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FloatingActionButton(
                    onClick = onStatsClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Filled.BarChart,
                        contentDescription = "Statistics"
                    )
                }

                FloatingActionButton(
                    onClick = {
                        showAddDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add"
                    )
                }
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
                            onClick = {
                                selectedFloor = floor.name
                                showDeleteDialog = true
                            },
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
    onRefresh: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedRoom by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("")}

    val sharedPref = LocalContext.current.getSharedPreferences("Cookies", Context.MODE_PRIVATE)
    val saved_sessionID: String = sharedPref.getString("sessionID", "") ?: ""

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showAddDialog) {
        var textInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = "Add New Room") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Room Name:")
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Room") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if(textInput.isEmpty()) return@Button
                        val req = RoomRequest(
                            floorName = floor.name,
                            roomName = textInput,
                            sessionID = saved_sessionID
                        )
                        coroutineScope.launch {
                            try {
                                val response = RetrofitClient.apiService.newRoomPostRequest(req)
                                if (response.isSuccessful && response.body()?.response == "success") {
                                    onRefresh()
                                    showAddDialog = false
                                }else{
                                    errorMessage = response.body()?.error ?: "Unknown error"
                                }
                            } catch (e: Exception) {
                                Log.d("API_ERROR", "Failed to add new room")
                                errorMessage = "Failed to add new room"
                            }
                        }
                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Are you sure?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val req = RoomRequest(
                        floorName = floor.name,
                        roomName = selectedRoom,
                        sessionID = saved_sessionID
                    )
                    coroutineScope.launch {
                        try {
                            val response = RetrofitClient.apiService.deleteRoomPostRequest(req)
                            if (response.isSuccessful && response.body()?.response == "success") {
                                onRefresh()
                            }
                        } catch (e: Exception) {
                            Log.d("API_ERROR", "Failed to add new floor")
                        }
                    }
                    showDeleteDialog = false
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("No")
                }
            }
        )
    }

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
                onClick = {
                    showAddDialog = true
                },
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
                            onClick = {
                                selectedRoom = room.name
                                showDeleteDialog = true
                            },
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
                text = "${multiSwitch.switches?.count { it.isOn } ?: 0}/${multiSwitch.switches?.size ?: 0} switches ON",
                style = MaterialTheme.typography.titleMedium
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(multiSwitch.switches ?: emptyList()) { switch ->
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
    floor: Floor,
    room: Room,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleDevice: (String) -> Unit,
    onMultiSwitchClick: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf("")}

    val sharedPref = LocalContext.current.getSharedPreferences("Cookies", Context.MODE_PRIVATE)
    val saved_sessionID: String = sharedPref.getString("sessionID", "") ?: ""

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showAddDialog) {
        var textInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = "Add New Item") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Item ID:")
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Item ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if(textInput.isEmpty()) return@Button
                        val req = DeviceRequest(
                            floorName = floor.name,
                            roomName = room.name,
                            itemID = textInput.toIntOrNull() ?: -1,
                            sessionID = saved_sessionID
                        )
                        coroutineScope.launch {
                            try {
                                val response = RetrofitClient.apiService.addItemToRoomPostRequest(req)
                                if (response.isSuccessful && response.body()?.response == "success") {
                                    onRefresh()
                                    showAddDialog = false
                                }else{
                                    errorMessage = response.body()?.error ?: "Unknown error"
                                }
                            } catch (e: Exception) {
                                Log.d("API_ERROR", "Failed to add new room")
                                errorMessage = "Failed to add new item"
                            }
                        }
                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Are you sure?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val req = DeviceRequest(
                        floorName = floor.name,
                        roomName = room.name,
                        itemID = selectedDevice,
                        sessionID = saved_sessionID
                    )
                    coroutineScope.launch {
                        try {
                            val response = RetrofitClient.apiService.removeItemFromRoomPostRequest(req)
                            if (response.isSuccessful && response.body()?.response == "success") {
                                onRefresh()
                            }
                        } catch (e: Exception) {
                            Log.d("API_ERROR", "Failed to add new floor")
                        }
                    }
                    showDeleteDialog = false
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("No")
                }
            }
        )
    }
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
                onClick = {
                    showAddDialog = true
                },
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
                    onDelete = {
                        selectedDevice = deviceId.toInt()
                        showDeleteDialog = true},
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
                            "${device.switches?.count { it.isOn } ?: 0}/${device.switches?.size ?: 0} on",
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