package de.lehrbaum.firefly

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSDecimal
import platform.Foundation.NSDecimalString
import platform.Foundation.NSLocale
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.currentLocale

actual fun parseAmount(input: String): ParsedAmount? {
	val trimmedInput = input.trim()
	if (trimmedInput.isEmpty()) return null

	val formatter = NSNumberFormatter()
	formatter.locale = NSLocale.currentLocale()
	formatter.numberStyle = NSNumberFormatterDecimalStyle
	val number = formatter.numberFromString(trimmedInput) ?: return null

	val decimalValue = number.decimalValue
	val normalized = memScoped {
		val decimal = alloc<NSDecimal>()
		decimal.value = decimalValue
		NSDecimalString(decimal.ptr, null)
	}
	return ParsedAmount(normalized)
}
