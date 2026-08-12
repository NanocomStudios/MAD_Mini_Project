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
}
