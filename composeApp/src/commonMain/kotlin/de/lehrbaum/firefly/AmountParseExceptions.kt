package de.lehrbaum.firefly

sealed class AmountParseException(message: String) : IllegalArgumentException(message)

class EmptyAmountException : AmountParseException("Amount is empty")

class FormatterUnavailableException : AmountParseException("Decimal formatter unavailable")

class InvalidAmountFormatException(val input: String) : AmountParseException("Invalid amount format: '$input'")

class NormalizationException : AmountParseException("Failed to normalize amount")
