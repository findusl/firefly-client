package de.lehrbaum.firefly

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
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
	/**
	 * Extracts the display text for an item returned by [fetcher]. Allows callers to supply the
	 * human-readable label without forcing [T] to implement a particular interface.
	 */
	private val textOf: (T) -> String = { it.toString() },
	debounce: Duration = 300.milliseconds,
) {
	var text by mutableStateOf("")
	var expanded by mutableStateOf(false)
	var selected by mutableStateOf<T?>(null)
	var suggestions by mutableStateOf<PersistentList<T>>(persistentListOf())
		private set
	var isLoading by mutableStateOf(false)
		private set

	val selectedText: String
		get() = selected?.let(textOf) ?: text

	private val query = MutableStateFlow("")

	init {
		scope.launch {
			query.debounce(debounce).collectLatest { q ->
				isLoading = true
				try {
					suggestions = fetcher(q).toPersistentList()
					selected = suggestions.firstOrNull { textOf(it) == text }
				} finally {
					isLoading = false
				}
			}
		}
	}

	fun onTextChange(newText: String) {
		text = newText
		expanded = true
		selected = suggestions.firstOrNull { textOf(it) == newText }
		query.value = newText
	}

	fun select(item: T) {
		text = textOf(item)
		selected = item
		expanded = false
	}

	fun clear() {
		text = ""
		expanded = false
		selected = null
		suggestions = persistentListOf()
		isLoading = false
		query.value = ""
	}

	fun prefill(prefillText: String) {
		text = prefillText
		expanded = false
		selected = suggestions.firstOrNull { textOf(it) == prefillText }
		query.value = prefillText
	}

	fun itemText(item: T): String = textOf(item)
}
