package com.jacobarau.minstrel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobarau.minstrel.data.Track
import com.jacobarau.minstrel.data.TrackListState
import com.jacobarau.minstrel.player.Player
import com.jacobarau.minstrel.player.PlaybackState
import com.jacobarau.minstrel.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val player: Player
) : ViewModel() {

    val tracks: StateFlow<TrackListState> = trackRepository.getTracks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrackListState.Loading
        )

    val playbackState: StateFlow<PlaybackState> = player.playbackState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlaybackState.Stopped
        )

    fun onTrackSelected(track: Track) {
        val trackListState = tracks.value
        if (trackListState is TrackListState.Success) {
            player.play(trackListState.tracks, track)
        }
    }

    fun onPlayPauseClicked() {
        player.togglePlayPause()
    }
}
