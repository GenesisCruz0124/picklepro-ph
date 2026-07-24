package com.gentech.picklepro.organizer.registration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.data.remote.dto.DivisionDto
import com.gentech.picklepro.data.repository.DivisionRepository
import com.gentech.picklepro.data.repository.RegisterOutcome
import com.gentech.picklepro.data.repository.RegistrationRepository
import com.gentech.picklepro.data.repository.ShellPlayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ManualAddUiState(
    val division: DivisionDto? = null,
    val name: String = "",
    val declaredRating: Float = 3.0f,
    val isLoading: Boolean = false,
    val outcome: ScanOutcomeUi? = null,
)

/** Manual add (spec §5.4): name + declared tier -> shell player, registered into the division. */
class ManualAddViewModel(
    private val divisionId: String,
    private val divisionRepository: DivisionRepository,
    private val shellPlayerRepository: ShellPlayerRepository,
    private val registrationRepository: RegistrationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualAddUiState())
    val uiState: StateFlow<ManualAddUiState> = _uiState

    init {
        viewModelScope.launch {
            val division = runCatching { divisionRepository.get(divisionId) }.getOrNull()
            _uiState.update { it.copy(division = division) }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onDeclaredRatingChange(value: Float) = _uiState.update { it.copy(declaredRating = value) }

    fun submit() {
        val state = _uiState.value
        val division = state.division ?: return
        if (state.name.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val declared = state.declaredRating.toDouble()
                val shell = shellPlayerRepository.create(
                    name = state.name.trim(),
                    declaredRating = declared,
                    eventType = division.eventType,
                )
                val outcome = registrationRepository.registerOrCheckIn(
                    divisionId = divisionId,
                    playerId = shell.id,
                    maxRating = division.maxRating,
                    maxSlots = division.maxSlots,
                    effectiveRating = declared,
                )
                val ui = when (outcome) {
                    is RegisterOutcome.Registered -> ScanOutcomeUi.Registered(shell.name)
                    is RegisterOutcome.CheckedIn -> ScanOutcomeUi.CheckedIn(shell.name)
                    is RegisterOutcome.Rejected -> ScanOutcomeUi.Rejected(shell.name, outcome.reason)
                }
                _uiState.update { it.copy(isLoading = false, outcome = ui, name = "") }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(isLoading = false, outcome = ScanOutcomeUi.Error("May problema. Subukan ulit."))
                }
            }
        }
    }

    fun dismissOutcome() = _uiState.update { it.copy(outcome = null) }
}

class ManualAddViewModelFactory(
    private val appContext: Context,
    private val divisionId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ManualAddViewModel(
            divisionId = divisionId,
            divisionRepository = DivisionRepository(appContext),
            shellPlayerRepository = ShellPlayerRepository(appContext),
            registrationRepository = RegistrationRepository(appContext),
        ) as T
    }
}
