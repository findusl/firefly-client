package de.lehrbaum.firefly

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

@Serializable
data class Account(
	val id: String,
	val name: String,
)

@Serializable
data class TransactionSuggestion(
	val id: String,
	val description: String,
)

private data class CacheEntry<T>(val value: T)

class AutocompleteApi(
	private val client: HttpClient,
) {
	private val accountCache = mutableMapOf<String, CacheEntry<List<Account>>>()
	private val transactionCache = mutableMapOf<String, CacheEntry<List<TransactionSuggestion>>>()

	suspend fun accounts(query: String): List<Account> {
		accountCache[query]?.let { return it.value }
		Napier.i("Fetching accounts autocomplete for '$query'")
		val response: List<Account> = client
			.get("${BuildKonfig.BASE_URL}/api/v1/autocomplete/accounts") {
				header(HttpHeaders.Authorization, "Bearer ${BuildKonfig.ACCESS_TOKEN}")
				accept(ContentType.Application.Json)
				parameter("query", query)
			}.body()
		accountCache[query] = CacheEntry(response)
		return response
	}

	suspend fun transactions(query: String): List<TransactionSuggestion> {
		transactionCache[query]?.let { return it.value }
		Napier.i("Fetching transactions autocomplete for '$query'")
		val response: List<TransactionSuggestion> = client
			.get("${BuildKonfig.BASE_URL}/api/v1/autocomplete/transactions") {
				header(HttpHeaders.Authorization, "Bearer ${BuildKonfig.ACCESS_TOKEN}")
				accept(ContentType.Application.Json)
				parameter("query", query)
			}.body()
		transactionCache[query] = CacheEntry(response)
		return response
	}
}
