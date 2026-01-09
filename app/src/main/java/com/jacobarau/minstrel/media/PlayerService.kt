package com.jacobarau.minstrel.media

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.jacobarau.minstrel.MainActivity
import com.jacobarau.minstrel.NOTIFICATION_CHANNEL_ID
import com.jacobarau.minstrel.R
import com.jacobarau.minstrel.data.PlaybackStateRepository
import com.jacobarau.minstrel.data.TrackListState
import com.jacobarau.minstrel.player.PlaybackState
import com.jacobarau.minstrel.player.Player
import com.jacobarau.minstrel.repository.TrackRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val NOTIFICATION_ID = 1
private const val CUSTOM_ACTION_SHUFFLE = "com.jacobarau.minstrel.media.SHUFFLE"
private const val CUSTOM_ACTION_STOP = "com.jacobarau.minstrel.media.STOP"

@AndroidEntryPoint
class PlayerService : LifecycleService() {
    private val tag = this.javaClass.simpleName

    @Inject
    lateinit var player: Player

    @Inject
    lateinit var trackRepository: TrackRepository

    @Inject
    lateinit var playbackStateRepository: PlaybackStateRepository

    private lateinit var mediaSession: MediaSessionCompat
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isStarted = false
    private val currentSearchQueryFlow = MutableStateFlow<String?>(null)
    private var progressSaverJob: Job? = null

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
            player.unpause()
            if (!isStarted) {
                startForegroundService(Intent(applicationContext, PlayerService::class.java))
            }
        }

        override fun onPause() {
            Log.d(tag, "onPause")
            player.pause()
        }

        override fun onStop() {
            Log.d(tag, "onStop")
            player.stop()
            stopSelf()
        }

        override fun onSkipToNext() {
            Log.d(tag, "onSkipToNext")
            player.skipToNext()
            player.unpause()
        }

        override fun onSkipToPrevious() {
            Log.d(tag, "onSkipToPrevious")
            player.skipToPrevious()
            player.unpause()
        }

        override fun onSkipToQueueItem(id: Long) {
            Log.d(tag, "onSkipToQueueItem $id")
            player.skipToTrack(id.toInt())
            player.unpause()
        }

        override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
            Log.d(tag, "onPlayFromMediaId $mediaId, extras: $extras")
            currentSearchQueryFlow.value = null
            if (!isStarted) {
                startForegroundService(Intent(applicationContext, PlayerService::class.java))
            }
            serviceScope.launch {
                val trackListState = trackRepository.getTracks().first { it is TrackListState.Success }
                if (trackListState is TrackListState.Success) {
                    val tracks = trackListState.tracks
                    var trackIndex = tracks.indexOfFirst { it.uri.toString() == mediaId }
                    if (trackIndex == -1) trackIndex = 0
                    player.play(tracks, tracks[trackIndex])
                }
            }
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            Log.d(tag, "onPlayFromSearch query: $query")
            currentSearchQueryFlow.value = query
            if (!isStarted) {
                startForegroundService(Intent(applicationContext, PlayerService::class.java))
            }

            serviceScope.launch {
                val trackListState = trackRepository.getTracks().first { it is TrackListState.Success }
                if (trackListState is TrackListState.Success) {
                    val allTracks = trackListState.tracks
                    if (allTracks.isEmpty()) return@launch

                    val tracksToPlay = if (query.isNullOrBlank()) {
                        allTracks
                    } else {
                        allTracks.filter {
                            it.title?.contains(query, ignoreCase = true) == true ||
                                    it.artist?.contains(query, ignoreCase = true) == true ||
                                    it.album?.contains(query, ignoreCase = true) == true ||
                                    it.filename.contains(query, ignoreCase = true)
                        }
                    }

                    if (tracksToPlay.isNotEmpty()) {
                        player.play(tracksToPlay, tracksToPlay.first())
                    } else {
                        Log.w(tag, "No results for query: $query")
                    }
                }
            }
        }

        override fun onSeekTo(pos: Long) {
            player.seekTo(pos)
        }

        override fun onCustomAction(action: String, extras: Bundle?) {
            when (action) {
                CUSTOM_ACTION_SHUFFLE -> {
                    val currentShuffleMode = player.shuffleModeEnabled.value
                    player.setShuffleModeEnabled(!currentShuffleMode)
                }
                CUSTOM_ACTION_STOP -> {
                    onStop()
                }
                else -> Log.w(tag, "Unhandled custom action: $action")
            }
        }
    }

    inner class PlayerBinder : Binder() {
        fun getMediaSessionToken(): MediaSessionCompat.Token {
            return mediaSession.sessionToken
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return PlayerBinder()
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(tag, "onCreate")

        mediaSession = MediaSessionCompat(this, "Minstrel-Player").apply {
            setCallback(sessionCallback)
            isActive = true
        }

        registerReceiver(becomingNoisyReceiver, intentFilter)

        serviceScope.launch {
            Log.i(tag, "Attempting to restore playback state...")
            val storedPlaybackState = playbackStateRepository.storedPlaybackState.first()
            Log.i(tag, "Restored state: $storedPlaybackState")
            player.setShuffleModeEnabled(storedPlaybackState.isShuffleEnabled)
            if (storedPlaybackState.trackUri != null) {
                val trackListState = trackRepository.getTracks().first { it is TrackListState.Success }
                if (trackListState is TrackListState.Success) {
                    val allTracks = trackListState.tracks
                    val query = storedPlaybackState.searchQuery
                    currentSearchQueryFlow.value = query
                    val tracksToPlay = if (query.isNullOrBlank()) {
                        allTracks
                    } else {
                        allTracks.filter {
                            it.title?.contains(query, ignoreCase = true) == true ||
                                    it.artist?.contains(query, ignoreCase = true) == true ||
                                    it.album?.contains(query, ignoreCase = true) == true ||
                                    it.filename.contains(query, ignoreCase = true)
                        }
                    }

                    val track = tracksToPlay.firstOrNull { it.uri.toString() == storedPlaybackState.trackUri }
                    if (track != null) {
                        Log.i(tag, "Track found. Setting player state.")
                        player.play(tracksToPlay, track, playWhenReady = false)
                        player.seekTo(storedPlaybackState.position)
                    } else {
                        Log.w(tag, "Track URI found in stored state, but track not found in repository.")
                    }
                }
            } else {
                Log.i(tag, "No track URI in stored state. Nothing to restore.")
            }
        }
        startPlaybackStateSaver()


        combine(player.tracks, player.currentTrack) { tracks, currentTrack ->
            Pair(tracks, currentTrack)
        }
            .onEach { (tracks, currentTrack) ->
                Log.d(tag, "Tracks or current track changed. Updating queue.")

                val allQueueItems =
                    tracks.mapIndexed { index, track -> track.toQueueItem(index.toLong()) }


                val maxQueueLength = 100
                val queue = if (allQueueItems.size <= maxQueueLength) {
                    // If the list is small enough, just use it as is.
                    allQueueItems
                } else {
                    val currentIndex = tracks.indexOf(currentTrack)

                    // If the current track isn't found or is early in the list,
                    // we can safely start the sublist from its index.
                    // We use coerceAtLeast(0) to treat the "not found" case (-1) as the start of the list.
                    val startIndex = currentIndex.coerceAtLeast(0)

                    // Determine the end index. It's either the start + 100 or the end of the list.
                    val endIndex = (startIndex + maxQueueLength).coerceAtMost(allQueueItems.size)

                    // If our calculated window is smaller than 100 (because we're at the end),
                    // we should instead just take the last 100 items.
                    if (endIndex - startIndex < maxQueueLength) {
                        allQueueItems.takeLast(maxQueueLength)
                    } else {
                        allQueueItems.subList(startIndex, endIndex)
                    }
                }

                // Limit to 100 tracks. Android Auto will fail to show the queue if we send
                // too many items.
                mediaSession.setQueue(queue)
                mediaSession.setQueueTitle("Up Next")
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

        player.shuffleModeEnabled.onEach { enabled ->
            val shuffleMode = if (enabled) {
                PlaybackStateCompat.SHUFFLE_MODE_ALL
            } else {
                PlaybackStateCompat.SHUFFLE_MODE_NONE
            }
            mediaSession.setShuffleMode(shuffleMode)
        }.launchIn(serviceScope)

        combine(
            player.playbackState,
            player.currentTrack,
            player.tracks,
            player.trackProgressMillis,
            player.shuffleModeEnabled
        ) { state, track, tracks, progress, shuffleEnabled ->
            val trackIndex = tracks.indexOf(track)
            var actions = PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM

            if (trackIndex > 0) {
                actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            }
            if (trackIndex < tracks.size - 1) {
                actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            }

            val shuffleAction = if (shuffleEnabled) {
                PlaybackStateCompat.CustomAction.Builder(
                    CUSTOM_ACTION_SHUFFLE, "Shuffle On", R.drawable.ic_shuffle_on
                ).build()
            } else {
                PlaybackStateCompat.CustomAction.Builder(
                    CUSTOM_ACTION_SHUFFLE, "Shuffle Off", R.drawable.ic_shuffle
                ).build()
            }

            val stopAction = PlaybackStateCompat.CustomAction.Builder(
                CUSTOM_ACTION_STOP, "Stop", R.drawable.ic_stop
            ).build()

            PlaybackStateCompat.Builder()
                .setActions(actions)
                .addCustomAction(shuffleAction)
                .addCustomAction(stopAction)
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

    private fun savePlaybackState() {
        val track = player.currentTrack.value
        val position = player.trackProgressMillis.value
        val isShuffleEnabled = player.shuffleModeEnabled.value
        val query = currentSearchQueryFlow.value
        Log.i(
            tag, "Saving playback state. Track: ${track?.uri}, " +
                "Position: $position, Query: $query, Shuffle: $isShuffleEnabled"
        )
        serviceScope.launch {
            playbackStateRepository.savePlaybackState(
                track?.uri.toString(),
                position,
                query,
                isShuffleEnabled
            )
        }
    }

    private fun startPlaybackStateSaver() {
        // Save state whenever track, shuffle mode, or search query changes, coalescing rapid changes.
        combine(
            player.currentTrack,
            player.shuffleModeEnabled,
            currentSearchQueryFlow
        ) { _, _, _ -> }
            .debounce(250)
            .onEach {
                savePlaybackState()
            }.launchIn(serviceScope)

        // Also save state on pause/stop, and periodically on play
        player.playbackState.onEach { state ->
            progressSaverJob?.cancel()
            when (state) {
                is PlaybackState.Playing -> {
                    progressSaverJob = serviceScope.launch {
                        while (true) {
                            savePlaybackState()
                            delay(5000)
                        }
                    }
                }
                is PlaybackState.Paused, is PlaybackState.Stopped -> {
                    savePlaybackState()
                }
            }
        }.launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(tag, "onStartCommand $intent $flags $startId")
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        isStarted = true
        return START_STICKY
    }

    private fun updateNotification(state: PlaybackState) {
        Log.d(tag, "updateNotification $state")
        when (state) {
            is PlaybackState.Playing, is PlaybackState.Paused -> {
                val notification = createNotification(state)
                startForeground(NOTIFICATION_ID, notification)
            }
            is PlaybackState.Stopped -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }
    }

    private fun createNotification(state: PlaybackState): Notification {
        val controller = mediaSession.controller
        val mediaMetadata = controller.metadata
        val description = mediaMetadata?.description

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
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_PAUSE
                )
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_play_arrow,
                "Play",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_PLAY
                )
            )
        }

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(description?.title ?: "Loading")
            .setContentText(description?.subtitle ?: "Loading")
            .setSubText(description?.description ?: "")
            .setLargeIcon(description?.iconBitmap)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_skip_previous,
                    "Previous",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
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
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    )
                )
            )
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        return builder.build()
    }

    override fun onDestroy() {
        Log.i(tag, "onDestroy")
        isStarted = false
        unregisterReceiver(becomingNoisyReceiver)
        super.onDestroy()
        serviceScope.cancel()
        mediaSession.release()
    }
}
