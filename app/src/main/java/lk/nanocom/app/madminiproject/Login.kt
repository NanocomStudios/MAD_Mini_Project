package lk.nanocom.app.madminiproject

import android.util.Log
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.launch
object RetrofitClient {
    private const val BASE_URL = "https://api.nanocom.lk"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

data class LoginRequest(
    val username: String,
    val password: String,
    val deviceID: String?
)

data class LoginResponse(
    val response: String,
    val sessionID: String?,
    val userID: Int
)

interface ApiService {
    @POST("user/login")
    suspend fun loginPostRequest(@Body request: LoginRequest): Response<LoginResponse>
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current

    var uname by remember {
        mutableStateOf("")
    }

    var passwd by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var loginError by remember {
        mutableStateOf(false)
    }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF252525))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                color = Color(0xFFFFFFFF),
                text = "MAD Mini Project",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                color = Color(0xFFFFFFFF),
                text = "Login",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = uname,
                onValueChange = {
                    uname = it
                    loginError = false
                },
                label = {
                    Text("Username")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = passwd,
                onValueChange = {
                    passwd = it
                    loginError = false
                },
                label = {
                    Text("Password")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {

                    IconButton(
                        onClick = {
                            passwordVisible = !passwordVisible
                        }
                    ) {

                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (passwordVisible) {
                                "Hide password"
                            } else {
                                "Show password"
                            }
                        )
                    }
                }
            )

            if (loginError) {
                Text(
                    text = "Invalid username or password",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    val sharedPref = context.getSharedPreferences("Cookies", Context.MODE_PRIVATE)
                    val req = LoginRequest(
                        username = uname,
                        password = passwd,
                        deviceID = sharedPref.getString("firebase_token", "")
                    )

                    // Execute inside a coroutine tied to this composable's lifecycle
                    coroutineScope.launch {
                        try {
                            val response = RetrofitClient.apiService.loginPostRequest(req)

                            if (response.isSuccessful && response.body() != null) {
                                val responseBody = response.body()
                                Log.d("API_SUCCESS", "Login Successful! ID: ${responseBody?.response}")
                                if(responseBody?.response == "success"){
                                    sharedPref.edit{
                                        putString("sessionID", responseBody?.sessionID)
                                        val userID: Int = responseBody?.userID?.toInt() ?: 0

                                        putInt("userID", userID)
                                    }
                                    onLoginSuccess()
                                }else{
                                    loginError = true
                                }

                            } else {
                                Log.e("API_ERROR", "Error Code: ${response.code()}")
                                loginError = true
                            }
                        } catch (e: Exception) {
                            Log.e("API_FAILURE", "Network error occurred", e)
                            loginError = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("LOGIN")

            }
        }
    }
}