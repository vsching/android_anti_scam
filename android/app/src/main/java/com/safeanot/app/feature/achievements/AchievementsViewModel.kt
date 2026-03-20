/**
 * ViewModel for the Achievements screen.
 * Exposes badge progress and streak data via use cases.
 */
package com.safeanot.app.feature.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeanot.app.domain.model.BadgeProgress
import com.safeanot.app.domain.model.Streak
import com.safeanot.app.domain.usecase.GetBadgesUseCase
import com.safeanot.app.domain.usecase.GetCurrentStreakUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AchievementsUiState(
    val badges: List<BadgeProgress> = emptyList(),
    val streak: Streak = Streak(),
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    getBadgesUseCase: GetBadgesUseCase,
    getCurrentStreakUseCase: GetCurrentStreakUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getBadgesUseCase().collect { badges ->
                _uiState.update { it.copy(badges = badges) }
            }
        }

        viewModelScope.launch {
            getCurrentStreakUseCase().collect { streak ->
                _uiState.update { it.copy(streak = streak) }
            }
        }
    }
}
