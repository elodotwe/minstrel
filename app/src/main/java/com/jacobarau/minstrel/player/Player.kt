package com.jacobarau.minstrel.player

import com.jacobarau.minstrel.data.Track
import kotlinx.coroutines.flow.StateFlow

interface Player {
    val playbackState: StateFlow<PlaybackState>
    fun play(tracks: List<Track>, track: Track)
    fun togglePlayPause()
    fun stop()
    fun release()
}
