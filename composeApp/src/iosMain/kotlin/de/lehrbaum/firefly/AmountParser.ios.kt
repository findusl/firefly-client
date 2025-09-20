package de.lehrbaum.firefly

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDecimalNumber
import platform.Foundation.NSLocale
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.currentLocale
import platform.Foundation.decimalValue

@OptIn(ExperimentalForeignApi::class)
actual fun parseAmount(input: String): ParsedAmount? {
	val trimmedInput = input.trim()
	if (trimmedInput.isEmpty()) return null

	val formatter = NSNumberFormatter()
	formatter.locale = NSLocale.currentLocale()
	formatter.numberStyle = NSNumberFormatterDecimalStyle
	val number = formatter.numberFromString(trimmedInput) ?: return null

	val normalized = NSDecimalNumber(decimal = number.decimalValue).description ?: return null
	return ParsedAmount(normalized)
}
