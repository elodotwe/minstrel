package com.jacobarau.minstrel.player

import com.jacobarau.minstrel.data.Track
import kotlinx.coroutines.flow.StateFlow

interface Player {
    val playbackState: StateFlow<PlaybackState>
    val shuffleModeEnabled: StateFlow<Boolean>
    fun play(tracks: List<Track>, track: Track, playWhenReady: Boolean = true)
    fun togglePlayPause()
    fun pause()
    fun unpause()
    fun skipToNext()
    fun skipToPrevious()
    fun skipToTrack(index: Int)
    fun seekTo(position: Long)
    fun stop()
    fun setShuffleModeEnabled(enabled: Boolean)
    fun release()
}
