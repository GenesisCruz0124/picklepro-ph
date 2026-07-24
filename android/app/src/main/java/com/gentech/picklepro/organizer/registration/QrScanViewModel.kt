package com.gentech.picklepro.organizer.registration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.core.qr.PlayerQrPayload
import com.gentech.picklepro.data.remote.dto.DivisionDto
import com.gentech.picklepro.data.repository.DivisionRepository
import com.gentech.picklepro.data.repository.ProfileRepository
import com.gentech.picklepro.data.repository.RatingRepository
import com.gentech.picklepro.data.repository.RegisterOutcome
import com.gentech.picklepro.data.repository.RegistrationRepository
import com.gentech.picklepro.data.repository.RejectReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ScanOutcomeUi {
    data class Registered(val playerName: String) : ScanOutcomeUi
    data class CheckedIn(val playerName: String) : ScanOutcomeUi
    data class Rejected(val playerName: String, val reason: RejectReason) : ScanOutcomeUi
    data class Error(val message: String) : ScanOutcomeUi
}

data class QrScanUiState(
    val division: DivisionDto? = null,
    val isScanning: Boolean = true,
    val isProcessing: Boolean = false,
    val outcome: ScanOutcomeUi? = null,
)

class QrScanViewModel(
    private val divisionId: String,
    private val divisionRepository: DivisionRepository,
    private val profileRepository: ProfileRepository,
    private val ratingRepository: RatingRepository,
    private val registrationRepository: RegistrationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrScanUiState())
    val uiState: StateFlow<QrScanUiState> = _uiState

    init {
        viewModelScope.launch {
            val division = runCatching { divisionRepository.get(divisionId) }.getOrNull()
            _uiState.update { it.copy(division = division) }
        }
    }

    fun onBarcodeDetected(raw: String) {
        val state = _uiState.value
        if (!state.isScanning || state.isProcessing) return
        val payload = PlayerQrPayload.decode(raw) ?: return

        _uiState.update { it.copy(isScanning = false, isProcessing = true) }
        viewModelScope.launch {
            try {
                val division = state.division ?: divisionRepository.get(divisionId)
                val profile = profileRepository.findById(payload.playerId)
                if (profile == null) {
                    _uiState.update {
                        it.copy(isProcessing = false, outcome = ScanOutcomeUi.Error("Hindi mahanap ang manlalaro."))
                    }
                    return@launch
                }
                val rating = ratingRepository.getForPlayer(profile.id, division.eventType)
                val effectiveRating = if (profile.duprVerified && profile.duprRating != null) {
                    profile.duprRating
                } else {
                    rating?.effectiveDisplay ?: 2.5
                }
                val outcome = registrationRepository.registerOrCheckIn(
                    divisionId = divisionId,
                    playerId = profile.id,
                    maxRating = division.maxRating,
                    maxSlots = division.maxSlots,
                    effectiveRating = effectiveRating,
                )
                val ui = when (outcome) {
                    is RegisterOutcome.Registered -> ScanOutcomeUi.Registered(profile.name)
                    is RegisterOutcome.CheckedIn -> ScanOutcomeUi.CheckedIn(profile.name)
                    is RegisterOutcome.Rejected -> ScanOutcomeUi.Rejected(profile.name, outcome.reason)
                }
                _uiState.update { it.copy(isProcessing = false, outcome = ui) }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(isProcessing = false, outcome = ScanOutcomeUi.Error("May problema. Subukan ulit."))
                }
            }
        }
    }

    fun scanAnother() = _uiState.update { it.copy(isScanning = true, isProcessing = false, outcome = null) }
}

class QrScanViewModelFactory(
    private val appContext: Context,
    private val divisionId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return QrScanViewModel(
            divisionId = divisionId,
            divisionRepository = DivisionRepository(appContext),
            profileRepository = ProfileRepository(appContext),
            ratingRepository = RatingRepository(appContext),
            registrationRepository = RegistrationRepository(appContext),
        ) as T
    }
}
