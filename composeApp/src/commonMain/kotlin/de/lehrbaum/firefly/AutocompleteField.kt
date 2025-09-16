package de.lehrbaum.firefly

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(ExperimentalTime::class, FlowPreview::class)
class AutocompleteField<T>(
	private val scope: CoroutineScope,
	private val fetcher: suspend (String) -> List<T>,
	private val textOf: (T) -> String = { it.toString() },
	debounce: Duration = 300.milliseconds,
) {
	var text by mutableStateOf("")
	var expanded by mutableStateOf(false)
	var selected by mutableStateOf<T?>(null)
	var suggestions by mutableStateOf<List<T>>(emptyList())
		private set

	val selectedText: String
		get() = selected?.let(textOf) ?: text

	private val query = MutableStateFlow("")
	private var fullSuggestions: List<T> = emptyList()

	init {
		scope.launch {
			query.debounce(debounce).collectLatest { q ->
				fullSuggestions = fetcher(q)
				filter()
			}
		}
	}

	fun onTextChange(newText: String) {
		text = newText
		expanded = true
		filter()
		query.value = newText
	}

	private fun filter() {
		suggestions = fullSuggestions.filter { textOf(it).contains(text, ignoreCase = true) }
		selected = suggestions.firstOrNull { textOf(it) == text }
	}

	fun select(item: T) {
		text = textOf(item)
		selected = item
		expanded = false
		filter()
	}

	fun clear() {
		text = ""
		expanded = false
		selected = null
		suggestions = emptyList()
		fullSuggestions = emptyList()
		query.value = ""
	}

	fun itemText(item: T): String = textOf(item)
}
