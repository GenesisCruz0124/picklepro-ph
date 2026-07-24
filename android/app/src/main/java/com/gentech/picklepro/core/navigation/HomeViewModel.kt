package com.gentech.picklepro.core.navigation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.data.repository.AuthRepository
import com.gentech.picklepro.data.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the Organizer bottom-nav tab's visibility off the signed-in
 * profile's cached role (spec §8: single APK, role-based UI). The tab
 * appears reactively once [ProfileRepository] refreshes after a successful
 * activation-code redemption.
 */
class HomeViewModel(
    userId: String,
    profileRepository: ProfileRepository,
) : ViewModel() {

    val isOrganizer: StateFlow<Boolean> = profileRepository.observeCached(userId)
        .map { it?.role == "organizer" || it?.role == "admin" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch { runCatching { profileRepository.refresh(userId) } }
    }
}

class HomeViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val userId = AuthRepository(appContext).currentUserId
            ?: error("HomeViewModel requires a signed-in user")
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(userId, ProfileRepository(appContext)) as T
    }
}
