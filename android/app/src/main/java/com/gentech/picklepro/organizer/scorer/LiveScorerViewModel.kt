package com.gentech.picklepro.organizer.scorer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.core.scoring.ScoringMode
import com.gentech.picklepro.core.scoring.Team
import com.gentech.picklepro.data.local.entity.MatchEntity
import com.gentech.picklepro.data.remote.dto.DivisionDto
import com.gentech.picklepro.data.repository.DivisionRepository
import com.gentech.picklepro.data.repository.MatchRepository
import com.gentech.picklepro.data.repository.RegistrationRepository
import com.gentech.picklepro.data.repository.ScoringConfig
import com.gentech.picklepro.data.repository.TeamRepository
import com.gentech.picklepro.organizer.bracket.resolveEntrants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveScorerUiState(
    val match: MatchEntity? = null,
    val division: DivisionDto? = null,
    val nameA: String = "",
    val nameB: String = "",
    val isLoading: Boolean = true,
    val showCompleteDialog: Boolean = false,
)

/** Live Scorer (spec §5.6). Every action goes through [MatchRepository], which is Room-first. */
class LiveScorerViewModel(
    private val matchId: String,
    private val matchRepository: MatchRepository,
    private val divisionRepository: DivisionRepository,
    private val registrationRepository: RegistrationRepository,
    private val teamRepository: TeamRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveScorerUiState())
    val uiState: StateFlow<LiveScorerUiState> = _uiState

    var config: ScoringConfig? = null
        private set

    init {
        viewModelScope.launch {
            matchRepository.ensureLoaded(matchId)
            val match = matchRepository.get(matchId)
            val division = match?.let { runCatching { divisionRepository.get(it.divisionId) }.getOrNull() }
            if (division != null) {
                config = ScoringConfig(
                    isDoubles = division.eventType != "singles",
                    mode = if (division.scoringMode == "rally") ScoringMode.RALLY else ScoringMode.SIDEOUT,
                    gameTo = division.gameTo,
                    winBy2 = division.winBy2,
                    bestOf = division.bestOf,
                    isSingleElimination = division.format == "single_elim",
                )
            }
            val (nameA, nameB) = if (match != null) resolveNames(match, division) else "" to ""
            _uiState.update {
                it.copy(match = match, division = division, nameA = nameA, nameB = nameB, isLoading = false)
            }
        }
        viewModelScope.launch {
            matchRepository.observe(matchId).collect { match ->
                val wasNotDone = _uiState.value.match?.status != "done"
                _uiState.update { state ->
                    state.copy(
                        match = match,
                        showCompleteDialog = state.showCompleteDialog || (match?.status == "done" && wasNotDone),
                    )
                }
            }
        }
    }

    private suspend fun resolveNames(match: MatchEntity, division: DivisionDto?): Pair<String, String> {
        if (division == null) return "" to ""
        val registrations = runCatching { registrationRepository.listForDivision(division.id) }.getOrDefault(emptyList())
        val teams = if (division.eventType == "singles") {
            emptyList()
        } else {
            runCatching { teamRepository.listForDivision(division.id) }.getOrDefault(emptyList())
        }
        val nameByRef = resolveEntrants(division, registrations, teams).associate { it.ref to it.name }
        return (nameByRef[match.sideARef] ?: "Team A") to (nameByRef[match.sideBRef] ?: "Team B")
    }

    /** side-out mode: tapping the serving team's panel scores them a point. */
    fun onServingPanelTapped() {
        val match = _uiState.value.match ?: return
        val cfg = config ?: return
        val state = matchRepository.currentGameState(match, cfg)
        viewModelScope.launch { matchRepository.recordRally(matchId, state.servingTeam, cfg) }
    }

    /** side-out mode: the receiving team won the rally — server rotates or side-out. */
    fun onSideOut() {
        val match = _uiState.value.match ?: return
        val cfg = config ?: return
        val state = matchRepository.currentGameState(match, cfg)
        val receivingTeam = if (state.servingTeam == Team.A) Team.B else Team.A
        viewModelScope.launch { matchRepository.recordRally(matchId, receivingTeam, cfg) }
    }

    /** rally mode: either panel directly awards that team the point. */
    fun onRallyPointTapped(team: Team) {
        val cfg = config ?: return
        viewModelScope.launch { matchRepository.recordRally(matchId, team, cfg) }
    }

    fun onTimeout(team: Team) {
        val cfg = config ?: return
        viewModelScope.launch { matchRepository.recordTimeout(matchId, team, cfg) }
    }

    fun onUndo() {
        val cfg = config ?: return
        viewModelScope.launch { matchRepository.undo(matchId, cfg) }
    }

    fun dismissCompleteDialog() = _uiState.update { it.copy(showCompleteDialog = false) }
}

class LiveScorerViewModelFactory(
    private val appContext: Context,
    private val matchId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LiveScorerViewModel(
            matchId = matchId,
            matchRepository = MatchRepository(appContext),
            divisionRepository = DivisionRepository(appContext),
            registrationRepository = RegistrationRepository(appContext),
            teamRepository = TeamRepository(appContext),
        ) as T
    }
}
