package de.lehrbaum.firefly

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AmountParserAndroidTest {
	private var previousLocale: Locale? = null

	@BeforeTest
	fun setUp() {
		previousLocale = Locale.getDefault()
		Locale.setDefault(Locale.US)
	}

	@AfterTest
	fun tearDown() {
		previousLocale?.let { Locale.setDefault(it) }
	}

	@Test
	fun parsesVerySmallAmountWithoutScientificNotation() {
		val parsed = parseAmount("0.0000001")
		val plain = assertNotNull(parsed).plainString
		assertEquals("0.0000001", plain)
	}

	@Test
	fun parsesHighPrecisionAmountWithoutRounding() {
		val input = "12345678901234567890.123456789"
		val parsed = parseAmount(input)
		val plain = assertNotNull(parsed).plainString
		assertEquals(input, plain)
	}
}
