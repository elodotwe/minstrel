package com.jacobarau.minstrel.player

import com.jacobarau.minstrel.data.Track

interface Player {
    fun play(tracks: List<Track>, selectedTrack: Track)
}
