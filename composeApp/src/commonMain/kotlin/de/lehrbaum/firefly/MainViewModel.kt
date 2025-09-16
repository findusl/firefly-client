package de.lehrbaum.firefly

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalTime::class)
class MainViewModel(
	private val client: HttpClient,
	private val autocompleteApi: AutocompleteApi = AutocompleteApi(client),
	private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) {
	val sourceField = AutocompleteField(
		scope,
		{ query -> autocompleteApi.accounts(query, types = listOf("Asset account")) },
		Account::name,
	)
	val targetField = AutocompleteField(scope, autocompleteApi::accounts, Account::name)
	val descriptionField = AutocompleteField(scope, { q -> autocompleteApi.transactions(q).map { it.description } }, { it })

	var amount by mutableStateOf("")
	var errorMessage by mutableStateOf<String?>(null)

	@OptIn(ExperimentalTime::class)
	var dateTime by mutableStateOf(
		Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
	)
		private set

	fun onDateTimeChange(newDateTime: LocalDateTime) {
		dateTime = newDateTime
	}

	@OptIn(ExperimentalTime::class)
	suspend fun save() {
		val src = sourceField.selected
		if (src != null && amount.isNotBlank() && descriptionField.selectedText.isNotBlank()) {
			runNetworkCall {
				createTransaction(
					client,
					src,
					targetField.selectedText,
					targetField.selected,
					descriptionField.selectedText,
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
				clearError()
				clear()
			}

	fun clearError() {
		errorMessage = null
	}

	@OptIn(ExperimentalTime::class)
	fun clear() {
		sourceField.clear()
		targetField.clear()
		descriptionField.clear()
		amount = ""
		dateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
	}
}
