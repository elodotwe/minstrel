package com.jacobarau.minstrel.media

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.jacobarau.minstrel.data.Track
import com.jacobarau.minstrel.player.Player
import com.jacobarau.minstrel.player.PlaybackState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MY_MEDIA_ROOT_ID = "media_root_id"

@AndroidEntryPoint
class MinstrelService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var player: Player

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var job: Job

    private val sessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            player.togglePlayPause()
        }

        override fun onPause() {
            player.togglePlayPause()
        }

        override fun onStop() {
            player.stop()
        }

        override fun onSkipToNext() {
            player.skipToNext()
        }

        override fun onSkipToPrevious() {
            player.skipToPrevious()
        }

        override fun onSkipToQueueItem(id: Long) {
            player.skipToTrack(id.toInt())
        }
    }

    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSessionCompat(this, "Minstrel").apply {
            setCallback(sessionCallback)
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

        job = CoroutineScope(Dispatchers.Main).launch {
            combine(player.playbackState, player.currentTrack, player.tracks) { state, track, tracks ->
                val playbackStateBuilder = PlaybackStateCompat.Builder()
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                                PlaybackStateCompat.ACTION_PAUSE or
                                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                                PlaybackStateCompat.ACTION_STOP or
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                                PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
                    )
                    .setState(
                        state.toPlaybackStateCompat(),
                        0,
                        1.0f
                    )
                    .setActiveQueueItemId(tracks.indexOf(track).toLong())
                mediaSession.setPlaybackState(playbackStateBuilder.build())

                val metadataBuilder = MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track?.filename)

                mediaSession.setMetadata(metadataBuilder.build())

                val queue = tracks.mapIndexed { index, track -> track.toQueueItem(index.toLong()) }
                mediaSession.setQueue(queue)
                mediaSession.setQueueTitle("Up Next")
            }.collect {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        mediaSession.release()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        // For now, allow anyone to connect.
        return BrowserRoot(MY_MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        //  Browsing not supported yet.
        if (MY_MEDIA_ROOT_ID != parentId) {
            result.sendResult(null)
            return
        }
        result.sendResult(emptyList()) // Return empty list for now.
    }
}

private fun PlaybackState.toPlaybackStateCompat(): Int {
    return when (this) {
        is PlaybackState.Playing -> PlaybackStateCompat.STATE_PLAYING
        is PlaybackState.Paused -> PlaybackStateCompat.STATE_PAUSED
        is PlaybackState.Stopped -> PlaybackStateCompat.STATE_STOPPED
    }
}

private fun Track.toQueueItem(id: Long): MediaSessionCompat.QueueItem {
    val description = MediaDescriptionCompat.Builder()
        .setMediaId(uri.toString())
        .setTitle(filename)
        .build()
    return MediaSessionCompat.QueueItem(description, id)
}
