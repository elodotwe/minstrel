package com.jacobarau.minstrel.player

import com.jacobarau.minstrel.data.Track

sealed class PlaybackState {
    object Stopped : PlaybackState()
    data class Playing(val track: Track) : PlaybackState()
    data class Paused(val track: Track) : PlaybackState()
}
