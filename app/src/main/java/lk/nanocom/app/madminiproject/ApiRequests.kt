package lk.nanocom.app.madminiproject

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

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

data class ItemInfoRequest(
    val itemID: Int,
    val sessionID: String
)

data class ItemInfoResponse(
    val response: String,
    val itemID: Int,
    val itemName: String,
    val type: String,
    val state: String,
    val lastOnTime: String,
    val cuttofftime: String,
    val error: String? = null

)

data class LogEntry(
    val itemID: Int,
    val state: String,
    val timestamp: String
)

data class ItemLogResponse(
    val response: String,
    val itemID: Int,
    val logs: List<LogEntry>,
    val error: String? = null
)

data class DeviceRequest(
    val floorName: String,
    val roomName: String,
    val itemID: Int,
    val itemName: String = "",
    val sessionID: String
)

data class ScheduleItemRequest(
    val itemID: Int,
    val action: String,
    val value: Int,
    val time_from: String,
    val time_to: String,
    val sessionID: String
)

data class CutoffTimeRequest(
    val itemID: Int,
    val cutoffTime: String,
    val sessionID: String
)


interface ApiService {
    @POST("user/login")
    suspend fun loginPostRequest(@Body request: LoginRequest): Response<LoginResponse>

    @POST("user/register")
    suspend fun registerPostRequest(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("user/validateSession")
    suspend fun validateSessionPostRequest(@Body request: SessionIDRequest): Response<STDResponse>

    @POST("user/updateFirebaseToken")
    suspend fun updateFirebaseTokenPostRequest(@Body request: FirebaseTokenUpdateRequest): Response<STDResponse>

    @POST("user/logout")
    suspend fun logoutPostRequest(@Body request: SessionIDRequest): Response<STDResponse>

    @POST("app/getRooms")
    suspend fun getAppRoomsPostRequest(@Body request: SessionIDRequest): Response<RoomsResponse>

    @POST("app/action")
    suspend fun actionPostRequest(@Body request: AppActionRequest): Response<STDResponse>

    @POST("app/newFloor")
    suspend fun newFloorPostRequest(@Body request: FloorRequest): Response<STDResponse>

    @POST("app/deleteFloor")
    suspend fun deleteFloorPostRequest(@Body request: FloorRequest): Response<STDResponse>

    @POST("app/newRoom")
    suspend fun newRoomPostRequest(@Body request: RoomRequest): Response<STDResponse>

    @POST("app/deleteRoom")
    suspend fun deleteRoomPostRequest(@Body request: RoomRequest): Response<STDResponse>

    @POST("app/addItemToRoom")
    suspend fun addItemToRoomPostRequest(@Body request: DeviceRequest): Response<STDResponse>

    @POST("app/removeItemFromRoom")
    suspend fun removeItemFromRoomPostRequest(@Body request: DeviceRequest): Response<STDResponse>

    @POST("app/getItemInfo")
    suspend fun getItemInfoPostRequest(@Body request: ItemInfoRequest): Response<ItemInfoResponse>

    @POST("app/scheduleItem")
    suspend fun scheduleItemPostRequest(@Body request: ScheduleItemRequest): Response<STDResponse>

    @POST("app/setCutoffTime")
    suspend fun setCutoffTimePostRequest(@Body request: CutoffTimeRequest): Response<STDResponse>

    @POST("app/getItemLog")
    suspend fun getItemLogPostRequest(@Body request: ItemInfoRequest): Response<ItemLogResponse>
}
