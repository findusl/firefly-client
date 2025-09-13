package de.lehrbaum.firefly

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

@Suppress("ktlint", "FunctionName", "unused") // Used from ios project
fun MainViewController(): UIViewController {
	initLogger()
	return ComposeUIViewController { App() }
}
