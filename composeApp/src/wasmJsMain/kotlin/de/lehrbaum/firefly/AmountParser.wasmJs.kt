package de.lehrbaum.firefly

private val allowedAmountRegex = Regex("^[+-]?[0-9]+(\\.[0-9]+)?$")

actual fun parseAmount(input: String, useGermanLocale: Boolean): Result<ParsedAmount> {
	val trimmedInput = input.trim()
	if (trimmedInput.isEmpty()) return Result.failure(EmptyAmountException())

	val sanitized = trimmedInput
		.replace("\u00A0", "")
		.replace(" ", "")

	val (groupingSeparator, decimalSeparator) = if (useGermanLocale) {
		'.' to ','
	} else {
		',' to '.'
	}

	if (sanitized.length > 1 && (sanitized[0] == '+' || sanitized[0] == '-')) {
		if (sanitized.drop(1).contains('+') || sanitized.drop(1).contains('-')) {
			return Result.failure(InvalidAmountFormatException(trimmedInput))
		}
	}

	if (sanitized.count { it == decimalSeparator } > 1) {
		return Result.failure(InvalidAmountFormatException(trimmedInput))
	}

	val decimalIndex = sanitized.indexOf(decimalSeparator)
	if (decimalIndex != -1 && sanitized.indexOf(groupingSeparator, startIndex = decimalIndex) != -1) {
		return Result.failure(InvalidAmountFormatException(trimmedInput))
	}

	if (sanitized.any { it !in charArrayOf('+', '-', groupingSeparator, decimalSeparator) && !it.isDigit() }) {
		return Result.failure(InvalidAmountFormatException(trimmedInput))
	}

	val withoutGrouping = sanitized.replace(groupingSeparator.toString(), "")
	val normalized = if (decimalSeparator == '.') withoutGrouping else withoutGrouping.replace(decimalSeparator, '.')

	return if (allowedAmountRegex.matches(normalized)) {
		Result.success(normalized)
	} else {
		Result.failure(InvalidAmountFormatException(trimmedInput))
	}
}
