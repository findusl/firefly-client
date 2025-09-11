package de.lehrbaum.firefly

class Greeting {
	private val platform = getPlatform()

	fun greet(): String = "Hello, ${platform.name}!"
}
