package de.lehrbaum.firefly

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import org.junit.Test
import org.junit.experimental.categories.Category

@OptIn(ExperimentalTestApi::class)
@Category(UiTest::class)
class AppTextFieldsUiTest {
	@Test
	fun displaysAllTextFields() =
		runComposeUiTest {
			setContent { App() }

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
}
