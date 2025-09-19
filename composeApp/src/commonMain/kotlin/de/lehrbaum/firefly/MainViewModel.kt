package de.lehrbaum.firefly

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings
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
	companion object {
		private const val LAST_SOURCE_ACCOUNT_KEY = "last_source_account"
	}

	private val settings: Settings = Settings()

	val sourceField = AutocompleteField(
		scope,
		{ query -> autocompleteApi.accounts(query, types = listOf("Asset account")) },
		Account::name,
	)
	val targetField = AutocompleteField(scope, autocompleteApi::accounts, Account::name)
	val descriptionField = AutocompleteField(scope, { q -> autocompleteApi.transactions(q).map { it.description } }, { it })
	val tagField = AutocompleteField(scope, autocompleteApi::tags, TagSuggestion::name)

	var amount by mutableStateOf("")
	var errorMessage by mutableStateOf<String?>(null)
	var isSaving by mutableStateOf(false)
		private set

	@OptIn(ExperimentalTime::class)
	var dateTime by mutableStateOf(
		Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
	)
		private set

	init {
		prefillLastSource()
	}

	fun onDateTimeChange(newDateTime: LocalDateTime) {
		dateTime = newDateTime
	}

	@OptIn(ExperimentalTime::class)
	suspend fun save() {
		if (isSaving) return

		val src = sourceField.selected
		if (src != null && amount.isNotBlank() && descriptionField.selectedText.isNotBlank()) {
			isSaving = true
			val savedSourceText = sourceField.selectedText
			runNetworkCall {
				createTransaction(
					client,
					src,
					targetField.selectedText,
					targetField.selected,
					descriptionField.selectedText,
					amount,
					dateTime.toInstant(TimeZone.currentSystemDefault()),
					tagField.selectedText.takeIf { it.isNotBlank() },
				)
			}.onSuccess {
				settings.putString(LAST_SOURCE_ACCOUNT_KEY, savedSourceText)
				clear(keepSource = true)
			}.also {
				isSaving = false
			}
		}
	}

	private suspend fun <T> runNetworkCall(block: suspend () -> T): Result<T> =
		try {
			Result.success(block()).also {
				Napier.d("Network call succeeded")
				clearError()
			}
		} catch (throwable: Throwable) {
			if (throwable is CancellationException) {
				throw throwable
			} else {
				Napier.e("Network call failed", throwable)
				errorMessage = "Failed to reach server"
			}
			Result.failure(throwable)
		}

	fun clearError() {
		errorMessage = null
	}

	@OptIn(ExperimentalTime::class)
	fun clear(keepSource: Boolean = false) {
		if (!keepSource) {
			sourceField.clear()
		}
		targetField.clear()
		descriptionField.clear()
		tagField.clear()
		amount = ""
		dateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
	}

	private fun prefillLastSource() {
		val savedSourceAccount = settings.getStringOrNull(LAST_SOURCE_ACCOUNT_KEY)
		if (!savedSourceAccount.isNullOrBlank()) {
			sourceField.prefill(savedSourceAccount)
		}
	}
}
