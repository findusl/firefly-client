package de.lehrbaum.firefly

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Locale

actual fun parseAmount(input: String): ParsedAmount? {
	val trimmedInput = input.trim()
	if (trimmedInput.isEmpty()) return null

	val formatter = NumberFormat.getNumberInstance(Locale.getDefault()) as? DecimalFormat ?: return null
	formatter.isParseBigDecimal = true
	val position = ParsePosition(0)
	val number = formatter.parse(trimmedInput, position) as? BigDecimal
	return if (number != null && position.index == trimmedInput.length) {
		ParsedAmount(number.toPlainString())
	} else {
		null
	}
}
