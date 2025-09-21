package de.lehrbaum.firefly

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Locale

actual fun parseAmount(input: String, useGermanLocale: Boolean): Result<ParsedAmount> {
	val trimmedInput = input.trim()
	if (trimmedInput.isEmpty()) return Result.failure(EmptyAmountException())

	val effectiveLocale = if (useGermanLocale) Locale.GERMANY else Locale.getDefault()
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
