package de.lehrbaum.firefly

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.client.HttpClient

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

	suspend fun loadAccounts() {
		accounts = fetchAccounts(client)
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

	suspend fun save() {
		val src = selectedSource
		if (src != null && amount.isNotBlank() && description.isNotBlank()) {
			createTransaction(client, src, targetText, selectedTarget, description, amount)
		}
	}

	fun clear() {
		sourceText = ""
		targetText = ""
		description = ""
		amount = ""
		selectedSource = null
		selectedTarget = null
	}
}
