package com.jacobarau.minstrel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobarau.minstrel.data.Track
import com.jacobarau.minstrel.data.TrackListState
import com.jacobarau.minstrel.player.Player
import com.jacobarau.minstrel.player.PlaybackState
import com.jacobarau.minstrel.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val player: Player
) : ViewModel() {

    private val searchQuery = MutableStateFlow<String?>(null)

    val tracks: StateFlow<TrackListState> = searchQuery.flatMapLatest { query ->
        trackRepository.getTracks(query)
    }.stateIn(
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

    val isPreviousEnabled: StateFlow<Boolean> = combine(tracks, playbackState) { trackListState, playbackState ->
        if (trackListState !is TrackListState.Success || playbackState is PlaybackState.Stopped) {
            return@combine false
        }
        val currentTrack = when (playbackState) {
            is PlaybackState.Playing -> playbackState.track
            is PlaybackState.Paused -> playbackState.track
            else -> null
        }
        val currentIndex = trackListState.tracks.indexOf(currentTrack)
        return@combine currentIndex > 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), false)

    val isNextEnabled: StateFlow<Boolean> = combine(tracks, playbackState) { trackListState, playbackState ->
        if (trackListState !is TrackListState.Success || playbackState is PlaybackState.Stopped) {
            return@combine false
        }
        val currentTrack = when (playbackState) {
            is PlaybackState.Playing -> playbackState.track
            is PlaybackState.Paused -> playbackState.track
            else -> null
        }
        val currentIndex = trackListState.tracks.indexOf(currentTrack)
        return@combine currentIndex < trackListState.tracks.size - 1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), false)


    val trackProgressMillis: StateFlow<Long> = player.trackProgressMillis
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    val trackDurationMillis: StateFlow<Long> = player.trackDurationMillis
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    fun onTrackSelected(track: Track, trackListState: TrackListState) {
        if (trackListState is TrackListState.Success) {
            player.play(trackListState.tracks, track)
        }
    }

    fun onPlayPauseClicked() {
        player.togglePlayPause()
    }

    fun onSeek(position: Long) {
        player.seekTo(position)
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onPreviousClicked() {
        val trackListState = tracks.value
        val currentPlaybackState = playbackState.value
        if (trackListState is TrackListState.Success && currentPlaybackState !is PlaybackState.Stopped) {
            val currentTrack = when (currentPlaybackState) {
                is PlaybackState.Playing -> currentPlaybackState.track
                is PlaybackState.Paused -> currentPlaybackState.track
                else -> return
            }
            val currentIndex = trackListState.tracks.indexOf(currentTrack)
            if (currentIndex > 0) {
                val previousTrack = trackListState.tracks[currentIndex - 1]
                player.play(trackListState.tracks, previousTrack)
            }
        }
    }

    fun onNextClicked() {
        val trackListState = tracks.value
        val currentPlaybackState = playbackState.value
        if (trackListState is TrackListState.Success && currentPlaybackState !is PlaybackState.Stopped) {
            val currentTrack = when (currentPlaybackState) {
                is PlaybackState.Playing -> currentPlaybackState.track
                is PlaybackState.Paused -> currentPlaybackState.track
                else -> return
            }
            val currentIndex = trackListState.tracks.indexOf(currentTrack)
            if (currentIndex < trackListState.tracks.size - 1) {
                val nextTrack = trackListState.tracks[currentIndex + 1]
                player.play(trackListState.tracks, nextTrack)
            }
        }
    }
}
