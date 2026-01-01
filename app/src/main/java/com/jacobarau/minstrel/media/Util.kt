package com.jacobarau.minstrel.media

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.jacobarau.minstrel.data.Track
import com.jacobarau.minstrel.player.PlaybackState

fun PlaybackState.toPlaybackStateCompat(): Int {
    return when (this) {
        is PlaybackState.Playing -> PlaybackStateCompat.STATE_PLAYING
        is PlaybackState.Paused -> PlaybackStateCompat.STATE_PAUSED
        is PlaybackState.Stopped -> PlaybackStateCompat.STATE_STOPPED
    }
}

private fun Track.getDescription(): MediaDescriptionCompat {
    return MediaDescriptionCompat.Builder()
        .setMediaId(uri.toString())
        .setTitle(title)
        .setSubtitle(artist)
        .build()
}

fun Track.toQueueItem(id: Long): MediaSessionCompat.QueueItem {
    return MediaSessionCompat.QueueItem(getDescription(), id)
}

fun Track.toMediaItem(): MediaBrowserCompat.MediaItem {
    return MediaBrowserCompat.MediaItem(getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
}