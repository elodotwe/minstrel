package com.jacobarau.minstrel.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player as ExoPlayerListener
import androidx.media3.exoplayer.ExoPlayer
import com.jacobarau.minstrel.data.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExoPlayerPlayer @Inject constructor(@ApplicationContext context: Context) : Player {
    private val exoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .build()

    private val playerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    override val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Stopped)
    override val playbackState = _playbackState.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    override val currentTrack = _currentTrack.asStateFlow()

    private val _trackProgressMillis = MutableStateFlow(0L)
    override val trackProgressMillis: StateFlow<Long> = _trackProgressMillis.asStateFlow()

    private val _trackDurationMillis = MutableStateFlow(0L)
    override val trackDurationMillis: StateFlow<Long> = _trackDurationMillis.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    override val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    init {
        exoPlayer.addListener(object : ExoPlayerListener.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updatePlaybackState()
                _trackDurationMillis.value = exoPlayer.duration
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == ExoPlayer.STATE_READY) {
                    _trackDurationMillis.value = exoPlayer.duration
                }
                if (playbackState == ExoPlayerListener.STATE_ENDED) {
                    _playbackState.value = PlaybackState.Stopped
                }
                updatePlaybackState()
            }
        })

        playerScope.launch {
            while (true) {
                if (exoPlayer.isPlaying) {
                    _trackProgressMillis.value = exoPlayer.currentPosition
                }
                delay(1000)
            }
        }
    }

    private fun updatePlaybackState() {
        val currentTrack = _tracks.value.getOrNull(exoPlayer.currentMediaItemIndex)
        _currentTrack.value = currentTrack

        _playbackState.value = when {
            exoPlayer.isPlaying -> currentTrack?.let { PlaybackState.Playing(it) } ?: PlaybackState.Stopped
            exoPlayer.playbackState == ExoPlayer.STATE_IDLE || exoPlayer.playbackState == ExoPlayer.STATE_ENDED -> PlaybackState.Stopped
            else -> currentTrack?.let { PlaybackState.Paused(it) } ?: PlaybackState.Stopped
        }
    }

    override fun play(tracks: List<Track>, track: Track, playWhenReady: Boolean) {
        this._tracks.value = tracks
        val mediaItems = tracks.map { MediaItem.fromUri(it.uri) }
        val selectedIndex = tracks.indexOf(track)
        exoPlayer.setMediaItems(mediaItems, selectedIndex, 0)
        exoPlayer.playWhenReady = playWhenReady
        exoPlayer.prepare()
    }

    override fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun unpause() {
        exoPlayer.play()
    }

    override fun skipToNext() {
        exoPlayer.seekToNextMediaItem()
    }

    override fun skipToPrevious() {
        exoPlayer.seekToPreviousMediaItem()
    }

    override fun skipToTrack(index: Int) {
        exoPlayer.seekTo(index, 0)
    }

    override fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    override fun stop() {
        exoPlayer.stop()
        _playbackState.value = PlaybackState.Stopped
    }



    override fun setShuffleModeEnabled(enabled: Boolean) {
        exoPlayer.shuffleModeEnabled = enabled
        _shuffleModeEnabled.value = enabled
    }

    override fun release() {
        playerScope.cancel()
        exoPlayer.release()
    }
}
