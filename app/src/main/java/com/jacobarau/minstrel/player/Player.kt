package com.jacobarau.minstrel.player

import com.jacobarau.minstrel.data.Track
import kotlinx.coroutines.flow.Flow

sealed class PlaybackState {
    data class Playing(val track: Track) : PlaybackState()
    data class Paused(val track: Track) : PlaybackState()
    object Stopped : PlaybackState()
}

interface Player {
    fun play(tracks: List<Track>, selectedTrack: Track)
    fun togglePlayPause()
    fun stop()
    val playbackState: Flow<PlaybackState>
}
