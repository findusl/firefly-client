package de.lehrbaum.firefly

import java.text.DecimalFormatSymbols
import java.util.Locale

internal actual fun localeSeparators(): LocaleSeparators {
	val symbols = DecimalFormatSymbols.getInstance(Locale.getDefault())
	val groupingSeparators = buildSet {
		val grouping = symbols.groupingSeparator
		if (grouping != Char.MIN_VALUE) add(grouping)
	}
	return LocaleSeparators(
		decimalSeparator = symbols.decimalSeparator,
		groupingSeparators = groupingSeparators,
	)
}
