package de.lehrbaum.firefly

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun DateTimePickerDialog(
	initialDateTime: LocalDateTime,
	onDateTimeChange: (LocalDateTime) -> Unit,
	onDismissRequest: () -> Unit,
	timeZone: TimeZone,
) {
	var showDatePicker by remember { mutableStateOf(true) }
	var showTimePicker by remember { mutableStateOf(false) }
	if (showDatePicker) {
		val datePickerState = rememberDatePickerState(
			initialSelectedDateMillis = initialDateTime.toInstant(timeZone).toEpochMilliseconds(),
		)
		DatePickerDialog(
			onDismissRequest = {
				showDatePicker = false
				onDismissRequest()
			},
			confirmButton = {
				TextButton(onClick = {
					val millis = datePickerState.selectedDateMillis
					if (millis != null) {
						val selectedDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone).date
						onDateTimeChange(LocalDateTime(selectedDate, initialDateTime.time))
					}
					showDatePicker = false
					showTimePicker = true
				}) { Text("OK") }
			},
			dismissButton = {
				TextButton(onClick = {
					showDatePicker = false
					onDismissRequest()
				}) { Text("Cancel") }
			},
		) {
			DatePicker(state = datePickerState)
		}
	}
	if (showTimePicker) {
		val timePickerState = rememberTimePickerState(
			initialHour = initialDateTime.hour,
			initialMinute = initialDateTime.minute,
			is24Hour = true,
		)
		AlertDialog(
			onDismissRequest = {
				showTimePicker = false
				onDismissRequest()
			},
			confirmButton = {
				TextButton(onClick = {
					val newTime = LocalTime(timePickerState.hour, timePickerState.minute)
					onDateTimeChange(LocalDateTime(initialDateTime.date, newTime))
					showTimePicker = false
					onDismissRequest()
				}) { Text("OK") }
			},
			dismissButton = {
				TextButton(onClick = {
					showTimePicker = false
					onDismissRequest()
				}) { Text("Cancel") }
			},
			text = { TimePicker(state = timePickerState) },
		)
	}
}
