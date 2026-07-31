package com.gentech.picklepro.organizer.certificates

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import com.gentech.picklepro.R
import com.gentech.picklepro.data.remote.dto.DivisionDto
import com.gentech.picklepro.data.remote.dto.TournamentDto
import com.gentech.picklepro.data.repository.CertificateRepository
import com.gentech.picklepro.data.repository.DivisionRepository
import com.gentech.picklepro.data.repository.ProfileRepository
import com.gentech.picklepro.data.repository.RegistrationRepository
import com.gentech.picklepro.data.repository.ResultsRepository
import com.gentech.picklepro.data.repository.TeamRepository
import com.gentech.picklepro.data.repository.TournamentRepository
import com.gentech.picklepro.organizer.bracket.resolveEntrants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

/** One generatable certificate row: podium placings first, then every entrant's participation. */
data class CertificateItem(
    val recipientRef: String,
    val recipientName: String,
    val kind: CertificateKind,
)

data class CertificatesUiState(
    val division: DivisionDto? = null,
    val tournament: TournamentDto? = null,
    val organizerName: String = "",
    val podiumItems: List<CertificateItem> = emptyList(),
    val participationItems: List<CertificateItem> = emptyList(),
    val isLoading: Boolean = true,
    val generatingRef: String? = null,
    val errorMessage: String? = null,
    /** Set when a PDF is ready; the screen fires the share sheet and clears it. */
    val fileToShare: File? = null,
)

class CertificatesViewModel(
    private val appContext: Context,
    private val divisionId: String,
    private val divisionRepository: DivisionRepository,
    private val tournamentRepository: TournamentRepository,
    private val profileRepository: ProfileRepository,
    private val registrationRepository: RegistrationRepository,
    private val teamRepository: TeamRepository,
    private val resultsRepository: ResultsRepository,
    private val certificateRepository: CertificateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CertificatesUiState())
    val uiState: StateFlow<CertificatesUiState> = _uiState

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val division = divisionRepository.get(divisionId)
                val tournament = tournamentRepository.get(division.tournamentId)
                val organizerName = runCatching { profileRepository.findById(tournament.organizerId)?.name }
                    .getOrNull().orEmpty()
                val registrations = registrationRepository.listForDivision(divisionId)
                val teams = if (division.eventType == "singles") {
                    emptyList()
                } else {
                    teamRepository.listForDivision(divisionId)
                }
                val entrants = resolveEntrants(division, registrations, teams)
                val nameByRef = entrants.associate { it.ref to it.name }
                val results = resultsRepository.deriveResults(division, entrants)

                val podium = buildList {
                    results.championRef?.let {
                        add(CertificateItem(it, nameByRef[it] ?: "?", CertificateKind.CHAMPION))
                    }
                    results.runnerUpRef?.let {
                        add(CertificateItem(it, nameByRef[it] ?: "?", CertificateKind.RUNNER_UP))
                    }
                }
                val participation = entrants.map {
                    CertificateItem(it.ref, it.name, CertificateKind.PARTICIPATION)
                }
                _uiState.update {
                    it.copy(
                        division = division,
                        tournament = tournament,
                        organizerName = organizerName,
                        podiumItems = podium,
                        participationItems = participation,
                        isLoading = false,
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = appContext.getString(R.string.certificates_load_error)) }
            }
        }
    }

    fun generate(item: CertificateItem, kindLabel: String, awardedToLabel: String, organizedByLabel: String) {
        val state = _uiState.value
        val division = state.division ?: return
        val tournament = state.tournament ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(generatingRef = item.recipientRef, errorMessage = null) }
            try {
                val file = CertificatePdfGenerator.generate(
                    appContext,
                    CertificateData(
                        kind = item.kind,
                        kindLabel = kindLabel,
                        awardedToLabel = awardedToLabel,
                        organizedByLabel = organizedByLabel,
                        recipientName = item.recipientName,
                        tournamentName = tournament.name,
                        divisionName = division.name,
                        organizerName = state.organizerName,
                        date = tournament.startDate ?: LocalDate.now().toString(),
                        logo = tournament.logoUrl?.let { fetchLogo(it) },
                    ),
                )
                // Best-effort record (spec §7 certificates table); sharing never waits on it.
                runCatching {
                    certificateRepository.record(tournament.id, division.id, item.recipientRef, item.kind.dbValue)
                }
                _uiState.update { it.copy(generatingRef = null, fileToShare = file) }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(generatingRef = null, errorMessage = appContext.getString(R.string.certificates_error))
                }
            }
        }
    }

    fun onShared() = _uiState.update { it.copy(fileToShare = null) }

    private suspend fun fetchLogo(url: String): Bitmap? = runCatching {
        val result = ImageLoader(appContext).execute(
            ImageRequest.Builder(appContext)
                .data(url)
                .allowHardware(false) // hardware bitmaps can't be drawn onto a PdfDocument canvas
                .build(),
        )
        (result.drawable as? BitmapDrawable)?.bitmap
    }.getOrNull()
}

class CertificatesViewModelFactory(
    private val appContext: Context,
    private val divisionId: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CertificatesViewModel(
            appContext = appContext,
            divisionId = divisionId,
            divisionRepository = DivisionRepository(appContext),
            tournamentRepository = TournamentRepository(appContext),
            profileRepository = ProfileRepository(appContext),
            registrationRepository = RegistrationRepository(appContext),
            teamRepository = TeamRepository(appContext),
            resultsRepository = ResultsRepository(appContext),
            certificateRepository = CertificateRepository(appContext),
        ) as T
    }
}
