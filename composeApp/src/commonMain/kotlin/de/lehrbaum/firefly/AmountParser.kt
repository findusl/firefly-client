package de.lehrbaum.firefly

internal data class LocaleSeparators(
	val decimalSeparator: Char,
	val groupingSeparators: Set<Char>,
)

internal expect fun localeSeparators(): LocaleSeparators

internal object AmountParser {
	private val additionalGroupingSeparators = setOf(' ', '\u00A0', '\u202F', Char(0x27))

	fun parseToApiFormat(rawAmount: String, separators: LocaleSeparators = localeSeparators()): String {
		val trimmed = rawAmount.trim()
		if (trimmed.isEmpty()) return trimmed

		val localeDecimal = separators.decimalSeparator
		val groupingSeparators = (separators.groupingSeparators + additionalGroupingSeparators)
			.filter { it != localeDecimal }
			.toSet()

		val detectedDecimal = detectDecimalSeparator(trimmed, localeDecimal, groupingSeparators)

		val cleaned = buildString(trimmed.length) {
			for (character in trimmed) {
				if (groupingSeparators.contains(character) && character != detectedDecimal) continue
				append(character)
			}
		}

		val replacedDecimal = when {
			detectedDecimal == null || detectedDecimal == '.' -> cleaned
			else -> cleaned.replace(detectedDecimal, '.')
		}

		val normalized = StringBuilder(replacedDecimal.length)
		var decimalAdded = false
		for (character in replacedDecimal) {
			when {
				character.isDigit() -> normalized.append(character)
				character == '-' && normalized.isEmpty() -> normalized.append(character)
				character == '+' && normalized.isEmpty() -> {} // Ignore leading plus signs
				character == '.' && !decimalAdded -> {
					if (normalized.isEmpty() || (normalized.length == 1 && normalized[0] == '-')) {
						normalized.append('0')
					}
					normalized.append('.')
					decimalAdded = true
				}
			}
		}
		val result = normalized.toString()
		return if (result.isNotEmpty() && result != "-") result else trimmed
	}

	private fun detectDecimalSeparator(
		value: String,
		localeDecimal: Char,
		groupingSeparators: Set<Char>,
	): Char? {
		val localeIndex = value.lastIndexOf(localeDecimal)
		if (localeIndex >= 0 && digitsAfter(value, localeIndex) > 0) {
			return localeDecimal
		}

		val lastDotIndex = value.lastIndexOf('.')
		val lastCommaIndex = value.lastIndexOf(',')

		if (lastDotIndex >= 0 && lastCommaIndex >= 0) {
			val candidateIndex = maxOf(lastDotIndex, lastCommaIndex)
			val candidate = value[candidateIndex]
			if (digitsAfter(value, candidateIndex) > 0 && candidate !in groupingSeparators) {
				return candidate
			}
		}

		if (lastDotIndex >= 0) {
			val digitsAfterDot = digitsAfter(value, lastDotIndex)
			if (digitsAfterDot > 0 && '.' !in groupingSeparators) {
				return '.'
			}
		}

		if (lastCommaIndex >= 0) {
			val digitsAfterComma = digitsAfter(value, lastCommaIndex)
			if (digitsAfterComma > 0 && ',' !in groupingSeparators) {
				return ','
			}

			if (lastCommaIndex >= 0 && digitsAfterComma in 1..2) {
				return ','
			}
		}

		return null
	}

	private fun digitsAfter(value: String, index: Int): Int = value.substring(index + 1).count { it.isDigit() }
}
