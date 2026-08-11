package lk.nanocom.app.madminiproject

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("user/login")
    suspend fun loginPostRequest(@Body request: LoginRequest): Response<LoginResponse>

    @POST("user/register")
    suspend fun registerPostRequest(@Body request: RegisterRequest): Response<RegisterResponse>
}
