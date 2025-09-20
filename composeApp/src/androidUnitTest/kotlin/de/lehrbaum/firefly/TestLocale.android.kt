package de.lehrbaum.firefly

import java.util.Locale

actual fun withEnglishLocale(block: () -> Unit) {
	val previousLocale = Locale.getDefault()
	Locale.setDefault(Locale.US)
	try {
		block()
	} finally {
		Locale.setDefault(previousLocale)
	}
}
