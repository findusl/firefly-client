package de.lehrbaum.firefly

import kotlin.test.Test
import kotlin.test.assertEquals

class AmountParserTest {
	@Test
	fun parsesVerySmallAmountWithoutScientificNotation() {
		val parsed = parseAmount("0,01", "de_DE").getOrThrow()
		assertEquals("0.01", parsed)
	}

	@Test
	fun parsesHighPrecisionAmountWithoutRounding() {
		val input = "123456789012,12"
		val parsed = parseAmount(input, "de_DE").getOrThrow()
		assertEquals("123456789012.12", parsed)
	}
}
