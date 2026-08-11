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
data class RegisterRequest(
    val username: String,
    val password: String,
    val deviceID: String?
)

data class RegisterResponse(
    val response: String,
    val error: String?
)


@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit
) {
    val context = LocalContext.current

    var uname by remember {
        mutableStateOf("")
    }

    var passwd by remember {
        mutableStateOf("")
    }

    var passwd_v by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var registerError by remember {
        mutableStateOf(false)
    }

    var registerErrorMsg by remember {
        mutableStateOf("")
    }

    var passwdMismatch by remember {
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
                text = "Register",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = uname,
                onValueChange = {
                    uname = it
                    registerError = false
                    passwdMismatch = false
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
                    registerError = false
                    passwdMismatch = false
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

            OutlinedTextField(
                value = passwd_v,
                onValueChange = {
                    passwd_v = it
                    registerError = false
                    passwdMismatch = false
                },
                label = {
                    Text("Reenter Password")
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

            if (registerError) {
                Text(
                    text = registerErrorMsg,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (passwdMismatch) {
                Text(
                    text = "Passwords do not match!",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    if(passwd != passwd_v){
                        passwdMismatch = true
                    }else {

                        val sharedPref =
                            context.getSharedPreferences("Cookies", Context.MODE_PRIVATE)
                        val req = RegisterRequest(
                            username = uname,
                            password = passwd,
                            deviceID = sharedPref.getString("firebase_token", "")
                        )

                        // Execute inside a coroutine tied to this composable's lifecycle
                        coroutineScope.launch {
                            try {
                                val response = RetrofitClient.apiService.registerPostRequest(req)

                                if (response.isSuccessful && response.body() != null) {
                                    val responseBody = response.body()
                                    Log.d(
                                        "API_SUCCESS",
                                        "Registration Successful! ID: ${responseBody?.response}"
                                    )
                                    if (responseBody?.response == "success") {
                                        onRegisterSuccess()
                                    } else if (responseBody?.response == "failure") {
                                        if (responseBody?.error == null) {
                                            registerErrorMsg = "Registration Failed!"
                                        } else {
                                            registerErrorMsg = responseBody?.error.orEmpty()
                                        }

                                        registerError = true
                                    } else {
                                        registerErrorMsg = "Server Connection Error"
                                        registerError = true
                                    }

                                } else {
                                    Log.e("API_ERROR", "Error Code: ${response.code()}")
                                    registerErrorMsg = "Server Connection Error"
                                    registerError = true
                                }
                            } catch (e: Exception) {
                                Log.e("API_FAILURE", "Network error occurred", e)
                                registerErrorMsg = "Network error occurred"
                                registerError = true
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("REGISTER")

            }
        }
    }
}