package de.lehrbaum.firefly

private val allowedAmountRegex = Regex("^[+-]?[0-9]+(\\.[0-9]+)?$")

actual fun parseAmount(input: String, useGermanLocale: Boolean): Result<ParsedAmount> {
	val trimmedInput = input.trim()
	if (trimmedInput.isEmpty()) return Result.failure(EmptyAmountException())

	val sanitized = trimmedInput
		.replace("\u00A0", "")
		.replace(" ", "")

	val withoutGrouping = sanitized.replace(".", "")
	val normalized = withoutGrouping.replace(',', '.')

	return if (allowedAmountRegex.matches(normalized)) {
		Result.success(normalized)
	} else {
		Result.failure(InvalidAmountFormatException(trimmedInput))
	}
}
