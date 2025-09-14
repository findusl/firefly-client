package de.lehrbaum.firefly

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class MainViewModel(private val client: HttpClient) {
	var accounts by mutableStateOf<List<Account>>(emptyList())
		private set
	var sourceText by mutableStateOf("")
	var targetText by mutableStateOf("")
	var description by mutableStateOf("")
	var amount by mutableStateOf("")
	var expandedSource by mutableStateOf(false)
	var expandedTarget by mutableStateOf(false)
	var selectedSource by mutableStateOf<Account?>(null)
	var selectedTarget by mutableStateOf<Account?>(null)
	var errorMessage by mutableStateOf<String?>(null)

	@OptIn(ExperimentalTime::class)
	var dateTime by mutableStateOf(
		Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
	)
		private set

	suspend fun loadAccounts() {
		runNetworkCall {
			accounts = fetchAccounts(client)
		}
	}

	fun onSourceTextChange(text: String) {
		sourceText = text
		expandedSource = true
		selectedSource = accounts.firstOrNull { it.name == text }
	}

	fun onTargetTextChange(text: String) {
		targetText = text
		expandedTarget = true
		selectedTarget = accounts.firstOrNull { it.name == text }
	}

	fun onDateTimeChange(newDateTime: LocalDateTime) {
		dateTime = newDateTime
	}

	@OptIn(ExperimentalTime::class)
	suspend fun save() {
		val src = selectedSource
		if (src != null && amount.isNotBlank() && description.isNotBlank()) {
			runNetworkCall {
				createTransaction(
					client,
					src,
					targetText,
					selectedTarget,
					description,
					amount,
					dateTime.toInstant(TimeZone.currentSystemDefault()),
				)
			}
		}
	}

	private inline fun <T> runNetworkCall(block: () -> T): Result<T> =
		runCatching(block)
			.onFailure {
				if (it is CancellationException) {
					throw it
				} else {
					Napier.e("Network call failed", it)
					errorMessage = "Failed to reach server"
				}
			}.onSuccess {
				Napier.d("Network call succeeded")
				errorMessage = null
			}

	fun clearError() {
		errorMessage = null
	}

	@OptIn(ExperimentalTime::class)
	fun clear() {
		sourceText = ""
		targetText = ""
		description = ""
		amount = ""
		selectedSource = null
		selectedTarget = null
		dateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
	}
}
