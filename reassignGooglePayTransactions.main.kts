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
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import java.io.File
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

// Cannot be const, not allowed in scripts top level for some reason
private val TARGET_IBAN = "DE96120300009005290904"
private val PROVENANCE_NOTE = "Paid via Google Pay (IBAN DE96120300009005290904)."
private val DEFAULT_GOOGLE_PAY_ACCOUNT_ID = "35"

val json = Json {
	ignoreUnknownKeys = true
}

data class JournalMatch(
	val journalId: String,
	val splitId: String,
)

fun logInfo(message: String) = println("INFO: $message")

fun logWarn(message: String) = println("WARN: $message")

fun logError(message: String) = println("ERROR: $message")

fun usage(): Nothing {
	System.err.println("Usage: reassignGooglePayTransactions.main.kts <csv-file> [-f] [--source-account-id=<id>]")
	exitProcess(1)
}

if (args.isEmpty()) usage()

val csvPath = args.first()
var googlePayAccountId = DEFAULT_GOOGLE_PAY_ACCOUNT_ID
var force = false
for (rawArg in args.drop(1)) {
	when {
		rawArg == "-f" || rawArg == "--force" -> force = true
		rawArg.startsWith("--source-account-id=") -> googlePayAccountId = rawArg.substringAfter('=')
		rawArg == "--source-account-id" -> usage()
		else -> usage()
	}
}

val dryRun = !force
if (dryRun) {
	logInfo("Running in dry-run mode. No changes will be sent to Firefly III.")
} else {
	logWarn("Force flag supplied; updates will be applied to Firefly III.")
}

val baseUrl = System.getenv("BASE_URL")!!
val accessToken = System.getenv("ACCESS_TOKEN")!!

val csvFile = File(csvPath)
if (!csvFile.isFile) {
	logError("CSV file '$csvPath' does not exist or is not a file.")
	exitProcess(1)
}

fun loadCsv(file: File): Pair<Map<String, String>, Int> {
	val lines = file.readLines().filter { it.isNotBlank() }
	if (lines.isEmpty()) {
		logError("CSV file is empty after removing blank lines.")
		exitProcess(1)
	}
	val header = parseCsvLine(lines.first())
	val ibanIndex = header.indexOf("IBAN")
	val e2eIndex = header.indexOf("Kundenreferenz")
	val merchantIndex = header.indexOf("Zahlungsempfänger*in")
	if (ibanIndex == -1 || e2eIndex == -1 || merchantIndex == -1) {
		logError("CSV header must include 'IBAN', 'Kundenreferenz', and 'Zahlungsempfänger*in'.")
		exitProcess(1)
	}
	val mapping = mutableMapOf<String, String>()
	val conflicts = mutableSetOf<String>()
	var considered = 0
	for (line in lines.drop(1)) {
		val columns = parseCsvLine(line)
		if (columns.size <= maxOf(ibanIndex, e2eIndex, merchantIndex)) {
			continue
		}
		val iban = columns[ibanIndex]
		if (iban != TARGET_IBAN) {
			continue
		}
		considered += 1
		val e2e = columns[e2eIndex]
		val merchant = columns[merchantIndex]
		if (e2e.isBlank()) {
			logWarn("Skipping row with empty Kundenreferenz (E2E).")
			continue
		}
		val existing = mapping[e2e]
		when {
			existing == null && !conflicts.contains(e2e) -> mapping[e2e] = merchant
			existing != null && existing != merchant && conflicts.add(e2e) -> {
				mapping.remove(e2e)
				logError("Conflicting merchants for E2E '$e2e': '$existing' vs '$merchant'. Skipping this E2E.")
			}
		}
	}
	return mapping to considered
}

fun parseCsvLine(line: String): List<String> {
	val result = mutableListOf<String>()
	val builder = StringBuilder()
	var i = 0
	var inQuotes = false
	while (i < line.length) {
		val c = line[i]
		if (inQuotes) {
			when (c) {
				'"' if i + 1 < line.length && line[i + 1] == '"' -> {
					builder.append('"')
					i += 2
				}
				'"' -> {
					inQuotes = false
					i += 1
				}
				else -> {
					builder.append(c)
					i += 1
				}
			}
		} else {
			when (c) {
				';' -> {
					result += builder.toString()
					builder.setLength(0)
					i += 1
				}

				'"' -> {
					inQuotes = true
					i += 1
				}

				'\r', '\n' -> {
					i += 1
				}

				else -> {
					builder.append(c)
					i += 1
				}
			}
		}
	}
	result += builder.toString()
	return result
}

fun appendProvenance(existing: String?): String =
	if (existing.isNullOrBlank()) {
		PROVENANCE_NOTE
	} else {
		"$existing\n\n$PROVENANCE_NOTE"
	}
val (e2eToMerchant, csvRowsConsidered) = loadCsv(csvFile)
logInfo("CSV rows considered after IBAN filter: $csvRowsConsidered")
if (e2eToMerchant.isEmpty()) {
	logWarn("No CSV rows matched the target IBAN. Nothing to process.")
	exitProcess(0)
}

val client = HttpClient(CIO)
val matchedByE2e = mutableMapOf<String, MutableList<JournalMatch>>()
val merchantAccountIds = mutableMapOf<String, String>()
var e2eNotFoundCount = 0
var merchantAccountsCreated = 0
var merchantAccountsReused = 0
var journalsUpdated = 0
var journalsFailed = 0
var journalsPlanned = 0

runBlocking {
	matchJournalEntries()
	val unmatched = e2eToMerchant.keys.filterNot(matchedByE2e::containsKey)
	for (e2e in unmatched) {
		logWarn("No matching Firefly III journal found for E2E '$e2e'.")
		e2eNotFoundCount += 1
	}
	val merchantsNeedingAccounts = matchedByE2e.keys.map { e2eToMerchant[it]!! }.toSet()
	for (merchant in merchantsNeedingAccounts) {
		val lookupResponse = client.get("$baseUrl/api/v1/autocomplete/accounts") {
			header("Authorization", "Bearer $accessToken")
			parameter("query", merchant)
			parameter("types", "expense")
		}
		if (lookupResponse.status.isSuccess()) {
			val payload = json.parseToJsonElement(lookupResponse.bodyAsText())
			val existing = payload.jsonArray
				.firstOrNull { it.jsonObject["name"]?.jsonPrimitive?.content == merchant }
				?.jsonObject
				?.get("id")
				?.jsonPrimitive
				?.content
			if (existing != null) {
				merchantAccountIds[merchant] = existing
				merchantAccountsReused += 1
				logWarn("Merchant account already exists for '$merchant' (id=$existing); reusing.")
				continue
			}
		}
		if (dryRun) {
			logInfo("Dry run: would create expense account '$merchant'.")
			merchantAccountIds[merchant] = "(dry-run-new-account)"
			continue
		} else {
			logInfo("Creating expense account '$merchant'.")
		}
		val createBody = buildJsonObject {
			put("name", merchant)
			put("type", "expense")
		}
		val createResponse = client.post("$baseUrl/api/v1/accounts") {
			header("Authorization", "Bearer $accessToken")
			setBody(
				TextContent(
					json.encodeToString(JsonObject.serializer(), createBody),
					ContentType.Application.Json,
				),
			)
		}
		val createText = createResponse.bodyAsText()
		if (!createResponse.status.isSuccess()) {
			logError("Failed to create expense account for '$merchant': HTTP ${createResponse.status.value} ${createResponse.status.description}")
			if (createText.isNotBlank()) {
				logError(createText)
			}
			continue
		}
		val created = json.parseToJsonElement(createText).jsonObject
		val id = created["data"]
			?.jsonObject
			?.get("id")
			?.jsonPrimitive
			?.content
		if (id == null) {
			logError("Account creation response missing ID for '$merchant'.")
			continue
		}
		merchantAccountIds[merchant] = id
		merchantAccountsCreated += 1
		logInfo("Created expense account '$merchant' (id=$id).")
	}
	for ((e2e, matches) in matchedByE2e) {
		val merchant = e2eToMerchant[e2e] ?: continue
		val destinationAccountId = merchantAccountIds[merchant] ?: run {
			logError("No expense account ID available for merchant '$merchant'. Skipping journals for E2E '$e2e'.")
			continue
		}
		for (match in matches) {
			logInfo("Getting ${match.journalId} for $e2e")
			val detailResponse = client.get("$baseUrl/api/v1/transactions/${match.journalId}") {
				header("Authorization", "Bearer $accessToken")
			}
			val detailText = detailResponse.bodyAsText()
			if (!detailResponse.status.isSuccess()) {
				logError("Failed to fetch journal ${match.journalId}: HTTP ${detailResponse.status.value} ${detailResponse.status.description}")
				if (detailText.isNotBlank()) {
					logError(detailText)
				}
				journalsFailed += 1
				continue
			}
			val detail = json.parseToJsonElement(detailText).jsonObject
			val split = detail["data"]
				?.jsonObject
				?.get("attributes")
				?.jsonObject
				?.get("transactions")
				?.jsonArray
				?.singleOrNull()
				?.jsonObject
			if (split == null) {
				logError("Journal ${match.journalId} does not contain a transaction split.")
				journalsFailed += 1
				continue
			}
			if (split["destination_id"]?.jsonPrimitive?.content == destinationAccountId) {
				logInfo("Journal ${match.journalId} already uses correct destination account $destinationAccountId.")
				continue
			}
			val updatedNotes = appendProvenance(split["notes"]?.jsonPrimitive?.contentOrNull)
			val updateSplit = buildJsonObject {
				put("destination_id", destinationAccountId)
				put("description", merchant)
				put("notes", updatedNotes)
			}
			if (dryRun) {
				journalsPlanned += 1
				logInfo("Dry run: would update journal ${match.journalId} for merchant '$merchant'.")
				continue
			} else {
				logInfo("Updating journal ${match.journalId} for merchant '$merchant'.")
			}
			val requestBody = buildJsonObject {
				put("apply_rules", false)
				put("fire_webhooks", false)
				put("transactions", JsonArray(listOf(updateSplit)))
			}
			val updateResponse = client.put("$baseUrl/api/v1/transactions/${match.journalId}") {
				header("Authorization", "Bearer $accessToken")
				setBody(
					TextContent(
						json.encodeToString(JsonObject.serializer(), requestBody),
						ContentType.Application.Json,
					),
				)
			}
			val updateText = updateResponse.bodyAsText()
			if (updateResponse.status.isSuccess()) {
				journalsUpdated += 1
				logInfo("Updated journal ${match.journalId} for merchant '$merchant'.")
			} else {
				journalsFailed += 1
				logError("Failed to update journal ${match.journalId}: HTTP ${updateResponse.status.value} ${updateResponse.status.description}")
				if (updateText.isNotBlank()) {
					logError(updateText)
				}
			}
		}
	}
}

client.close()
logInfo(
	"Summary: CSV rows considered=$csvRowsConsidered, " +
		"E2E not found=$e2eNotFoundCount, " +
		"merchant accounts created=$merchantAccountsCreated, " +
		"merchant accounts reused=$merchantAccountsReused, " +
		"journals updated=$journalsUpdated, " +
		"journals planned (dry-run)=$journalsPlanned, journals failed=$journalsFailed.",
)
if (dryRun) {
	logInfo("Dry run complete. Re-run with -f to apply these changes.")
}

private suspend fun matchJournalEntries() {
	var page = 1
	var totalPages: Int? = null
	while (totalPages == null || page <= totalPages) {
		val response = client.get("$baseUrl/api/v1/accounts/$DEFAULT_GOOGLE_PAY_ACCOUNT_ID/transactions") {
			header("Authorization", "Bearer $accessToken")
			parameter("page", page)
		}
		val bodyText = response.bodyAsText()
		if (!response.status.isSuccess()) {
			logError("Failed to list transactions (page $page): HTTP ${response.status.value} ${response.status.description}")
			if (bodyText.isNotBlank()) {
				logError(bodyText)
			}
			break
		}
		val root = json.parseToJsonElement(bodyText).jsonObject
		val data = root["data"]?.jsonArray ?: JsonArray(emptyList())
		for (entry in data) {
			val journal = entry.jsonObject
			val journalId = journal["id"]?.jsonPrimitive?.content ?: continue
			val split = journal["attributes"]
				?.jsonObject
				?.get("transactions")
				?.jsonArray
				?.singleOrNull()
				?.jsonObject ?: continue
			if (split["destination_id"]?.jsonPrimitive?.content != googlePayAccountId) {
				continue
			}
			val e2e = split["sepa_ct_id"]?.jsonPrimitive?.content ?: continue
			if (!e2eToMerchant.containsKey(e2e)) {
				continue
			}
			val splitId = split["transaction_journal_id"]?.jsonPrimitive?.content ?: journalId
			matchedByE2e.getOrPut(e2e) { mutableListOf() }.add(JournalMatch(journalId, splitId))
		}
		val pagination = root["meta"]
			?.jsonObject
			?.get("pagination")
			?.jsonObject
		totalPages = pagination?.get("total_pages")?.jsonPrimitive?.int
		logInfo("Processed page '$page' of total $totalPages")
		page += 1
	}
}
