package com.jacobarau.minstrel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobarau.minstrel.data.Track
import com.jacobarau.minstrel.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TrackViewModel @Inject constructor(
    trackRepository: TrackRepository
) : ViewModel() {

    val tracks: StateFlow<List<Track>> = trackRepository.getTracks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
