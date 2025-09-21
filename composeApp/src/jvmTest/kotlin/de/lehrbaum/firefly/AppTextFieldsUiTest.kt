package de.lehrbaum.firefly

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import java.awt.GraphicsEnvironment
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category

@OptIn(ExperimentalTestApi::class)
@UiTest
@Category(UiTest::class)
class AppTextFieldsUiTest {
	@get:Rule
	val composeTestRule = run {
		assumeFalse(GraphicsEnvironment.isHeadless())
		createComposeRule()
	}

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
}
