package com.jacobarau.minstrel.repository

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.jacobarau.minstrel.data.Track
import com.jacobarau.minstrel.data.TrackListState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject

class MediaStoreTrackRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : TrackRepository {

    private val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    override fun getTracks(filter: String?): Flow<TrackListState> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                // In case of a change, re-query and send the new list.
                trySend(TrackListState.Success(query(filter)))
            }
        }

        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            trySend(TrackListState.MissingPermissions)
        } else {
            // Register the observer to listen for changes.
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
            // Send the initial list of tracks.
            trySend(TrackListState.Success(query(filter)))
        }

        // This is the final statement, and will be called when the flow is cancelled.
        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    private fun query(filter: String?): List<Track> {
        val trackList = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST
        )

        val selectionClauses = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        selectionClauses.add("${MediaStore.Audio.Media.IS_MUSIC} != 0")

        if (!filter.isNullOrBlank()) {
            val filterPattern = "%$filter%"
            selectionClauses.add(
                "(${MediaStore.Audio.Media.DATA} LIKE ? OR " +
                        "${MediaStore.Audio.Media.TITLE} LIKE ? OR " +
                        "${MediaStore.Audio.Media.ALBUM} LIKE ? OR " +
                        "${MediaStore.Audio.Media.ARTIST} LIKE ?)"
            )
            selectionArgs.add(filterPattern)
            selectionArgs.add(filterPattern)
            selectionArgs.add(filterPattern)
            selectionArgs.add(filterPattern)
        }

        val selection = selectionClauses.joinToString(" AND ")
        val sortOrder = "${MediaStore.Audio.Media.DATA} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs.toTypedArray(),
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val path = cursor.getString(dataColumn)
                val title = cursor.getString(titleColumn)
                val album = cursor.getString(albumColumn)
                val artist = cursor.getString(artistColumn)
                val file = File(path)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                trackList.add(Track(contentUri, title, artist, album, file.name, file.parent ?: ""))
            }
        }
        return trackList
    }
}
