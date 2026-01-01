package com.jacobarau.minstrel.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import androidx.media.utils.MediaConstants
import com.jacobarau.minstrel.data.Track
import com.jacobarau.minstrel.data.TrackListState
import com.jacobarau.minstrel.repository.TrackRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import javax.inject.Inject

private const val MY_MEDIA_ROOT_ID = "media_root_id"

@AndroidEntryPoint
class BrowserService : MediaBrowserServiceCompat() {
    private val tag = this.javaClass.simpleName
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Inject
    lateinit var trackRepository: TrackRepository

    private val playerConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(tag, "onServiceConnected")
            if (service is PlayerService.PlayerBinder) {
                sessionToken = service.getMediaSessionToken()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(tag, "onServiceDisconnected")
            // Nothing to do here
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "onCreate")
        val intent = Intent(this, PlayerService::class.java)
        bindService(intent, playerConnection, BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        unbindService(playerConnection)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        val root = BrowserRoot(MY_MEDIA_ROOT_ID, Bundle())
        root.extras.putBoolean(MediaConstants.BROWSER_SERVICE_EXTRAS_KEY_SEARCH_SUPPORTED, true)
        return root
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        if (MY_MEDIA_ROOT_ID != parentId) {
            result.sendResult(null)
            return
        }

        result.detach()

        trackRepository.getTracks().onEach { trackListState ->
            if (trackListState is TrackListState.Success) {
                // Limit to 100 tracks. Android Auto will fail to browse if we send
                // too many items.
                result.sendResult(trackListState.tracks.take(100).map(Track::toMediaItem).toMutableList())
            } else {
                result.sendResult(null)
            }
        }
            .take(1)
            .launchIn(serviceScope)
    }

    override fun onSearch(
        query: String,
        extras: Bundle?,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        Log.d(tag, "onSearch query: $query")
        result.detach()
        trackRepository.getTracks(query).onEach { trackListState ->
            when (trackListState) {
                is TrackListState.Success -> {
                    // Limit to 100 tracks. Android Auto will fail to browse if we send
                    // too many items.
                    result.sendResult(trackListState.tracks.take(100).map(Track::toMediaItem))
                }
                else -> {
                    result.sendResult(emptyList())
                }
            }
        }
            .take(1)
            .launchIn(serviceScope)
    }
}