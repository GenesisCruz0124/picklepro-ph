package com.gentech.picklepro.organizer.scoreboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.data.local.entity.MatchEntity
import com.gentech.picklepro.data.remote.dto.DivisionDto
import com.gentech.picklepro.data.repository.DivisionRepository
import com.gentech.picklepro.data.repository.MatchRepository
import com.gentech.picklepro.data.repository.RegistrationRepository
import com.gentech.picklepro.data.repository.TeamRepository
import com.gentech.picklepro.data.repository.TournamentRepository
import com.gentech.picklepro.organizer.bracket.resolveEntrants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScoreboardUiState(
    val match: MatchEntity? = null,
    val division: DivisionDto? = null,
    val tournamentName: String = "",
    val organizerLogoUrl: String? = null,
    val nameA: String = "",
    val nameB: String = "",
    val isLoading: Boolean = true,
)

/** Read-only fullscreen display for a live match (spec §5.7) — a second device or spectator screen. */
class ScoreboardViewModel(
    private val matchId: String,
    private val matchRepository: MatchRepository,
    private val divisionRepository: DivisionRepository,
    private val tournamentRepository: TournamentRepository,
    private val registrationRepository: RegistrationRepository,
    private val teamRepository: TeamRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScoreboardUiState())
    val uiState: StateFlow<ScoreboardUiState> = _uiState

    init {
        viewModelScope.launch {
            matchRepository.ensureLoaded(matchId)
            val match = matchRepository.get(matchId)
            val division = match?.let { runCatching { divisionRepository.get(it.divisionId) }.getOrNull() }
            val tournament = division?.let { runCatching { tournamentRepository.get(it.tournamentId) }.getOrNull() }
            val (nameA, nameB) = if (match != null && division != null) resolveNames(match, division) else "" to ""
            _uiState.update {
                it.copy(
                    match = match,
                    division = division,
                    tournamentName = tournament?.name.orEmpty(),
                    organizerLogoUrl = tournament?.logoUrl,
                    nameA = nameA,
                    nameB = nameB,
                    isLoading = false,
                )
            }
        }
        viewModelScope.launch {
            matchRepository.observe(matchId).collect { match -> _uiState.update { it.copy(match = match) } }
        }
    }

    private suspend fun resolveNames(match: MatchEntity, division: DivisionDto): Pair<String, String> {
        val registrations = runCatching { registrationRepository.listForDivision(division.id) }.getOrDefault(emptyList())
        val teams = if (division.eventType == "singles") {
            emptyList()
        } else {
            runCatching { teamRepository.listForDivision(division.id) }.getOrDefault(emptyList())
        }
        val nameByRef = resolveEntrants(division, registrations, teams).associate { it.ref to it.name }
        return (nameByRef[match.sideARef] ?: "Team A") to (nameByRef[match.sideBRef] ?: "Team B")
    }
}

class ScoreboardViewModelFactory(
    private val appContext: Context,
    private val matchId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ScoreboardViewModel(
            matchId = matchId,
            matchRepository = MatchRepository(appContext),
            divisionRepository = DivisionRepository(appContext),
            tournamentRepository = TournamentRepository(appContext),
            registrationRepository = RegistrationRepository(appContext),
            teamRepository = TeamRepository(appContext),
        ) as T
    }
}
