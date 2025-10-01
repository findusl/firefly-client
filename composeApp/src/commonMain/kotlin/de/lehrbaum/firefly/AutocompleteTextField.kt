package de.lehrbaum.firefly

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AutocompleteTextField(
	field: AutocompleteField<T>,
	label: String,
	error: String? = null,
	onTextChange: (String) -> Unit = field::onTextChange,
	onSuggestionSelected: (T) -> Unit = field::select,
) {
	val focusRequester = remember { FocusRequester() }
	ExposedDropdownMenuBox(
		expanded = field.expanded,
		onExpandedChange = { expanded ->
			focusRequester.requestFocus()
			field.expanded = expanded
		},
	) {
		OutlinedTextField(
			modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).menuAnchor(MenuAnchorType.PrimaryEditable),
			value = field.text,
			onValueChange = onTextChange,
			label = { Text(label) },
			isError = error != null,
			supportingText = error?.let { message ->
				{ Text(message) }
			},
		)
		ExposedDropdownMenu(
			expanded = field.expanded,
			onDismissRequest = { field.expanded = false },
		) {
			field.suggestions.forEach { suggestion ->
				DropdownMenuItem(
					onClick = { onSuggestionSelected(suggestion) },
					text = { Text(field.itemText(suggestion)) },
				)
			}
		}
	}
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
