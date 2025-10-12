package de.lehrbaum.firefly

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AppTextFieldsUiTest {
	@get:Rule
	val composeTestRule = createComposeRule()

	@Test
	fun displaysAllTextFields() {
		composeTestRule.setContent { App() }

		listOf(
			"Source account",
			"Target account",
			"Description",
			"Tag (optional)",
			"Amount",
			"Date & Time",
		).forEach { label ->
			composeTestRule.waitUntilAtLeastOneExists(hasText(label))
		}
	}

	@Test
	fun showsErrorsWhenRequiredFieldsMissing() {
		composeTestRule.setContent { App() }

		composeTestRule.onNodeWithText("Save").performClick()

		composeTestRule.waitUntilAtLeastOneExists(hasText("Select a source account"))
		composeTestRule.waitUntilAtLeastOneExists(hasText("Enter a description"))
	}
}
