package de.lehrbaum.firefly

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AmountParserWasmTest {
	@Test
	fun parsesEnglishFormattedAmount() {
		val parsed = parseAmount("1,234.56", useGermanLocale = false).getOrThrow()
		assertEquals("1234.56", parsed)
	}

	@Test
	fun parsesGermanFormattedAmount() {
		val parsed = parseAmount("1.234,56", useGermanLocale = true).getOrThrow()
		assertEquals("1234.56", parsed)
	}

	@Test
	fun rejectsMismatchedSeparators() {
		assertFailsWith<InvalidAmountFormatException> {
			parseAmount("1.234,56", useGermanLocale = false).getOrThrow()
		}
	}
}
