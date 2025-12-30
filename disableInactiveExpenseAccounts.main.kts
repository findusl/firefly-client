#!/usr/bin/env kotlin
@file:DependsOn("io.ktor:ktor-client-core-jvm:3.3.0")
@file:DependsOn("io.ktor:ktor-client-cio-jvm:3.3.0")
@file:DependsOn("io.ktor:ktor-client-content-negotiation-jvm:3.3.0")
@file:DependsOn("io.ktor:ktor-serialization-kotlinx-json-jvm:3.3.0")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
@file:Suppress("ktlint:standard:property-naming")

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

fun usage(): Nothing {
	System.err.println("Usage: disableInactiveExpenseAccounts.main.kts [-f|--force]")
	exitProcess(1)
}

val force = args.any { it == "-f" || it == "--force" }
if (args.isNotEmpty() && !force) usage()

val dryRun = !force
if (dryRun) {
	logInfo("Running in dry-run mode. No changes will be sent to Firefly III.")
} else {
	logWarn("Force flag supplied; updates will be applied to Firefly III.")
}

val baseUrl = System.getenv("BASE_URL") ?: run {
	logError("BASE_URL environment variable is not set.")
	exitProcess(1)
}
val accessToken = System.getenv("ACCESS_TOKEN") ?: run {
	logError("ACCESS_TOKEN environment variable is not set.")
	exitProcess(1)
}

val json = Json { ignoreUnknownKeys = true }

val client = HttpClient(CIO) {
	engine { requestTimeout = 600_000 }
}

var accountsVisited = 0
var accountsWithoutTransactions = 0
var updatesPlanned = 0
var updatesApplied = 0
var updatesFailed = 0

suspend fun accountsPage(page: Int): Pair<JsonArray, Int>? {
	val response = client.get("$baseUrl/api/v1/accounts") {
		header("Authorization", "Bearer $accessToken")
		parameter("page", page)
		parameter("type", "expense")
	}
	val bodyText = response.bodyAsText()
	if (!response.status.isSuccess()) {
		logError("Failed to list accounts: HTTP ${response.status.value} ${response.status.description}")
		if (bodyText.isNotBlank()) {
			logError(bodyText)
		}
		return null
	}

	val root = json.parseToJsonElement(bodyText).jsonObject
	val data = root["data"]?.jsonArray ?: JsonArray(emptyList())
	val pagination = root["meta"]?.jsonObject?.get("pagination")?.jsonObject
	val totalPages = pagination?.get("total_pages")?.jsonPrimitive?.intOrNull
	if (totalPages == null) {
		logError("Failed to read total_pages from accounts pagination metadata on page $page.")
		return null
	}

	return data to totalPages
}

runBlocking {
	var page = 1
	var totalPages: Int? = null
	while (totalPages == null || page <= totalPages) {
		val (data, pageCount) = accountsPage(page) ?: break
		totalPages = pageCount
		logInfo("Processing page $page of $totalPages")
		for (entry in data) {
			val account = entry.jsonObject
			val id = account["id"]?.jsonPrimitive?.contentOrNull ?: continue
			val attributes = account["attributes"]?.jsonObject ?: continue
			if (!isExpenseAccount(attributes)) continue
			accountsVisited += 1
			val transactionCount = accountTransactionCount(id)
			if (transactionCount > 0) continue
			accountsWithoutTransactions += 1
			disableAccount(id, attributes)
		}
		if (page >= totalPages) break
		page += 1
	}
}

client.close()

logInfo(
	"Summary: accounts inspected=$accountsVisited, no-transaction expense accounts=$accountsWithoutTransactions, " +
		"updates planned=$updatesPlanned, updates applied=$updatesApplied, updates failed=$updatesFailed.",
)
if (dryRun) {
	logInfo("Dry run complete. Re-run with -f to apply these changes.")
}

suspend fun accountTransactionCount(id: String): Int {
	val response = client.get("$baseUrl/api/v1/accounts/$id/transactions") {
		header("Authorization", "Bearer $accessToken")
		parameter("page", 1)
		parameter("limit", 1)
	}
	val bodyText = response.bodyAsText()
	if (!response.status.isSuccess()) {
		logError(
			"Failed to list transactions for account $id: HTTP ${response.status.value} ${response.status.description}",
		)
		if (bodyText.isNotBlank()) {
			logError(bodyText)
		}
		return Int.MAX_VALUE
	}

	val root = json.parseToJsonElement(bodyText).jsonObject
	val pagination = root["meta"]?.jsonObject?.get("pagination")?.jsonObject
	return pagination?.get("total")?.jsonPrimitive?.intOrNull
		?: pagination?.get("total_entries")?.jsonPrimitive?.intOrNull
		?: 0
}

suspend fun disableAccount(id: String, attributes: JsonObject) {
	val name = attributes["name"]?.jsonPrimitive?.content
	val active = attributes["active"]?.jsonPrimitive?.booleanOrNull ?: true

	if (!active) {
		logInfo(
			"Expense account '$name' (id=$id) already inactive and has no transactions; skipping update.",
		)
		return
	}

	val updateRequest = buildJsonObject {
		attributes.forEach { (key, value) ->
			put(key, value)
		}
		put("active", false)
	}

	if (dryRun) {
		updatesPlanned += 1
		logInfo("Dry run: would disable expense account '$name' (id=$id).")
		return
	}

	logInfo("Disabling expense account '$name' (id=$id).")
	val response = client.put("$baseUrl/api/v1/accounts/$id") {
		header("Authorization", "Bearer $accessToken")
		setBody(
			TextContent(
				json.encodeToString(JsonObject.serializer(), updateRequest),
				ContentType.Application.Json,
			),
		)
	}
	val responseText = response.bodyAsText()
	if (response.status.isSuccess()) {
		updatesApplied += 1
		logInfo("Disabled expense account '$name' (id=$id).")
	} else {
		updatesFailed += 1
		logError("Failed to update account $id: HTTP ${response.status.value} ${response.status.description}")
		if (responseText.isNotBlank()) {
			logError(responseText)
		}
	}
}

fun isExpenseAccount(attributes: JsonObject): Boolean {
	val type = attributes["type"]?.jsonPrimitive?.contentOrNull ?: return false
	return type.equals("expense", ignoreCase = true) || type.equals("Expense account", ignoreCase = true)
}

fun logInfo(message: String) = println("INFO: $message")

fun logWarn(message: String) = println("WARN: $message")

fun logError(message: String) = println("ERROR: $message")
