package de.lehrbaum.firefly

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Locale

private fun parseLocaleString(localeString: String): Locale {
	// Accept "de_DE", "de-DE", or full BCP-47 tags. Fallback to default on failure.
	val bcp47 = localeString.replace('_', '-')
	val fromTag = Locale.forLanguageTag(bcp47)
	if (fromTag.language.isNotEmpty()) return fromTag
	val parts = localeString.split('_')
	return when (parts.size) {
		1 -> Locale(parts[0])
		2 -> Locale(parts[0], parts[1])
		3 -> Locale(parts[0], parts[1], parts[2])
		else -> Locale.getDefault()
	}
}

actual fun parseAmount(input: String, locale: String?): Result<ParsedAmount> {
	val trimmedInput = input.trim()
	if (trimmedInput.isEmpty()) return Result.failure(EmptyAmountException())

	val effectiveLocale = locale?.let { parseLocaleString(it) } ?: Locale.getDefault()
	val formatter = NumberFormat.getNumberInstance(effectiveLocale) as? DecimalFormat
		?: return Result.failure(FormatterUnavailableException())
	formatter.isParseBigDecimal = true
	val position = ParsePosition(0)
	val number = formatter.parse(trimmedInput, position) as? BigDecimal
	return if (number != null && position.index == trimmedInput.length) {
		Result.success(number.toPlainString())
	} else {
		Result.failure(InvalidAmountFormatException(trimmedInput))
	}
}
