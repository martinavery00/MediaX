package com.mediax.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mediax.shared.model.DashboardSnapshot
import com.mediax.shared.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    data class Content(
        val snapshot: DashboardSnapshot,
    ) : DashboardUiState

    data class Error(
        val message: String,
    ) : DashboardUiState
}

class DashboardViewModel(
    private val repository: MediaRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val state: StateFlow<DashboardUiState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        mutableState.value = DashboardUiState.Loading
        viewModelScope.launch {
            mutableState.value =
                runCatching { repository.loadDashboard() }
                    .fold(
                        onSuccess = DashboardUiState::Content,
                        onFailure = { DashboardUiState.Error("We couldn't load your dashboard.") },
                    )
        }
    }

    companion object {
        fun factory(repository: MediaRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = DashboardViewModel(repository) as T
            }
    }
}
