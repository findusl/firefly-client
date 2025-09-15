package de.lehrbaum.firefly

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class AutocompleteApiTest {
	@Test
	fun cachesAccounts() =
		runTest {
			var requests = 0
			val engine = MockEngine { _ ->
				requests++
				respond(
					content = """[{"id":"1","name":"Main"}]""",
					status = HttpStatusCode.OK,
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
}
