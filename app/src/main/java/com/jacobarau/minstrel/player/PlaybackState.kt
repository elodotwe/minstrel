package com.jacobarau.minstrel.player

import com.jacobarau.minstrel.data.Track

sealed class PlaybackState {
    object Stopped : PlaybackState()
    data class Playing(
        val isPaused: Boolean,
        val tracks: List<Track>,
        val currentTrackIndex: Int,
        val trackProgressMillis: Long,
        val trackDurationMillis: Long
    ) : PlaybackState()
}
