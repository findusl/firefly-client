package de.lehrbaum.firefly

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.russhwolf.settings.MapSettings
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AppTextFieldsUiTest {
	@Test
	fun displaysAllTextFields() =
		runComposeUiTest {
			val settings = MapSettings()
			setContent { App(settings = settings) }

			listOf(
				"Source account",
				"Target account",
				"Description",
				"Tag (optional)",
				"Amount",
				"Date & Time",
			).forEach { label ->
				waitUntilAtLeastOneExists(hasText(label))
			}
		}

	@Test
	fun showsErrorsWhenRequiredFieldsMissing() =
		runComposeUiTest {
			val settings = MapSettings()
			setContent { App(settings = settings) }

			onNodeWithText("Save").performClick()

			waitUntilAtLeastOneExists(hasText("Select a source account"))
			waitUntilAtLeastOneExists(hasText("Enter a description"))
		}
}
