package com.gentech.picklepro.player.qr

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gentech.picklepro.data.local.entity.ProfileEntity
import com.gentech.picklepro.data.repository.AuthRepository
import com.gentech.picklepro.data.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class QrViewModel(
    playerId: String,
    profileRepository: ProfileRepository,
) : ViewModel() {

    val profile: StateFlow<ProfileEntity?> = profileRepository.observeCached(playerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

class QrViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val playerId = AuthRepository(appContext).currentUserId
            ?: error("QrViewModel requires a signed-in user")
        @Suppress("UNCHECKED_CAST")
        return QrViewModel(playerId, ProfileRepository(appContext)) as T
    }
}
