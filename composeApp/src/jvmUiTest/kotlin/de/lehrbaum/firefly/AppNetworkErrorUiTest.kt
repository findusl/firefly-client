package de.lehrbaum.firefly

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AppNetworkErrorUiTest {
	@get:Rule
	val composeTestRule = createComposeRule()

	@Test
	fun showsBackendErrorMessageFromResponseException() {
		val mockEngine = MockEngine { request ->
			val path = request.url.encodedPath
			when {
				path == "/api/v1/transactions" ->
					respond(
						content = "Backend rejected transaction",
						status = HttpStatusCode.BadRequest,
						headers = headersOf(HttpHeaders.ContentType, "text/plain"),
					)
				path in setOf(
					"/api/v1/autocomplete/accounts",
					"/api/v1/autocomplete/transactions",
					"/api/v1/autocomplete/tags",
				) ->
					respond(
						content = "[]",
						headers = headersOf(HttpHeaders.ContentType, "application/json"),
					)
				else -> error("Unhandled ${'$'}{request.url}")
			}
		}
		val httpClient = HttpClient(mockEngine) {
			install(ContentNegotiation) {
				json(Json { ignoreUnknownKeys = true })
			}
		}
		val viewModel = MainViewModel(
			client = httpClient,
			autocompleteApi = AutocompleteApi(httpClient),
		)
		val sourceAccount = Account(id = "1", name = "Checking")
		viewModel.sourceField.select(sourceAccount)
		viewModel.targetField.onTextChange("Savings")
		viewModel.descriptionField.onTextChange("Groceries")
		viewModel.amount = "10.00"

		composeTestRule.setContent {
			App(viewModelFactory = { viewModel })
		}

		composeTestRule.onNodeWithText("Save").performClick()

		val expectedMessage =
			"Request to https://firefly.lehrenko.de/api/v1/transactions failed (400 Bad Request): Backend rejected transaction"
		composeTestRule.waitUntilAtLeastOneExists(
			hasText(expectedMessage),
		)
	}
}
