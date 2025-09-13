package de.lehrbaum.firefly

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

@Serializable
data class AccountAttributes(
	val name: String,
	val type: String,
)

@Serializable
data class AccountData(
	val id: String,
	val attributes: AccountAttributes,
)

@Serializable
data class AccountsResponse(
	val data: List<AccountData>,
)

@Serializable
data class Account(
	val id: String,
	val name: String,
	val type: String,
)

suspend fun fetchAccounts(client: HttpClient): List<Account> {
	val response: AccountsResponse = client
		.get("${BuildKonfig.BASE_URL}/api/v1/accounts") {
			header(HttpHeaders.Authorization, "Bearer ${BuildKonfig.ACCESS_TOKEN}")
			accept(ContentType.Application.Json)
		}.body()
	return response.data
		.filterNot { it.attributes.type == "initial-balance" }
		.map { Account(it.id, it.attributes.name, it.attributes.type) }
}
