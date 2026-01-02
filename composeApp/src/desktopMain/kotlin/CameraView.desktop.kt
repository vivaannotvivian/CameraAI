package screens

import androidx.compose.foundation.background
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.sarxos.webcam.Webcam
import com.github.sarxos.webcam.WebcamPanel
import com.github.sarxos.webcam.WebcamResolution
import com.github.sarxos.webcam.ds.javacv.JavaCvDriver
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import java.util.Locale
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape

@Serializable
data class StreamResponse(
    val success: Boolean,
    val scanned: Boolean = false,
    val book_name: String? = null,
    val return_status: String? = null,
    val timestamp: String? = null,
    val error: String? = null
)

@Composable
actual fun CameraView(
    cameraOpened: Boolean,
    cameraSelected: CameraSelected,
    hazeState: HazeState
) {
    var scannedBook by remember { mutableStateOf<String?>(null) }
    var scannedBookStatus by remember { mutableStateOf<String?>(null) }

    val os = System.getProperty("os.name").lowercase(Locale.getDefault())
    var webcamError by remember { mutableStateOf<Throwable?>(null) }
    var currentWebcamNumber by mutableStateOf(0)

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

    val serverUrl = "http://localhost:5000"

    // Set driver before getting webcams
    LaunchedEffect(Unit) {
        if (os.startsWith("mac")) {
            Webcam.setDriver(JavaCvDriver::class.java)
        }
    }

    val webcams = remember {
        runCatching { Webcam.getWebcams() }.getOrElse {
            webcamError = it
            null
        }
    }

    if (!webcams.isNullOrEmpty()) {
        key(cameraSelected) {
            if (webcams.size > 1) {
                currentWebcamNumber = when (cameraSelected) {
                    CameraSelected.RearOrDefaultWebcam -> 0
                    CameraSelected.SelfieOrAdditionalWebcam -> 1
                    else -> 0
                }
            }
        }

        val webcam = remember(currentWebcamNumber) { webcams[currentWebcamNumber] }

        val panel = remember(webcam) {
            webcam.viewSize = WebcamResolution.VGA.size
            WebcamPanel(webcam, false).apply {
                isFPSDisplayed = true
                isMirrored = true
                start()
            }
        }

        // Reset scans on start
        LaunchedEffect(Unit) {
            try {
                httpClient.post("$serverUrl/reset-scans")
            } catch (e: Exception) {
                println("❌ Reset failed: ${e.message}")
            }
        }

        // Streaming Loop
        LaunchedEffect(cameraOpened, webcam) {
            if (cameraOpened) {
                withContext(Dispatchers.IO) {
                    while (isActive) {
                        try {
                            val image = webcam.image
                            if (image != null) {
                                val outputStream = ByteArrayOutputStream()
                                ImageIO.write(image, "jpg", outputStream)
                                val frameBytes = outputStream.toByteArray()

                                val response = httpClient.post("$serverUrl/video-stream") {
                                    contentType(ContentType.Application.OctetStream)
                                    setBody(frameBytes)
                                }

                                if (response.status.isSuccess()) {
                                    val body = response.body<StreamResponse>()
                                    if (body.scanned && body.book_name != null) {
                                        withContext(Dispatchers.Main) {
                                            scannedBook = body.book_name
                                            scannedBookStatus = body.return_status
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println("✗ Stream error: ${e.message}")
                        }
                        delay(100) // Stabilize at ~10 FPS
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (cameraOpened) {
                SwingPanel(
                    factory = { panel },
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(
                            state = hazeState,
                            style = HazeStyle(
                                tint = Color.Black.copy(alpha = .2f),
                                blurRadius = 30.dp,
                                noiseFactor = HazeDefaults.noiseFactor
                            )
                        )
                )
            } else {
                ClosedCameraView(hazeState = hazeState)
            }

            // Only show overlay if a book is actually scanned
            if (scannedBook != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                        .background(Color(0xAAFF6600), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "$scannedBook status: $scannedBookStatus",
                        color = Color.White
                    )
                }
            }
        }

        DisposableEffect(webcam) {
            onDispose {
                panel.stop()
                webcam.close()
            }
        }
    } else {
        Text(text = "Oops: ${webcamError?.message ?: "No webcams detected."}")
    }
}