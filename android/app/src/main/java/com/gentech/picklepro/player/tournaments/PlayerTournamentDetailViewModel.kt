package com.gentech.picklepro.player.tournaments

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.data.remote.dto.DivisionDto
import com.gentech.picklepro.data.remote.dto.TournamentDto
import com.gentech.picklepro.data.repository.DivisionRepository
import com.gentech.picklepro.data.repository.DivisionResults
import com.gentech.picklepro.data.repository.ProfileRepository
import com.gentech.picklepro.data.repository.RegistrationRepository
import com.gentech.picklepro.data.repository.ResultsRepository
import com.gentech.picklepro.data.repository.TeamRepository
import com.gentech.picklepro.data.repository.TournamentBrowseRepository
import com.gentech.picklepro.data.repository.TournamentRepository
import com.gentech.picklepro.organizer.bracket.resolveEntrants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A published division's results, names already resolved for display. */
data class PublishedResults(val results: DivisionResults, val nameByRef: Map<String, String>)

data class PlayerTournamentDetailUiState(
    val tournament: TournamentDto? = null,
    val organizerName: String = "",
    val divisions: List<DivisionDto> = emptyList(),
    val registeredCountByDivision: Map<String, Int> = emptyMap(),
    val resultsByDivision: Map<String, PublishedResults> = emptyMap(),
    val isLoading: Boolean = true,
)

/** Player-facing tournament detail (spec §4.4); published divisions also show results (spec §5.8). */
class PlayerTournamentDetailViewModel(
    private val tournamentId: String,
    private val tournamentRepository: TournamentRepository,
    private val profileRepository: ProfileRepository,
    private val divisionRepository: DivisionRepository,
    private val browseRepository: TournamentBrowseRepository,
    private val registrationRepository: RegistrationRepository,
    private val teamRepository: TeamRepository,
    private val resultsRepository: ResultsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerTournamentDetailUiState())
    val uiState: StateFlow<PlayerTournamentDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            try {
                val tournament = tournamentRepository.get(tournamentId)
                val organizerName = runCatching { profileRepository.findById(tournament.organizerId)?.name }
                    .getOrNull().orEmpty()
                val divisions = divisionRepository.listForTournament(tournamentId)
                val counts = runCatching { browseRepository.registrationCounts(divisions.map { it.id }) }
                    .getOrDefault(emptyMap())

                val resultsByDivision = mutableMapOf<String, PublishedResults>()
                for (division in divisions.filter { it.published }) {
                    runCatching {
                        val registrations = registrationRepository.listForDivision(division.id)
                        val teams = if (division.eventType == "singles") {
                            emptyList()
                        } else {
                            teamRepository.listForDivision(division.id)
                        }
                        val entrants = resolveEntrants(division, registrations, teams)
                        resultsByDivision[division.id] = PublishedResults(
                            results = resultsRepository.deriveResults(division, entrants),
                            nameByRef = entrants.associate { it.ref to it.name },
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        tournament = tournament,
                        organizerName = organizerName,
                        divisions = divisions,
                        registeredCountByDivision = counts,
                        resultsByDivision = resultsByDivision,
                        isLoading = false,
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

class PlayerTournamentDetailViewModelFactory(
    private val appContext: Context,
    private val tournamentId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PlayerTournamentDetailViewModel(
            tournamentId = tournamentId,
            tournamentRepository = TournamentRepository(appContext),
            profileRepository = ProfileRepository(appContext),
            divisionRepository = DivisionRepository(appContext),
            browseRepository = TournamentBrowseRepository(appContext),
            registrationRepository = RegistrationRepository(appContext),
            teamRepository = TeamRepository(appContext),
            resultsRepository = ResultsRepository(appContext),
        ) as T
    }
}
