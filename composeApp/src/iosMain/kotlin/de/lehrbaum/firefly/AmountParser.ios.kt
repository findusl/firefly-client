package de.lehrbaum.firefly

import platform.Foundation.NSLocale
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.currentLocale

val usFormatter = NSNumberFormatter().apply {
	locale = NSLocale("en_US_POSIX")
	numberStyle = NSNumberFormatterDecimalStyle
	usesGroupingSeparator = false
}

actual fun parseAmount(input: String, locale: String?): Result<ParsedAmount> {
	val trimmedInput = input.trim()
	if (trimmedInput.isEmpty()) return Result.failure(EmptyAmountException())

	val chosenLocale = if (locale != null) NSLocale(locale) else NSLocale.currentLocale()
	val localParser = NSNumberFormatter().apply {
		this.locale = chosenLocale
		numberStyle = NSNumberFormatterDecimalStyle
		generatesDecimalNumbers = true
		lenient = true // tolerate common minor variations
	}

	val number = localParser.numberFromString(trimmedInput)
		?: return Result.failure(InvalidAmountFormatException(trimmedInput))

	val normalized = usFormatter.stringFromNumber(number) ?: return Result.failure(NormalizationException())

	return Result.success(normalized)
}
