package de.lehrbaum.firefly

class JvmPlatform : Platform {
	override val name: String = "JVM ${System.getProperty("os.name") ?: "Unknown OS"}"
}

actual fun getPlatform(): Platform = JvmPlatform()
