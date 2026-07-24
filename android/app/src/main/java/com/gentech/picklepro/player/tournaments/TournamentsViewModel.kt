package com.gentech.picklepro.player.tournaments

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.data.remote.dto.TournamentDto
import com.gentech.picklepro.data.repository.TournamentBrowseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TournamentsUiState(
    val tournaments: List<TournamentDto> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
) {
    private val filtered: List<TournamentDto>
        get() = if (searchQuery.isBlank()) {
            tournaments
        } else {
            tournaments.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.venue?.contains(searchQuery, ignoreCase = true) == true
            }
        }

    /** Spec §4.4 grouping: Upcoming (open registration) / Ongoing / Finished. */
    val byStatus: Map<String, List<TournamentDto>>
        get() = filtered.groupBy { it.status }
}

class TournamentsViewModel(
    private val browseRepository: TournamentBrowseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TournamentsUiState())
    val uiState: StateFlow<TournamentsUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val tournaments = runCatching { browseRepository.listVisible() }.getOrDefault(emptyList())
            _uiState.update { it.copy(tournaments = tournaments, isLoading = false) }
        }
    }

    fun onSearchChange(query: String) = _uiState.update { it.copy(searchQuery = query) }
}

class TournamentsViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TournamentsViewModel(TournamentBrowseRepository(appContext)) as T
    }
}
