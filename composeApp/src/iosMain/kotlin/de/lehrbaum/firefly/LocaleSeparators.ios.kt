package de.lehrbaum.firefly

import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle

internal actual fun localeSeparators(): LocaleSeparators {
	val formatter = NSNumberFormatter().apply {
		numberStyle = NSNumberFormatterDecimalStyle
	}
	val decimalSeparator = formatter.decimalSeparator?.firstOrNull() ?: '.'
	val groupingSeparators = buildSet {
		formatter.groupingSeparator?.firstOrNull()?.let { add(it) }
	}
	return LocaleSeparators(
		decimalSeparator = decimalSeparator,
		groupingSeparators = groupingSeparators,
	)
}
