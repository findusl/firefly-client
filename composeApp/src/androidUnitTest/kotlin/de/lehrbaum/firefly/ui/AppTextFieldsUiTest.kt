package de.lehrbaum.firefly.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import de.lehrbaum.firefly.App
import de.lehrbaum.firefly.testing.UiTest
import de.lehrbaum.firefly.testing.UiTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowBuild

@UiTest
@RunWith(RobolectricTestRunner::class)
class AppTextFieldsUiTest {
	@get:Rule(order = 0)
	val skipIfRequested = UiTestRule()

	@Before
	fun setUp() {
		ShadowBuild.setFingerprint("firefly-test")
	}

	@OptIn(ExperimentalTestApi::class)
	@Test
	fun displaysAllTextFields() {
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
				onNodeWithText(label, useUnmergedTree = true).assertIsDisplayed()
			}
		}
	}
}
