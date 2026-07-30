package com.gentech.picklepro.organizer.division

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.data.remote.dto.DivisionDto
import com.gentech.picklepro.data.remote.dto.DivisionUpsertDto
import com.gentech.picklepro.data.repository.DivisionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Editable form fields, separate from [DivisionDto] since ratings/slots are typed text while editing. */
data class DivisionFormState(
    val id: String? = null,
    val name: String = "",
    val eventType: String = "singles",
    val skillGateEnabled: Boolean = false,
    val maxRating: String = "3.5",
    val ageBracketEnabled: Boolean = false,
    val ageBracket: String = "19+",
    val maxSlots: String = "",
    val format: String = "single_elim",
    val gameTo: Int = 11,
    val winBy2: Boolean = true,
    val bestOf: Int = 1,
    val scoringMode: String = "sideout",
    val bronzeMatch: Boolean = false,
)

data class DivisionSetupUiState(
    val divisions: List<DivisionDto> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val form: DivisionFormState? = null,
    val errorMessage: String? = null,
)

private val AGE_BRACKETS = listOf("19+", "35+", "50+", "60+")

class DivisionSetupViewModel(
    private val tournamentId: String,
    private val divisionRepository: DivisionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DivisionSetupUiState())
    val uiState: StateFlow<DivisionSetupUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val divisions = runCatching { divisionRepository.listForTournament(tournamentId) }.getOrDefault(emptyList())
            _uiState.update { it.copy(divisions = divisions, isLoading = false) }
        }
    }

    fun startNewDivision() = _uiState.update { it.copy(form = DivisionFormState()) }

    fun startEditDivision(division: DivisionDto) = _uiState.update {
        it.copy(
            form = DivisionFormState(
                id = division.id,
                name = division.name,
                eventType = division.eventType,
                skillGateEnabled = division.maxRating != null,
                maxRating = (division.maxRating ?: 3.5).toString(),
                ageBracketEnabled = division.ageBracket != null,
                ageBracket = division.ageBracket ?: AGE_BRACKETS.first(),
                maxSlots = division.maxSlots?.toString().orEmpty(),
                format = division.format,
                gameTo = division.gameTo,
                winBy2 = division.winBy2,
                bestOf = division.bestOf,
                scoringMode = division.scoringMode,
                bronzeMatch = division.bronzeMatch,
            ),
        )
    }

    fun cancelForm() = _uiState.update { it.copy(form = null, errorMessage = null) }

    fun updateForm(transform: (DivisionFormState) -> DivisionFormState) {
        _uiState.update { state -> state.form?.let { state.copy(form = transform(it)) } ?: state }
    }

    fun saveForm() {
        val form = _uiState.value.form ?: return
        if (form.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Kailangan ng pangalan ng division.") }
            return
        }
        val payload = DivisionUpsertDto(
            tournamentId = tournamentId,
            eventType = form.eventType,
            name = form.name.trim(),
            minRating = null,
            maxRating = if (form.skillGateEnabled) form.maxRating.toDoubleOrNull() else null,
            ageBracket = if (form.ageBracketEnabled) form.ageBracket else null,
            maxSlots = form.maxSlots.toIntOrNull(),
            format = form.format,
            gameTo = form.gameTo,
            winBy2 = form.winBy2,
            bestOf = form.bestOf,
            scoringMode = form.scoringMode,
            bronzeMatch = form.bronzeMatch,
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                if (form.id == null) {
                    divisionRepository.create(payload)
                } else {
                    divisionRepository.update(form.id, payload)
                }
                _uiState.update { it.copy(isSaving = false, form = null) }
                refresh()
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Hindi na-save. Subukan ulit.") }
            }
        }
    }

    fun deleteDivision(divisionId: String) {
        viewModelScope.launch {
            try {
                divisionRepository.delete(divisionId)
                refresh()
            } catch (t: Throwable) {
                _uiState.update { it.copy(errorMessage = "Hindi na-alis. Subukan ulit.") }
            }
        }
    }
}

class DivisionSetupViewModelFactory(
    private val appContext: Context,
    private val tournamentId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DivisionSetupViewModel(tournamentId, DivisionRepository(appContext)) as T
    }
}
