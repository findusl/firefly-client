package de.lehrbaum.firefly

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class AutocompleteApiTest {
	@Test
	fun cachesAccountsWithoutTypes() =
		runTest {
			var requests = 0
			val engine = MockEngine { _ ->
				requests++
				respond(
					content = "[{\"id\":\"1\",\"name\":\"Main\"}]",
					headers = headersOf(HttpHeaders.ContentType, "application/json"),
				)
			}
			val client = HttpClient(engine) {
				install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
			}
			val api = AutocompleteApi(client)
			api.accounts("ma")
			api.accounts("ma")
			assertEquals(1, requests)
		}

	@Test
	fun cachesAccountsSeparatelyByTypes() =
		runTest {
			var requests = 0
			val engine = MockEngine { _ ->
				requests++
				respond(
					content = "[{\"id\":\"1\",\"name\":\"Main\"}]",
					headers = headersOf(HttpHeaders.ContentType, "application/json"),
				)
			}
			val client = HttpClient(engine) {
				install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
			}
			val api = AutocompleteApi(client)
			api.accounts("ma")
			api.accounts("ma", types = listOf("asset"))
			api.accounts("ma", types = listOf("asset"))
			assertEquals(2, requests)
		}

	@Test
	fun includesTypesQueryParameters() =
		runTest {
			var captured: List<String>? = null
			val engine = MockEngine { request ->
				captured = request.url.parameters.getAll("types")
				respond(
					content = "[{\"id\":\"1\",\"name\":\"Main\"}]",
					headers = headersOf(HttpHeaders.ContentType, "application/json"),
				)
			}
			val client = HttpClient(engine) {
				install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
			}
			val api = AutocompleteApi(client)
			api.accounts("ma", types = listOf("asset", "liability"))
			assertEquals(listOf("asset", "liability"), captured)
		}
}
