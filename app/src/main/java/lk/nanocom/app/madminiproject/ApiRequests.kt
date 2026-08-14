package lk.nanocom.app.madminiproject

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

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
}
