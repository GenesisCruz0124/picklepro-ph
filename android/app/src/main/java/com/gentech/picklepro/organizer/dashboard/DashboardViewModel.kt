package com.gentech.picklepro.organizer.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.data.local.PickleProDatabase
import com.gentech.picklepro.data.local.dao.PendingOpDao
import com.gentech.picklepro.data.remote.dto.TournamentDto
import com.gentech.picklepro.data.repository.AuthRepository
import com.gentech.picklepro.data.repository.DashboardCounts
import com.gentech.picklepro.data.repository.DashboardRepository
import com.gentech.picklepro.data.repository.TournamentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val tournaments: List<TournamentDto> = emptyList(),
    val counts: DashboardCounts = DashboardCounts(0, 0, 0),
    val isLoading: Boolean = true,
)

class DashboardViewModel(
    private val organizerId: String,
    private val tournamentRepository: TournamentRepository,
    private val dashboardRepository: DashboardRepository,
    pendingOpDao: PendingOpDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    /** Unsynced ops (spec §5.10) — non-zero means results/Elo haven't reached the server yet. */
    val pendingOpCount: StateFlow<Int> = pendingOpDao.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val tournaments = runCatching { tournamentRepository.listMine(organizerId) }.getOrDefault(emptyList())
            val counts = runCatching { dashboardRepository.getCounts(tournaments.map { it.id }) }
                .getOrDefault(DashboardCounts(0, 0, 0))
            _uiState.update { it.copy(tournaments = tournaments, counts = counts, isLoading = false) }
        }
    }
}

val DashboardUiState.activeTournamentCount: Int
    get() = tournaments.count { it.status == "registration" || it.status == "ongoing" }

class DashboardViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val userId = AuthRepository(appContext).currentUserId
            ?: error("DashboardViewModel requires a signed-in user")
        @Suppress("UNCHECKED_CAST")
        return DashboardViewModel(
            organizerId = userId,
            tournamentRepository = TournamentRepository(appContext),
            dashboardRepository = DashboardRepository(appContext),
            pendingOpDao = PickleProDatabase.get(appContext).pendingOpDao(),
        ) as T
    }
}
