package com.jacobarau.minstrel.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player as ExoPlayerListener
import androidx.media3.exoplayer.ExoPlayer
import com.jacobarau.minstrel.data.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExoPlayerPlayer(context: Context) : Player {
    private val exoPlayer = ExoPlayer.Builder(context).build()
    private var tracks: List<Track> = emptyList()

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Stopped)
    override val playbackState = _playbackState.asStateFlow()

    init {
        exoPlayer.addListener(object : ExoPlayerListener.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updatePlaybackState()
            }
        })
    }

    private fun updatePlaybackState() {
        val currentTrack = tracks.getOrNull(exoPlayer.currentMediaItemIndex) ?: return

        _playbackState.value = when {
            exoPlayer.isPlaying -> PlaybackState.Playing(currentTrack)
            exoPlayer.playbackState == ExoPlayer.STATE_IDLE || exoPlayer.playbackState == ExoPlayer.STATE_ENDED -> PlaybackState.Stopped
            else -> PlaybackState.Paused(currentTrack)
        }
    }

    override fun play(tracks: List<Track>, selectedTrack: Track) {
        this.tracks = tracks
        val mediaItems = tracks.map { MediaItem.fromUri(it.uri) }
        val selectedIndex = tracks.indexOf(selectedTrack)
        exoPlayer.setMediaItems(mediaItems, selectedIndex, 0)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    override fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    override fun stop() {
        exoPlayer.stop()
        _playbackState.value = PlaybackState.Stopped
    }
}
