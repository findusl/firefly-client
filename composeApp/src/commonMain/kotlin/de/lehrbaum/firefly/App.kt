package de.lehrbaum.firefly

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
fun App() {
	MaterialTheme {
		val client = remember {
			HttpClient {
				install(ContentNegotiation) {
					json(Json { ignoreUnknownKeys = true })
				}
			}
		}
		val viewModel = remember { MainViewModel(client) }
		LaunchedEffect(Unit) { viewModel.loadAccounts() }
		val scope = rememberCoroutineScope()
		var showDatePicker by remember { mutableStateOf(false) }
		var showTimePicker by remember { mutableStateOf(false) }
		var pendingDateMillis by remember { mutableStateOf<Long?>(null) }
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
			ExposedDropdownMenuBox(
				expanded = viewModel.expandedSource,
				onExpandedChange = { viewModel.expandedSource = !viewModel.expandedSource },
			) {
				OutlinedTextField(
					modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
					value = viewModel.sourceText,
					onValueChange = viewModel::onSourceTextChange,
					label = { Text("Source account") },
				)
				val suggestions =
					viewModel.accounts.filter { it.name.contains(viewModel.sourceText, true) && it.type == "asset" }
				DropdownMenu(
					expanded = viewModel.expandedSource,
					onDismissRequest = { viewModel.expandedSource = false },
				) {
					suggestions.forEach { account ->
						DropdownMenuItem(
							onClick = {
								viewModel.onSourceTextChange(account.name)
								viewModel.expandedSource = false
							},
							text = { Text(account.name) },
						)
					}
				}
			}
			ExposedDropdownMenuBox(
				expanded = viewModel.expandedTarget,
				onExpandedChange = { viewModel.expandedTarget = !viewModel.expandedTarget },
			) {
				OutlinedTextField(
					modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
					value = viewModel.targetText,
					onValueChange = viewModel::onTargetTextChange,
					label = { Text("Target account") },
				)
				val suggestions = viewModel.accounts.filter {
					it.name.contains(viewModel.targetText, true) && it.id != viewModel.selectedSource?.id
				}
				DropdownMenu(
					expanded = viewModel.expandedTarget,
					onDismissRequest = { viewModel.expandedTarget = false },
				) {
					suggestions.forEach { account ->
						DropdownMenuItem(
							onClick = {
								viewModel.onTargetTextChange(account.name)
								viewModel.expandedTarget = false
							},
							text = { Text(account.name) },
						)
					}
				}
			}
			OutlinedTextField(
				modifier = Modifier.fillMaxWidth(),
				value = viewModel.description,
				onValueChange = { viewModel.description = it },
				label = { Text("Description") },
			)
			OutlinedTextField(
				modifier = Modifier.fillMaxWidth(),
				value = viewModel.amount,
				onValueChange = { viewModel.amount = it },
				label = { Text("Amount") },
			)
			OutlinedTextField(
				modifier = Modifier.fillMaxWidth(),
				value = viewModel.dateTime.toString(),
				onValueChange = {},
				label = { Text("Date & Time") },
				readOnly = true,
			)
			Button(onClick = { showDatePicker = true }) { Text("Set Date & Time") }
			if (showDatePicker) {
				val datePickerState = rememberDatePickerState(initialSelectedDateMillis = viewModel.dateTime.toEpochMilliseconds())
				DatePickerDialog(
					onDismissRequest = { showDatePicker = false },
					confirmButton = {
						TextButton(
							onClick = {
								val millis = datePickerState.selectedDateMillis
								if (millis != null) {
									pendingDateMillis = millis
									showDatePicker = false
									showTimePicker = true
								} else {
									showDatePicker = false
								}
							},
						) { Text("OK") }
					},
					dismissButton = {
						TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
					},
				) {
					DatePicker(state = datePickerState)
				}
			}
			if (showTimePicker) {
				val hour = ((viewModel.dateTime.toEpochMilliseconds() % (24L * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()
				val minute = ((viewModel.dateTime.toEpochMilliseconds() % (60 * 60 * 1000)) / (60 * 1000)).toInt()
				val timePickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute)
				AlertDialog(
					onDismissRequest = { showTimePicker = false },
					confirmButton = {
						TextButton(
							onClick = {
								val base = pendingDateMillis ?: viewModel.dateTime.toEpochMilliseconds()
								val newInstant = Instant
									.fromEpochMilliseconds(base)
									.plus(timePickerState.hour.hours)
									.plus(timePickerState.minute.minutes)
								viewModel.onDateTimeChange(newInstant)
								showTimePicker = false
							},
						) { Text("OK") }
					},
					dismissButton = {
						TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
					},
					text = {
						TimePicker(state = timePickerState)
					},
				)
			}
			Row(modifier = Modifier.fillMaxWidth()) {
				Button(onClick = { scope.launch { viewModel.save() } }) { Text("Save") }
				Button(onClick = { viewModel.clear() }) { Text("Clear") }
			}
		}
	}
}
