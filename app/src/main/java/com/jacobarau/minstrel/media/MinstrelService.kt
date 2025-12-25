package com.jacobarau.minstrel.media

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.jacobarau.minstrel.MainActivity
import com.jacobarau.minstrel.NOTIFICATION_CHANNEL_ID
import com.jacobarau.minstrel.R
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
private const val NOTIFICATION_ID = 1

@AndroidEntryPoint
class MinstrelService : MediaBrowserServiceCompat() {

    private val tag = this.javaClass.simpleName

    @Inject
    lateinit var player: Player

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var job: Job

    private val sessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            Log.d(tag, "onPlay")
            player.togglePlayPause()
        }

        override fun onPause() {
            Log.d(tag, "onPause")
            player.togglePlayPause()
        }

        override fun onStop() {
            Log.d(tag, "onStop")
            player.stop()
        }

        override fun onSkipToNext() {
            Log.d(tag, "onSkipToNext")
            player.skipToNext()
        }

        override fun onSkipToPrevious() {
            Log.d(tag, "onSkipToPrevious")
            player.skipToPrevious()
        }

        override fun onSkipToQueueItem(id: Long) {
            Log.d(tag, "onSkipToQueueItem $id")
            player.skipToTrack(id.toInt())
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "onCreate")

        mediaSession = MediaSessionCompat(this, "Minstrel").apply {
            setCallback(sessionCallback)
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

        job = CoroutineScope(Dispatchers.Main).launch {
            combine(player.playbackState, player.currentTrack, player.tracks) { state, track, tracks ->
                Log.d(tag, "combine $state $track sizeof(tracks)=${tracks.size}")
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

                updateNotification(state)
            }.collect {}
        }
    }

    private fun updateNotification(state: PlaybackState) {
        Log.d(tag, "updateNotification $state")
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (state) {
            is PlaybackState.Playing -> {
                val notification = createNotification(state)
                startForeground(NOTIFICATION_ID, notification)
            }
            is PlaybackState.Paused -> {
                stopForeground(false)
                val notification = createNotification(state)
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
            is PlaybackState.Stopped -> {
                stopForeground(true)
            }
        }
    }

    private fun createNotification(state: PlaybackState): Notification {
        val controller = mediaSession.controller
        val mediaMetadata = controller.metadata
        val description = mediaMetadata.description

        val activityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (state is PlaybackState.Playing) {
            NotificationCompat.Action(
                R.drawable.ic_pause,
                "Pause",
                androidx.media.session.MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_PAUSE
                )
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_play_arrow,
                "Play",
                androidx.media.session.MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_PLAY
                )
            )
        }

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(description.title)
            .setContentText(description.subtitle)
            .setSubText(description.description)
            .setLargeIcon(description.iconBitmap)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_skip_previous,
                    "Previous",
                    androidx.media.session.MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    )
                )
            )
            .addAction(playPauseAction)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_skip_next,
                    "Next",
                    androidx.media.session.MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    )
                )
            )
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        return builder.build()
    }


    override fun onDestroy() {
        Log.d(tag, "onDestroy")
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
