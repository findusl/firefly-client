package de.lehrbaum.firefly

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AmountParserTest {
	@Test
	fun parsesVerySmallAmountWithoutScientificNotation() =
		withEnglishLocale {
			val parsed = parseAmount("0.0000001")
			val plain = assertNotNull(parsed).plainString
			assertEquals("0.0000001", plain)
		}

	@Test
	fun parsesHighPrecisionAmountWithoutRounding() =
		withEnglishLocale {
			val input = "12345678901234567890.123456789"
			val parsed = parseAmount(input)
			val plain = assertNotNull(parsed).plainString
			assertEquals(input, plain)
		}
}
