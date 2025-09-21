package de.lehrbaum.firefly

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.exitApplication

fun main() {
	initLogger()
	application {
		Window(onCloseRequest = ::exitApplication, title = "Firefly Client") {
			App()
		}
	}
}
