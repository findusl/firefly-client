package de.lehrbaum.firefly

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
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
		Column(
			modifier = Modifier
				.background(MaterialTheme.colorScheme.primaryContainer)
				.safeContentPadding()
				.fillMaxSize()
				.padding(16.dp),
		) {
			ExposedDropdownMenuBox(
				expanded = viewModel.expandedSource,
				onExpandedChange = { viewModel.expandedSource = !viewModel.expandedSource },
			) {
				OutlinedTextField(
					modifier = Modifier.fillMaxWidth().menuAnchor(),
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
					modifier = Modifier.fillMaxWidth().menuAnchor(),
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
			Row(modifier = Modifier.fillMaxWidth()) {
				Button(onClick = { scope.launch { viewModel.save() } }) { Text("Save") }
				Button(onClick = { viewModel.clear() }) { Text("Clear") }
			}
		}
	}
}
