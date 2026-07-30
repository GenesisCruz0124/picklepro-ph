package com.gentech.picklepro.organizer.results

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.R
import com.gentech.picklepro.data.remote.dto.DivisionDto
import com.gentech.picklepro.data.repository.DivisionRepository
import com.gentech.picklepro.data.repository.DivisionResults
import com.gentech.picklepro.data.repository.RegistrationRepository
import com.gentech.picklepro.data.repository.ResultsRepository
import com.gentech.picklepro.data.repository.TeamRepository
import com.gentech.picklepro.organizer.bracket.resolveEntrants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResultsUiState(
    val division: DivisionDto? = null,
    val results: DivisionResults? = null,
    val nameByRef: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true,
    val isTogglingPublish: Boolean = false,
    val errorMessage: String? = null,
)

/** Tabulation & results per division (spec §5.8). */
class ResultsViewModel(
    private val context: Context,
    private val divisionId: String,
    private val divisionRepository: DivisionRepository,
    private val registrationRepository: RegistrationRepository,
    private val teamRepository: TeamRepository,
    private val resultsRepository: ResultsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val division = divisionRepository.get(divisionId)
                val registrations = registrationRepository.listForDivision(divisionId)
                val teams = if (division.eventType == "singles") {
                    emptyList()
                } else {
                    teamRepository.listForDivision(divisionId)
                }
                val entrants = resolveEntrants(division, registrations, teams)
                val results = resultsRepository.deriveResults(division, entrants)
                _uiState.update {
                    it.copy(
                        division = division,
                        results = results,
                        nameByRef = entrants.associate { e -> e.ref to e.name },
                        isLoading = false,
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = context.getString(R.string.results_error_load_failed)) }
            }
        }
    }

    fun setPublished(published: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTogglingPublish = true, errorMessage = null) }
            runCatching { divisionRepository.setPublished(divisionId, published) }
                .onFailure {
                    _uiState.update { s -> s.copy(errorMessage = context.getString(R.string.results_error_publish_failed)) }
                }
            _uiState.update { it.copy(isTogglingPublish = false) }
            refresh()
        }
    }
}

class ResultsViewModelFactory(
    private val appContext: Context,
    private val divisionId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ResultsViewModel(
            context = appContext,
            divisionId = divisionId,
            divisionRepository = DivisionRepository(appContext),
            registrationRepository = RegistrationRepository(appContext),
            teamRepository = TeamRepository(appContext),
            resultsRepository = ResultsRepository(appContext),
        ) as T
    }
}
