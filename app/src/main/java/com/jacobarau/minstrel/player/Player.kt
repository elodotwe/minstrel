package com.jacobarau.minstrel.player

import com.jacobarau.minstrel.data.Track
import kotlinx.coroutines.flow.StateFlow

interface Player {
    val playbackState: StateFlow<PlaybackState>
    val currentTrack: StateFlow<Track?>
    val tracks: StateFlow<List<Track>>
    fun play(tracks: List<Track>, track: Track)
    fun togglePlayPause()
    fun pause()
    fun skipToNext()
    fun skipToPrevious()
    fun skipToTrack(index: Int)
    fun stop()
    fun release()
}
