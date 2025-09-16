package de.lehrbaum.firefly

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AutocompleteTextField(field: AutocompleteField<T>, label: String) {
	ExposedDropdownMenuBox(
		expanded = field.expanded,
		onExpandedChange = { field.expanded = !field.expanded },
	) {
		OutlinedTextField(
			modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
			value = field.text,
			onValueChange = field::onTextChange,
			label = { Text(label) },
		)
		DropdownMenu(
			expanded = field.expanded,
			onDismissRequest = { field.expanded = false },
		) {
			field.suggestions.forEach { suggestion ->
				DropdownMenuItem(
					onClick = { field.select(suggestion) },
					text = { Text(field.itemText(suggestion)) },
				)
			}
		}
	}
}
