package de.lehrbaum.firefly

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
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
	var sourceText by mutableStateOf("")
	var targetText by mutableStateOf("")
	var description by mutableStateOf("")
	var amount by mutableStateOf("")
	var expandedSource by mutableStateOf(false)
	var expandedTarget by mutableStateOf(false)
	var expandedDescription by mutableStateOf(false)
	var selectedSource by mutableStateOf<Account?>(null)
	var selectedTarget by mutableStateOf<Account?>(null)
	var sourceSuggestions by mutableStateOf<List<Account>>(emptyList())
		private set
	var targetSuggestions by mutableStateOf<List<Account>>(emptyList())
		private set
	var descriptionSuggestions by mutableStateOf<List<String>>(emptyList())
		private set
	var errorMessage by mutableStateOf<String?>(null)

	private val sourceQuery = MutableStateFlow("")
	private val targetQuery = MutableStateFlow("")
	private val descriptionQuery = MutableStateFlow("")

	init {
		scope.launch {
			sourceQuery.debounce(300.milliseconds).collectLatest { query ->
				sourceSuggestions = autocompleteApi.accounts(query)
				selectedSource = sourceSuggestions.firstOrNull { it.name == sourceText }
			}
		}
		scope.launch {
			targetQuery.debounce(300.milliseconds).collectLatest { query ->
				targetSuggestions = autocompleteApi.accounts(query)
				selectedTarget = targetSuggestions.firstOrNull { it.name == targetText }
			}
		}
		scope.launch {
			descriptionQuery.debounce(300.milliseconds).collectLatest { query ->
				descriptionSuggestions =
					autocompleteApi.transactions(query).map { it.description }
			}
		}
	}

	@OptIn(ExperimentalTime::class)
	var dateTime by mutableStateOf(
		Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
	)
		private set

	fun onSourceTextChange(text: String) {
		sourceText = text
		expandedSource = true
		scope.launch { sourceQuery.emit(text) }
	}

	fun onTargetTextChange(text: String) {
		targetText = text
		expandedTarget = true
		scope.launch { targetQuery.emit(text) }
	}

	fun onDescriptionTextChange(text: String) {
		description = text
		expandedDescription = true
		scope.launch { descriptionQuery.emit(text) }
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
		expandedSource = false
		expandedTarget = false
		expandedDescription = false
		sourceSuggestions = emptyList()
		targetSuggestions = emptyList()
		descriptionSuggestions = emptyList()
		scope.launch {
			sourceQuery.emit("")
			targetQuery.emit("")
			descriptionQuery.emit("")
		}
		dateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
	}
}
