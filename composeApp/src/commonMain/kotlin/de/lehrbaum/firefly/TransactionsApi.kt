package de.lehrbaum.firefly

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class TransactionSplitRequest(
	val type: String,
	val date: String,
	val amount: String,
	val description: String,
	@SerialName("source_id") val sourceId: String,
	@SerialName("destination_id") val destinationId: String? = null,
	@SerialName("destination_name") val destinationName: String? = null,
	val tags: List<String>? = null,
)

@Serializable
private data class TransactionRequest(
	val transactions: List<TransactionSplitRequest>,
)

@OptIn(ExperimentalTime::class)
// date must be provided by caller
suspend fun createTransaction(
	client: HttpClient,
	source: Account,
	targetText: String,
	target: Account?,
	description: String,
	amount: String,
	tag: String?,
	date: Instant,
) {
	Napier.i("Creating transaction $description $amount from ${source.name} to ${target?.name ?: targetText}")
	val type = if (target != null) "transfer" else "withdrawal"
	val split = TransactionSplitRequest(
		type = type,
		date = date.toString(),
		amount = amount,
		description = description,
		sourceId = source.id,
		destinationId = target?.id,
		destinationName = if (target == null) targetText else null,
		tags = tag?.let(::listOf),
	)
	client.post("${BuildKonfig.BASE_URL}/api/v1/transactions") {
		header(HttpHeaders.Authorization, "Bearer ${BuildKonfig.ACCESS_TOKEN}")
		accept(ContentType.parse("application/vnd.api+json"))
		contentType(ContentType.Application.Json)
		setBody(TransactionRequest(listOf(split)))
	}
	Napier.d("Transaction created")
}
