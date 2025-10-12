package de.lehrbaum.firefly

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AppTextFieldsUiTest {
	private val testHttpClient = HttpClient(MockEngine { _ -> respondBadRequest() }) {
		this.install(ContentNegotiation) {
			this.json(Json { this.ignoreUnknownKeys = true })
		}
	}
	private val testMainViewModel = MainViewModel(testHttpClient, settings = MapSettings())

	@Test
	fun displaysAllTextFields() =
		runComposeUiTest {
			setContent { App(viewModelFactory = { testMainViewModel }) }

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
			setContent { App(viewModelFactory = { testMainViewModel }) }

			onNodeWithText("Save").performClick()

			waitUntilAtLeastOneExists(hasText("Select a source account"))
			waitUntilAtLeastOneExists(hasText("Enter a description"))
		}
}
