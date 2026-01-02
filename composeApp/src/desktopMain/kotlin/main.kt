import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cafe.adriel.voyager.navigator.Navigator
import screens.DefaultMain

fun main() = application {
    System.setProperty("compose.interop.blending", "true")

    Window(
        onCloseRequest = ::exitApplication,
        title = "type stuff"
    ) {
            Navigator(DefaultMain)
    }
}