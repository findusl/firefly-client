package de.lehrbaum.firefly

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlin.system.exitProcess

fun main() {
	initLogger()
	application {
		Window(onCloseRequest = { exitProcess(0) }, title = "Firefly Client") {
			App()
		}
	}
}
