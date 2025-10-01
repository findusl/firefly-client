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

data class BannerState(val message: String, val type: BannerType)

enum class BannerType {
	Success,
	Error,
}

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
	var bannerState by mutableStateOf<BannerState?>(null)
	var isSaving by mutableStateOf(false)
		private set

	var sourceFieldError by mutableStateOf<String?>(null)
		private set
	var descriptionFieldError by mutableStateOf<String?>(null)
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

	fun onSourceTextChange(newText: String) {
		sourceFieldError = null
		sourceField.onTextChange(newText)
	}

	fun onSourceSuggestionSelected(account: Account) {
		sourceFieldError = null
		sourceField.select(account)
	}

	fun onDescriptionTextChange(newText: String) {
		descriptionFieldError = null
		descriptionField.onTextChange(newText)
	}

	fun onDescriptionSuggestionSelected(description: String) {
		descriptionFieldError = null
		descriptionField.select(description)
	}

	@OptIn(ExperimentalTime::class)
	suspend fun save() {
		if (isSaving) return

		dismissBanner()
		sourceFieldError = null
		descriptionFieldError = null

		val src = sourceField.selected
		val description = descriptionField.selectedText
		var hasError = false
		if (src == null) {
			sourceFieldError = if (sourceField.selectedText.isBlank()) {
				"Source account is required"
			} else {
				"Select a valid source account"
			}
			hasError = true
		}
		if (description.isBlank()) {
			descriptionFieldError = "Description is required"
			hasError = true
		}
		if (amount.isBlank()) {
			hasError = true
		}
		if (hasError) {
			showError("Please fill in all required fields")
			return
		}
		if (src != null && amount.isNotBlank() && description.isNotBlank()) {
			val parsedAmount = parseAmount(amount).getOrElse {
				showError("Invalid amount")
				return
			}
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
				showSuccess("Transaction saved successfully")
			}.also {
				isSaving = false
			}
		}
	}

	private suspend fun <T> runNetworkCall(block: suspend () -> T): Result<T> =
		try {
			Result.success(block()).also {
				Napier.d("Network call succeeded")
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
					showError(
						buildString {
							append("Request to ")
							append(url)
							append(" failed (")
							append(statusInfo)
							append(')')
							if (bodyInfo != null) {
								append(": ")
								append(bodyInfo)
							}
						},
					)
				}
				is IOException -> {
					Napier.e("Network call failed due to I/O error", throwable)
					showError("Connection failed")
				}
				else -> {
					Napier.e("Network call failed", throwable)
					showError(throwable.message ?: "Unexpected error")
				}
			}
			Result.failure(throwable)
		}

	private fun showError(message: String) {
		bannerState = BannerState(message, BannerType.Error)
	}

	private fun showSuccess(message: String) {
		bannerState = BannerState(message, BannerType.Success)
	}

	fun dismissBanner() {
		bannerState = null
	}

	@OptIn(ExperimentalTime::class)
	fun clear(keepSource: Boolean = false) {
		sourceFieldError = null
		descriptionFieldError = null
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
