package com.jacobarau.minstrel.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.jacobarau.minstrel.data.Track

class ExoPlayerPlayer(context: Context) : Player {
    private val exoPlayer = ExoPlayer.Builder(context).build()

    override fun play(tracks: List<Track>, selectedTrack: Track) {
        val mediaItems = tracks.map { MediaItem.fromUri(it.uri) }
        val selectedIndex = tracks.indexOf(selectedTrack)
        exoPlayer.setMediaItems(mediaItems, selectedIndex, 0)
        exoPlayer.prepare()
        exoPlayer.play()
    }
}
