package de.lehrbaum.firefly

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
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
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AppNetworkErrorUiTest {
	@Test
	fun showsBackendErrorMessageFromResponseException() =
		runComposeUiTest {
			initLogger()
			val errorResponse = "Backend rejected transaction"
			val mockEngine = MockEngine { request ->
				val path = request.url.encodedPath
				when {
					path == "/api/v1/transactions" ->
						respond(
							content = errorResponse,
							status = HttpStatusCode.BadRequest,
							headers = headersOf(HttpHeaders.ContentType, "text/plain"),
						)
					else -> error("Unhandled ${request.url}")
				}
			}
			val httpClient = HttpClient(mockEngine) {
				this.expectSuccess = true
				this.install(ContentNegotiation) {
					this.json(Json { this.ignoreUnknownKeys = true })
				}
			}
			val viewModel = MainViewModel(
				client = httpClient,
			)
			val sourceAccount = Account(id = "1", name = "Checking")
			viewModel.sourceField.select(sourceAccount)
			viewModel.targetField.onTextChange("Savings")
			viewModel.descriptionField.onTextChange("Groceries")
			viewModel.amount = "10.00"

			setContent {
				App(viewModelFactory = { viewModel })
			}

			onNodeWithText("Save").performClick()

			val expectedMessage = errorResponse
			waitUntilAtLeastOneExists(
				hasText(expectedMessage, substring = true),
			)
		}
}
