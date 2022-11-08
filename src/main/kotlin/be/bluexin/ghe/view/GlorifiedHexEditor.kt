package be.bluexin.ghe.view

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import be.bluexin.ghe.view.shell.App
import mu.KotlinLogging

val logger = KotlinLogging.logger {}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Glorified Hex Editor") { App() }
}
