package com.gentech.picklepro.organizer.bracket

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.data.remote.dto.DivisionDto
import com.gentech.picklepro.data.repository.BracketGenerationResult
import com.gentech.picklepro.data.repository.BracketRepository
import com.gentech.picklepro.data.repository.DivisionRepository
import com.gentech.picklepro.data.repository.Entrant
import com.gentech.picklepro.data.repository.RegistrationRepository
import com.gentech.picklepro.data.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BracketSetupUiState(
    val division: DivisionDto? = null,
    val seeds: List<Entrant> = emptyList(),
    val unpairedCount: Int = 0,
    val canRegenerate: Boolean = true,
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val message: String? = null,
    val generated: Boolean = false,
)

/** Seeding + bracket/round-robin generation (spec §5.5). */
class BracketSetupViewModel(
    private val divisionId: String,
    private val divisionRepository: DivisionRepository,
    private val registrationRepository: RegistrationRepository,
    private val teamRepository: TeamRepository,
    private val bracketRepository: BracketRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BracketSetupUiState())
    val uiState: StateFlow<BracketSetupUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val division = runCatching { divisionRepository.get(divisionId) }.getOrNull()
            val registrations = runCatching { registrationRepository.listForDivision(divisionId) }.getOrDefault(emptyList())
            val teams = if (division?.eventType == "singles") {
                emptyList()
            } else {
                runCatching { teamRepository.listForDivision(divisionId) }.getOrDefault(emptyList())
            }
            val entrants = division?.let { resolveEntrants(it, registrations, teams) } ?: emptyList()
            val unpaired = if (division?.eventType != "singles") registrations.count { it.teamId == null } else 0
            val canRegen = runCatching { bracketRepository.canRegenerate(divisionId) }.getOrDefault(true)

            _uiState.update {
                it.copy(
                    division = division,
                    seeds = entrants.sortedByDescending { e -> e.seedRating },
                    unpairedCount = unpaired,
                    canRegenerate = canRegen,
                    isLoading = false,
                )
            }
        }
    }

    fun moveUp(index: Int) = reorder(index, index - 1)
    fun moveDown(index: Int) = reorder(index, index + 1)

    private fun reorder(from: Int, to: Int) = _uiState.update { state ->
        if (to < 0 || to >= state.seeds.size) return@update state
        val updated = state.seeds.toMutableList()
        val item = updated.removeAt(from)
        updated.add(to, item)
        state.copy(seeds = updated)
    }

    fun generate() {
        val state = _uiState.value
        val division = state.division ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, message = null) }
            val result = if (division.format == "round_robin") {
                bracketRepository.generateRoundRobin(divisionId, state.seeds)
            } else {
                bracketRepository.generateSingleElimination(divisionId, state.seeds, division.bronzeMatch)
            }
            when (result) {
                is BracketGenerationResult.Success -> {
                    _uiState.update { it.copy(isGenerating = false, generated = true) }
                }
                is BracketGenerationResult.NotEnoughEntrants -> {
                    _uiState.update { it.copy(isGenerating = false, message = "not_enough") }
                }
                is BracketGenerationResult.AlreadyHasResults -> {
                    _uiState.update { it.copy(isGenerating = false, message = "has_results") }
                }
            }
        }
    }
}

class BracketSetupViewModelFactory(
    private val appContext: Context,
    private val divisionId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return BracketSetupViewModel(
            divisionId = divisionId,
            divisionRepository = DivisionRepository(appContext),
            registrationRepository = RegistrationRepository(appContext),
            teamRepository = TeamRepository(appContext),
            bracketRepository = BracketRepository(appContext),
        ) as T
    }
}
