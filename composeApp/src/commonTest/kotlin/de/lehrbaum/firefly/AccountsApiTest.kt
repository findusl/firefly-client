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

class AccountsApiTest {
	@Test
	fun fetchAccounts_parsesIdAndName() =
		runTest {
			val engine = MockEngine { _ ->
				respond(
					content = ACCOUNTS_JSON,
					headers = headersOf(HttpHeaders.ContentType, "application/json"),
				)
			}
			val client = HttpClient(engine) {
				install(ContentNegotiation) {
					json(Json { ignoreUnknownKeys = true })
				}
			}
			val accounts = fetchAccounts(client)
			assertEquals(listOf(Account("1", "Main account")), accounts)
		}
}

private const val ACCOUNTS_JSON = """{
	"data": [
		{
			"type": "accounts",
			"id": "1",
			"attributes": {
				"name": "Main account"
			}
		}
	]
}
"""
