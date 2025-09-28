#!/usr/bin/env kotlin
@file:DependsOn("io.ktor:ktor-client-core-jvm:3.3.0")
@file:DependsOn("io.ktor:ktor-client-cio-jvm:3.3.0")
@file:DependsOn("io.ktor:ktor-client-content-negotiation-jvm:3.3.0")
@file:DependsOn("io.ktor:ktor-serialization-kotlinx-json-jvm:3.3.0")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
@file:Suppress("PropertyName")

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
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Cannot be const, not allowed in scripts top level for some reason
private val TARGET_IBAN = "DE96120300009005290904"
private val PROVENANCE_NOTE = "Paid via Google Pay (IBAN DE96120300009005290904)."
private val DEFAULT_GOOGLE_PAY_ACCOUNT_ID = "35"

val json = Json {
	ignoreUnknownKeys = true
}

@Serializable
data class AutocompleteAccount(
	val id: String,
	val name: String,
)

@Serializable
data class AccountData(
	val id: String,
)

@Serializable
data class ApiSingleResponse<T>(
	val data: T? = null,
)

@Serializable
data class ApiListResponse<T>(
	val data: List<T> = emptyList(),
	val meta: ResponseMeta? = null,
)

@Serializable
data class ResponseMeta(
	val pagination: Pagination? = null,
)

@Serializable
data class Pagination(
	val total_pages: Int? = null,
)

@Serializable
data class TransactionJournal(
	val id: String,
	val attributes: TransactionJournalAttributes? = null,
)

@Serializable
data class TransactionJournalAttributes(
	val transactions: List<TransactionSplit> = emptyList(),
)

@Serializable
data class TransactionSplit(
	val destination_id: String? = null,
	val sepa_ct_id: String? = null,
	val transaction_journal_id: String? = null,
	val description: String? = null,
	val notes: String? = null,
)

@Serializable
data class CreateAccountRequest(
	val name: String,
	val type: String,
)

@Serializable
data class UpdateTransactionRequest(
	val apply_rules: Boolean,
	val fire_webhooks: Boolean,
	val transactions: List<UpdateTransactionSplit>,
)

@Serializable
data class UpdateTransactionSplit(
	val destination_id: String,
	val description: String,
	val notes: String,
)

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
			val payload = json.decodeFromString<List<AutocompleteAccount>>(lookupResponse.bodyAsText())
			val existing = payload.firstOrNull { it.name == merchant }?.id
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
		val createBody = CreateAccountRequest(
			name = merchant,
			type = "expense",
		)
		val createResponse = client.post("$baseUrl/api/v1/accounts") {
			header("Authorization", "Bearer $accessToken")
			setBody(
				TextContent(
					json.encodeToString(createBody),
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
		val created = json.decodeFromString<ApiSingleResponse<AccountData>>(createText)
		val id = created.data?.id
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
			val detail = json.decodeFromString<ApiSingleResponse<TransactionJournal>>(detailText)
			val split = detail.data
				?.attributes
				?.transactions
				?.singleOrNull()
			if (split == null) {
				logError("Journal ${match.journalId} does not contain a transaction split.")
				journalsFailed += 1
				continue
			}
			val updatedNotes = appendProvenance(split.notes)
			val updateSplit = UpdateTransactionSplit(
				destination_id = destinationAccountId,
				description = merchant,
				notes = updatedNotes,
			)
			if (dryRun) {
				journalsPlanned += 1
				logInfo("Dry run: would update journal ${match.journalId} for merchant '$merchant'.")
				continue
			} else {
				logInfo("Updating journal ${match.journalId} for merchant '$merchant'.")
			}
			val requestBody = UpdateTransactionRequest(
				apply_rules = false,
				fire_webhooks = false,
				transactions = listOf(updateSplit),
			)
			val updateResponse = client.put("$baseUrl/api/v1/transactions/${match.journalId}") {
				header("Authorization", "Bearer $accessToken")
				setBody(
					TextContent(
						json.encodeToString(requestBody),
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
		val root = json.decodeFromString<ApiListResponse<TransactionJournal>>(bodyText)
		for (journal in root.data) {
			val split = journal.attributes
				?.transactions
				?.singleOrNull() ?: continue
			if (split.destination_id != googlePayAccountId) {
				continue
			}
			val e2e = split.sepa_ct_id ?: continue
			if (!e2eToMerchant.containsKey(e2e)) {
				continue
			}
			val splitId = split.transaction_journal_id ?: journal.id
			matchedByE2e.getOrPut(e2e) { mutableListOf() }.add(JournalMatch(journal.id, splitId))
		}
		totalPages = root.meta?.pagination?.total_pages
		logInfo("Processed page '$page' of total $totalPages")
		page += 1
	}
}
