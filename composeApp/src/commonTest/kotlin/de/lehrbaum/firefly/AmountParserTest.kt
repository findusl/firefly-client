package de.lehrbaum.firefly

import kotlin.test.Test
import kotlin.test.assertEquals

class AmountParserTest {
	private val germanSeparators = LocaleSeparators(decimalSeparator = ',', groupingSeparators = setOf('.'))
	private val englishSeparators = LocaleSeparators(decimalSeparator = '.', groupingSeparators = setOf(','))

	@Test
	fun parsesGermanAmountWithThousands() {
		val result = AmountParser.parseToApiFormat("1.234,56", germanSeparators)
		assertEquals("1234.56", result)
	}

	@Test
	fun parsesEnglishAmountWithThousands() {
		val result = AmountParser.parseToApiFormat("1,234.56", englishSeparators)
		assertEquals("1234.56", result)
	}

	@Test
	fun parsesNegativeAmount() {
		val result = AmountParser.parseToApiFormat("-1.234,56", germanSeparators)
		assertEquals("-1234.56", result)
	}

	@Test
	fun handlesLeadingDecimal() {
		val result = AmountParser.parseToApiFormat(",5", germanSeparators)
		assertEquals("0.5", result)
	}

	@Test
	fun stripsSpaces() {
		val result = AmountParser.parseToApiFormat("1 234,56", germanSeparators)
		assertEquals("1234.56", result)
	}

	@Test
	fun leavesInvalidAmountUntouched() {
		val input = "abc"
		val result = AmountParser.parseToApiFormat(input, germanSeparators)
		assertEquals(input, result)
	}

	@Test
	fun treatsGermanThousandsWithoutDecimalCorrectly() {
		val result = AmountParser.parseToApiFormat("1.234", germanSeparators)
		assertEquals("1234", result)
	}

	@Test
	fun acceptsCommaDecimalInEnglishWhenClearIntent() {
		val result = AmountParser.parseToApiFormat("1,23", englishSeparators)
		assertEquals("1.23", result)
	}
}
