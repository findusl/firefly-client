package de.lehrbaum.firefly

expect fun parseAmount(input: String, locale: String? = null): Result<ParsedAmount>
