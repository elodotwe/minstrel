package com.jacobarau.minstrel.player

import com.jacobarau.minstrel.data.Track
import kotlinx.coroutines.flow.StateFlow

interface Player {
    val playbackState: StateFlow<PlaybackState>
    val currentTrack: StateFlow<Track?>
    val tracks: StateFlow<List<Track>>
    val trackProgressMillis: StateFlow<Long>
    val trackDurationMillis: StateFlow<Long>
    fun play(tracks: List<Track>, track: Track)
    fun togglePlayPause()
    fun pause()
    fun unpause()
    fun skipToNext()
    fun skipToPrevious()
    fun skipToTrack(index: Int)
    fun seekTo(position: Long)
    fun stop()
    fun release()
}
