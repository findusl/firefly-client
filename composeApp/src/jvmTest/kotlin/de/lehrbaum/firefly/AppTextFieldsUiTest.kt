package de.lehrbaum.firefly

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import java.awt.GraphicsEnvironment
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

@OptIn(ExperimentalTestApi::class)
@Category(UiTest::class)
class AppTextFieldsUiTest {
	private val composeRuleDelegate: ComposeContentTestRule? =
		if (GraphicsEnvironment.isHeadless()) null else createComposeRule()

	private val composeTestRule: ComposeContentTestRule
		get() = requireNotNull(composeRuleDelegate) {
			"Compose tests require a displayable environment"
		}

	@get:Rule
	val headlessAwareRule: TestRule = composeRuleDelegate
		?.let { RuleChain.outerRule(HeadlessSkipRule).around(it) }
		?: HeadlessSkipRule

	@Test
	fun displaysAllTextFields() {
		assumeFalse(GraphicsEnvironment.isHeadless())
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
		assumeFalse(GraphicsEnvironment.isHeadless())
		composeTestRule.setContent { App() }

		composeTestRule.onNodeWithText("Save").performClick()

		composeTestRule.waitUntilAtLeastOneExists(hasText("Select a source account"))
		composeTestRule.waitUntilAtLeastOneExists(hasText("Enter a description"))
	}
}
