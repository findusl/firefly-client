package de.lehrbaum.firefly

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform