package de.lehrbaum.firefly

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MainViewModelTest {
	@Test
	fun clear_resetsFields() {
		val viewModel = MainViewModel(HttpClient(MockEngine { respondOk() }))
		viewModel.sourceText = "src"
		viewModel.targetText = "tgt"
		viewModel.description = "desc"
		viewModel.amount = "1"
		viewModel.selectedSource = Account("1", "A", "asset")
		viewModel.selectedTarget = Account("2", "B", "asset")
		viewModel.clear()
		assertEquals("", viewModel.sourceText)
		assertEquals("", viewModel.targetText)
		assertEquals("", viewModel.description)
		assertEquals("", viewModel.amount)
		assertNull(viewModel.selectedSource)
		assertNull(viewModel.selectedTarget)
	}
}
