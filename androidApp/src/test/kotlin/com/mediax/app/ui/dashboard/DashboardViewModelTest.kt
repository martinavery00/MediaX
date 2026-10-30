package com.mediax.app.ui.dashboard

import com.mediax.shared.model.DashboardSnapshot
import com.mediax.shared.model.DashboardSummary
import com.mediax.shared.repository.MediaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads dashboard content`() =
        runTest(mainDispatcherRule.dispatcher) {
            val snapshot = DashboardSnapshot(DashboardSummary(4, 3, 250), emptyList())
            val viewModel = DashboardViewModel(repositoryReturning(snapshot))

            advanceUntilIdle()

            assertEquals(DashboardUiState.Content(snapshot), viewModel.state.value)
        }

    @Test
    fun `maps repository failure to friendly error`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel =
                DashboardViewModel(
                    object : MediaRepository {
                        override suspend fun loadDashboard(): DashboardSnapshot = error("Offline")
                    },
                )

            advanceUntilIdle()

            assertTrue(viewModel.state.value is DashboardUiState.Error)
        }

    private fun repositoryReturning(snapshot: DashboardSnapshot) =
        object : MediaRepository {
            override suspend fun loadDashboard(): DashboardSnapshot = snapshot
        }
}
