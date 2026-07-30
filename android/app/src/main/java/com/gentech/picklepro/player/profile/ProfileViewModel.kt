package com.gentech.picklepro.player.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.data.local.entity.ProfileEntity
import com.gentech.picklepro.data.local.entity.RatingEntity
import com.gentech.picklepro.data.remote.dto.RatingHistoryDto
import com.gentech.picklepro.data.repository.AuthRepository
import com.gentech.picklepro.data.repository.MatchHistoryItem
import com.gentech.picklepro.data.repository.MatchHistoryRepository
import com.gentech.picklepro.data.repository.ProfileRepository
import com.gentech.picklepro.data.repository.RatingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: ProfileEntity? = null,
    val ratings: List<RatingEntity> = emptyList(),
    val selectedEventType: String = "singles",
    val ratingHistory: List<RatingHistoryDto> = emptyList(),
    val matchHistory: List<MatchHistoryItem> = emptyList(),
    val isLoadingMatchHistory: Boolean = true,
    val isRefreshing: Boolean = false,
)

class ProfileViewModel(
    private val playerId: String,
    private val profileRepository: ProfileRepository,
    private val ratingRepository: RatingRepository,
    private val matchHistoryRepository: MatchHistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                profileRepository.observeCached(playerId),
                ratingRepository.observeCached(playerId),
            ) { profile, ratings -> profile to ratings }
                .collect { (profile, ratings) ->
                    _uiState.update { it.copy(profile = profile, ratings = ratings) }
                }
        }
        refresh()
        loadRatingHistory("singles")
        loadMatchHistory()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            runCatching {
                profileRepository.refresh(playerId)
                ratingRepository.refresh(playerId)
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun selectEventType(eventType: String) {
        _uiState.update { it.copy(selectedEventType = eventType) }
        loadRatingHistory(eventType)
    }

    private fun loadRatingHistory(eventType: String) {
        viewModelScope.launch {
            val history = runCatching { ratingRepository.fetchHistory(playerId, eventType) }
                .getOrDefault(emptyList())
            _uiState.update { it.copy(ratingHistory = history) }
        }
    }

    private fun loadMatchHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMatchHistory = true) }
            val matches = runCatching { matchHistoryRepository.fetchRecent(playerId) }
                .getOrDefault(emptyList())
            _uiState.update { it.copy(matchHistory = matches, isLoadingMatchHistory = false) }
        }
    }
}

class ProfileViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val playerId = AuthRepository(appContext).currentUserId
            ?: error("ProfileViewModel requires a signed-in user")
        @Suppress("UNCHECKED_CAST")
        return ProfileViewModel(
            playerId = playerId,
            profileRepository = ProfileRepository(appContext),
            ratingRepository = RatingRepository(appContext),
            matchHistoryRepository = MatchHistoryRepository(appContext),
        ) as T
    }
}
