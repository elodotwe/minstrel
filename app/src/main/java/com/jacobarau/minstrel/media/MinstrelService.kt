package com.jacobarau.minstrel.media

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
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
import com.jacobarau.minstrel.data.TrackListState
import com.jacobarau.minstrel.player.Player
import com.jacobarau.minstrel.player.PlaybackState
import com.jacobarau.minstrel.repository.TrackRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MY_MEDIA_ROOT_ID = "media_root_id"
private const val NOTIFICATION_ID = 1

@AndroidEntryPoint
class MinstrelService : MediaBrowserServiceCompat() {

    private val tag = this.javaClass.simpleName

    @Inject
    lateinit var player: Player

    @Inject
    lateinit var trackRepository: TrackRepository

    private lateinit var mediaSession: MediaSessionCompat
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var isStarted = false

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                player.pause()
            }
        }
    }
    private val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)


    private val sessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            Log.d(tag, "onPlay")
            player.togglePlayPause()
            if (!isStarted) {
                // MediaButtonReceiver binds us when handling a Bluetooth button press, but we are only
                // bound at that point. We need to become started too if we're to keep a notification
                // or a media session.
                startForegroundService(Intent(applicationContext, MinstrelService::class.java))
            }
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

        override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
            Log.d(tag, "onPlayFromMediaId $mediaId")
            if (!isStarted) {
                // If invoked from Android Auto we may only be bound. Ensure we're started.
                startForegroundService(Intent(applicationContext, MinstrelService::class.java))
            }
            serviceScope.launch {
                val trackListState = trackRepository.getTracks().first { it is TrackListState.Success }
                if (trackListState is TrackListState.Success) {
                    val tracks = trackListState.tracks
                    var trackIndex = tracks.indexOfFirst { it.uri.toString() == mediaId }
                    if (trackIndex == -1) trackIndex = 0;
                    player.play(tracks, tracks[trackIndex])
                }
            }
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            Log.d(tag, "onPlayFromSearch query: $query")
            if (!isStarted) {
                startForegroundService(Intent(applicationContext, MinstrelService::class.java))
            }

            serviceScope.launch {
                val trackListState = trackRepository.getTracks().first { it is TrackListState.Success }
                if (trackListState is TrackListState.Success) {
                    val allTracks = trackListState.tracks
                    if (allTracks.isEmpty()) return@launch

                    val trackToPlay = if (query.isNullOrBlank()) {
                        allTracks.first()
                    } else {
                        allTracks.firstOrNull {
                            it.title?.contains(query, ignoreCase = true) == true ||
                                    it.artist?.contains(query, ignoreCase = true) == true ||
                                    it.album?.contains(query, ignoreCase = true) == true ||
                                    it.filename.contains(query, ignoreCase = true) == true
                        }
                    }

                    if (trackToPlay != null) {
                        player.play(allTracks, trackToPlay)
                    } else {
                        Log.w(tag, "No results for query: $query")
                    }
                }
            }
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

        registerReceiver(becomingNoisyReceiver, intentFilter)

        player.tracks
            .onEach { tracks ->
                Log.d(tag, "Tracks changed. Updating queue.")
                val queue =
                    tracks.mapIndexed { index, track -> track.toQueueItem(index.toLong()) }
                        .take(100)
                mediaSession.setQueue(queue)
                mediaSession.setQueueTitle("Up Next")
                notifyChildrenChanged(MY_MEDIA_ROOT_ID)
            }
            .launchIn(serviceScope)

        combine(player.currentTrack, player.trackDurationMillis) { track, duration ->
            Pair(track, duration)
        }
            .distinctUntilChanged()
            .onEach { (track, duration) ->
                Log.d(tag, "Track or duration changed. Updating metadata.")
                val metadataBuilder = MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track?.title)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track?.artist)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track?.album)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                mediaSession.setMetadata(metadataBuilder.build())
            }
            .launchIn(serviceScope)

        player.playbackState
            .onEach { state ->
                Log.d(tag, "Playback state changed. Updating notification.")
                updateNotification(state)
            }
            .launchIn(serviceScope)

        combine(
            player.playbackState,
            player.currentTrack,
            player.tracks,
            player.trackProgressMillis
        ) { state, track, tracks, progress ->
            val trackIndex = tracks.indexOf(track)
            var actions = PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
            if (trackIndex > 0) {
                actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            }
            if (trackIndex < tracks.size - 1) {
                actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            }

            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(
                    state.toPlaybackStateCompat(),
                    progress,
                    1.0f
                )
                .setActiveQueueItemId(trackIndex.toLong())
                .build()
        }
            .onEach { playbackState ->
                mediaSession.setPlaybackState(playbackState)
            }
            .launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "onStartCommand $intent $flags $startId")
        isStarted = true
        return super.onStartCommand(intent, flags, startId)
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
        isStarted = false
        super.onDestroy()
        serviceScope.cancel()
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
        if (MY_MEDIA_ROOT_ID != parentId) {
            result.sendResult(null)
            return
        }

        result.detach()

        serviceScope.launch {
            val trackListState = trackRepository.getTracks().first { it is TrackListState.Success }
            if (trackListState is TrackListState.Success) {
                val mediaItems = trackListState.tracks.map { track ->
                    val description = MediaDescriptionCompat.Builder()
                        .setMediaId(track.uri.toString())
                        .setTitle(track.title)
                        .setSubtitle(track.artist)
                        .build()
                    MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
                }.take(100)
                result.sendResult(mediaItems)
            }
        }
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
        .setTitle(title)
        .setSubtitle(artist)
        .build()
    return MediaSessionCompat.QueueItem(description, id)
}
