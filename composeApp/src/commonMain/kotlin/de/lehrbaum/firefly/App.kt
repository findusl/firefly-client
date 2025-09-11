package de.lehrbaum.firefly

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
	MaterialTheme {
		var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }
		val client = remember {
			HttpClient {
				install(ContentNegotiation) {
					json(Json { ignoreUnknownKeys = true })
				}
			}
		}
		LaunchedEffect(Unit) {
			accounts = fetchAccounts(client)
		}
		Column(
			modifier = Modifier
				.background(MaterialTheme.colorScheme.primaryContainer)
				.safeContentPadding()
				.fillMaxSize(),
		) {
			accounts.forEach { account ->
				Text(account.name)
			}
		}
	}
}
