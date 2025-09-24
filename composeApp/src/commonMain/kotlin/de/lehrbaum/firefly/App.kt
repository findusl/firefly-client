@file:Suppress("ktlint:standard:no-wildcard-imports")

package de.lehrbaum.firefly
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.*
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview

private val SIMPLE_FORMAT = LocalDateTime.Format {
	date(LocalDate.Formats.ISO)
	char(' ')
	hour()
	char(':')
	minute()
}

@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun App(
	viewModelFactory: () -> MainViewModel = {
		MainViewModel(
			HttpClient {
				install(ContentNegotiation) {
					json(Json { ignoreUnknownKeys = true })
				}
			},
		)
	},
) {
	MaterialTheme {
		val viewModel = remember(viewModelFactory) { viewModelFactory() }
		val scope = rememberCoroutineScope()
		val timeZone = remember { TimeZone.currentSystemDefault() }
		var showDateTimePicker by remember { mutableStateOf(false) }
		Column(
			modifier = Modifier
				.background(MaterialTheme.colorScheme.primaryContainer)
				.safeContentPadding()
				.fillMaxSize()
				.padding(16.dp),
		) {
			if (viewModel.errorMessage != null) {
				Text(
					viewModel.errorMessage!!,
					modifier = Modifier
						.fillMaxWidth()
						.background(MaterialTheme.colorScheme.errorContainer)
						.padding(8.dp),
					color = MaterialTheme.colorScheme.onErrorContainer,
				)
			}
			AutocompleteTextField(field = viewModel.sourceField, label = "Source account")
			AutocompleteTextField(field = viewModel.targetField, label = "Target account")
			AutocompleteTextField(field = viewModel.descriptionField, label = "Description")
			AutocompleteTextField(field = viewModel.tagField, label = "Tag (optional)")
			OutlinedTextField(
				modifier = Modifier.fillMaxWidth(),
				value = viewModel.amount,
				onValueChange = { viewModel.amount = it },
				label = { Text("Amount") },
			)
			OutlinedTextField(
				modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) showDateTimePicker = true },
				value = SIMPLE_FORMAT.format(viewModel.dateTime),
				onValueChange = {},
				label = { Text("Date & Time") },
				readOnly = true,
			)
			Row(modifier = Modifier.fillMaxWidth()) {
				Button(
					onClick = { scope.launch { viewModel.save() } },
					enabled = !viewModel.isSaving,
				) {
					if (viewModel.isSaving) {
						CircularProgressIndicator(
							modifier = Modifier.size(16.dp),
							color = MaterialTheme.colorScheme.onPrimary,
							strokeWidth = 2.dp,
						)
						Spacer(Modifier.width(8.dp))
					}
					Text("Save")
				}
				Button(onClick = { viewModel.clear() }) { Text("Clear") }
			}
		}
		if (showDateTimePicker) {
			DateTimePickerDialog(
				initialDateTime = viewModel.dateTime,
				onDateTimeChange = viewModel::onDateTimeChange,
				onDismissRequest = { showDateTimePicker = false },
				timeZone = timeZone,
			)
		}
	}
}
