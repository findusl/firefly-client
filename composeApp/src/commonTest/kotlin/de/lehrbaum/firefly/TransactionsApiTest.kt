package de.lehrbaum.firefly

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class TransactionsApiTest {
	@Test
	fun createTransaction_setsJsonContentType() =
		runTest {
			var recordedContentType: String? = null
			val engine = MockEngine { request ->
				val content = request.body as OutgoingContent
				recordedContentType = content.contentType?.toString()
				respondOk()
			}
			val client = HttpClient(engine) {
				install(ContentNegotiation) {
					json(Json { ignoreUnknownKeys = true })
				}
			}
			val source = Account("1", "Source", "asset")
			createTransaction(client, source, "Target", null, "desc", "1")
			assertEquals("application/json", recordedContentType)
		}
}
