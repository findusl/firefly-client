package de.lehrbaum.firefly

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.IOException

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
			val parsedAmount = parseAmount(amount).getOrElse {
				errorMessage = "Invalid amount"
				return
			}
			clearError()
			val normalizedAmount = parsedAmount
			isSaving = true
			val savedSourceText = sourceField.selectedText
			runNetworkCall {
				createTransaction(
					client,
					src,
					targetField.selectedText,
					targetField.selected,
					descriptionField.selectedText,
					normalizedAmount,
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
			when (throwable) {
				is CancellationException -> throw throwable
				is ResponseException -> {
					Napier.e("Network call failed with response", throwable)
					val response = throwable.response
					val status = response.status
					val url = response.call.request.url
					val body = runCatching { response.bodyAsText() }.getOrNull()?.takeIf { it.isNotBlank() }
					val statusInfo = "${status.value} ${status.description}"
					val bodyInfo = body?.let {
						if (it.length > 500) it.take(497) + "..." else it
					}
					errorMessage = buildString {
						append("Request to ")
						append(url)
						append(" failed (")
						append(statusInfo)
						append(')')
						if (bodyInfo != null) {
							append(": ")
							append(bodyInfo)
						}
					}
				}
				is IOException -> {
					Napier.e("Network call failed due to I/O error", throwable)
					errorMessage = "Connection failed"
				}
				else -> {
					Napier.e("Network call failed", throwable)
					errorMessage = throwable.message ?: "Unexpected error"
				}
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
