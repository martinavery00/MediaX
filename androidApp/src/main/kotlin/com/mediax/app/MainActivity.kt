package com.mediax.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediax.app.ui.dashboard.DashboardScreen
import com.mediax.app.ui.dashboard.DashboardViewModel
import com.mediax.app.ui.theme.MediaXTheme
import com.mediax.shared.repository.InMemoryMediaRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MediaXTheme {
                val dashboardViewModel: DashboardViewModel =
                    viewModel(
                        factory = DashboardViewModel.factory(InMemoryMediaRepository()),
                    )
                val state by dashboardViewModel.state.collectAsStateWithLifecycle()

                DashboardScreen(
                    state = state,
                    onRetry = dashboardViewModel::refresh,
                )
            }
        }
    }
}
