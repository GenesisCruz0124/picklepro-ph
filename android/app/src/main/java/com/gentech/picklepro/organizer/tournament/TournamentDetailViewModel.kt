package com.gentech.picklepro.organizer.tournament

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.R
import com.gentech.picklepro.data.remote.dto.TournamentDto
import com.gentech.picklepro.data.remote.dto.TournamentUpdateDto
import com.gentech.picklepro.data.repository.TournamentRepository
import com.gentech.picklepro.organizer.common.nextTournamentStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TournamentDetailUiState(
    val tournament: TournamentDto? = null,
    val name: String = "",
    val venue: String = "",
    val description: String = "",
    val entryFeeNote: String = "",
    val courtCount: String = "1",
    val startDate: String = "",
    val endDate: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUploadingLogo: Boolean = false,
    val errorMessage: String? = null,
)

class TournamentDetailViewModel(
    private val context: Context,
    private val tournamentId: String,
    private val tournamentRepository: TournamentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TournamentDetailUiState())
    val uiState: StateFlow<TournamentDetailUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val t = tournamentRepository.get(tournamentId)
                _uiState.update {
                    it.copy(
                        tournament = t,
                        name = t.name,
                        venue = t.venue.orEmpty(),
                        description = t.description.orEmpty(),
                        entryFeeNote = t.entryFeeNote.orEmpty(),
                        courtCount = t.courtCount.toString(),
                        startDate = t.startDate.orEmpty(),
                        endDate = t.endDate.orEmpty(),
                        isLoading = false,
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = context.getString(R.string.tournament_error_load_failed)) }
            }
        }
    }

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v) }
    fun onVenueChange(v: String) = _uiState.update { it.copy(venue = v) }
    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }
    fun onEntryFeeNoteChange(v: String) = _uiState.update { it.copy(entryFeeNote = v) }
    fun onCourtCountChange(v: String) = _uiState.update { it.copy(courtCount = v.filter(Char::isDigit)) }
    fun onStartDateChange(v: String) = _uiState.update { it.copy(startDate = v) }
    fun onEndDateChange(v: String) = _uiState.update { it.copy(endDate = v) }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                tournamentRepository.update(
                    tournamentId,
                    TournamentUpdateDto(
                        name = state.name.trim(),
                        venue = state.venue.trim().ifBlank { null },
                        description = state.description.trim().ifBlank { null },
                        entryFeeNote = state.entryFeeNote.trim().ifBlank { null },
                        courtCount = state.courtCount.toIntOrNull() ?: 1,
                        startDate = state.startDate.trim().ifBlank { null },
                        endDate = state.endDate.trim().ifBlank { null },
                    ),
                )
                refresh()
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSaving = false, errorMessage = context.getString(R.string.common_error_save_failed)) }
            }
        }
    }

    fun advanceStatus() {
        val current = _uiState.value.tournament?.status ?: return
        val next = nextTournamentStatus(current) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                tournamentRepository.advanceStatus(tournamentId, next)
                refresh()
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSaving = false, errorMessage = context.getString(R.string.tournament_error_status_update_failed)) }
            }
        }
    }

    fun uploadLogo(bytes: ByteArray, fileExtension: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingLogo = true, errorMessage = null) }
            try {
                tournamentRepository.uploadLogo(tournamentId, bytes, fileExtension)
                refresh()
            } catch (t: Throwable) {
                _uiState.update { it.copy(errorMessage = context.getString(R.string.tournament_error_logo_upload_failed)) }
            } finally {
                _uiState.update { it.copy(isUploadingLogo = false) }
            }
        }
    }
}

class TournamentDetailViewModelFactory(
    private val appContext: Context,
    private val tournamentId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TournamentDetailViewModel(appContext, tournamentId, TournamentRepository(appContext)) as T
    }
}
