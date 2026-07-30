package com.gentech.picklepro.organizer.registration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.data.remote.dto.DivisionDto
import com.gentech.picklepro.data.remote.dto.RegistrationWithProfileDto
import com.gentech.picklepro.data.repository.DivisionRepository
import com.gentech.picklepro.data.repository.RegistrationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistrationListUiState(
    val division: DivisionDto? = null,
    val registrations: List<RegistrationWithProfileDto> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
) {
    val filtered: List<RegistrationWithProfileDto>
        get() = if (searchQuery.isBlank()) {
            registrations
        } else {
            registrations.filter { it.profiles.name.contains(searchQuery, ignoreCase = true) }
        }
}

/** Registration list per division (spec §5.4): search, check-in toggle, unregister. */
class RegistrationListViewModel(
    private val divisionId: String,
    private val divisionRepository: DivisionRepository,
    private val registrationRepository: RegistrationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationListUiState())
    val uiState: StateFlow<RegistrationListUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val division = runCatching { divisionRepository.get(divisionId) }.getOrNull()
            val registrations = runCatching { registrationRepository.listForDivision(divisionId) }.getOrDefault(emptyList())
            _uiState.update { it.copy(division = division, registrations = registrations, isLoading = false) }
        }
    }

    fun onSearchChange(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun toggleCheckIn(registrationId: String, checkedIn: Boolean) {
        viewModelScope.launch {
            runCatching { registrationRepository.setCheckedIn(registrationId, checkedIn) }
            refresh()
        }
    }

    fun unregister(registrationId: String) {
        viewModelScope.launch {
            runCatching { registrationRepository.unregister(registrationId) }
            refresh()
        }
    }
}

class RegistrationListViewModelFactory(
    private val appContext: Context,
    private val divisionId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RegistrationListViewModel(
            divisionId = divisionId,
            divisionRepository = DivisionRepository(appContext),
            registrationRepository = RegistrationRepository(appContext),
        ) as T
    }
}
