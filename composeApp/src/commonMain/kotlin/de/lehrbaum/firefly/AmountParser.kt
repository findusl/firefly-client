package de.lehrbaum.firefly

expect fun parseAmount(input: String, useGermanLocale: Boolean = false): Result<ParsedAmount>
