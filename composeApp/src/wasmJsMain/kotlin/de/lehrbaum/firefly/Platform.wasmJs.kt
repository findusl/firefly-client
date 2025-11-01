package de.lehrbaum.firefly

import kotlinx.browser.window

class WasmPlatform(private val description: String) : Platform {
	override val name: String = "Wasm ($description)"
}

actual fun getPlatform(): Platform {
	val userAgent = runCatching { window.navigator.userAgent }.getOrElse { "Unknown browser" }
	return WasmPlatform(userAgent)
}
