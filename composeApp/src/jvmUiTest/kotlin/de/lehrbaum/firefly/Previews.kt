package de.lehrbaum.firefly

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun App_Preview() {
	App()
}

@Preview
@Composable
private fun AutocompleteTextField_Preview() {
	val scope = rememberCoroutineScope()
	val fetcher: suspend (String) -> List<String> = { q ->
		val all = listOf("Apple", "Apricot", "Banana", "Blueberry", "Cherry", "Grape")
		if (q.isBlank()) all else all.filter { it.contains(q, ignoreCase = true) }
	}
	val field = AutocompleteField(scope, fetcher)

	AutocompleteTextField(field, "Fruit")
}
