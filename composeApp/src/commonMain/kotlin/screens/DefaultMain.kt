// shared/src/commonMain/kotlin/screens/DefaultScreen.kt
package screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.currentOrThrow

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch




import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.IO
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.String

@Serializable
data class LoginFlask(
    val success: Boolean
)
@Serializable
data class Login(
    val username: String,
    val password: String
)

object DefaultMain : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow

        MaterialTheme {
            var showContent by remember { mutableStateOf(false) }
            var text by remember { mutableStateOf("Initial Text") }
            var text_pw by remember { mutableStateOf("Initial Text") }
            var text_status by remember { mutableStateOf("Please login") }
            val scope = rememberCoroutineScope()

            val httpClient = remember {
                HttpClient {
                    install(ContentNegotiation) {
                        json(Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        })
                    }
                }
            }

            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .safeContentPadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,  // Centers content vertically
                    horizontalAlignment = Alignment.CenterHorizontally // Centers content horizontally
                )


                {

                    TextField(
                        value = text,
                        onValueChange = { newText -> text = newText },
                        label = { Text("Username") }
                    )
                    TextField(
                        value = text_pw,
                        onValueChange = { newText -> text_pw = newText },
                        label = { Text("Password") }
                    )
                    Button(onClick = {
                        scope.launch {
                            try {
                            val response = withContext(Dispatchers.IO){
                                httpClient.post("http://localhost:5000/login"){
                                    contentType(ContentType.Application.Json)
                                    setBody(Login(
                                        username = text,
                                        password = text_pw
                                    ))

                                }

                            }
                                if (response.status.isSuccess()){
                                    val body = response.body<LoginFlask>()
                                    if (body.success) {
                                        // 4. Switch to Main thread only for UI changes/Navigation
                                        withContext(Dispatchers.Main) {
                                            navigator.push(CheckOut)
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            // Update a 'var errorMessage' to show on screen
                                            text_status = "Login failed: Invalid credentials"
                                        }
                                    }
                                }
                            } catch (e: Exception){
                                println("Error: ${e.message}")
                            }
                        }

                    }) {
                        Text("Login")
                    }
                    Text(text_status)
                }

            }
        }
    }
}