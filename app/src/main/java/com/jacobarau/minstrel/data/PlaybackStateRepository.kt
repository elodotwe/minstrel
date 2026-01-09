package com.jacobarau.minstrel.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class StoredPlaybackState(
    val trackUri: String?,
    val position: Long,
    val searchQuery: String?,
    val isShuffleEnabled: Boolean
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PlaybackStateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val TRACK_URI = stringPreferencesKey("current_track_uri")
        val TRACK_POSITION = longPreferencesKey("current_track_position")
        val SEARCH_QUERY = stringPreferencesKey("search_query")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
    }

    val storedPlaybackState: Flow<StoredPlaybackState> = context.dataStore.data
        .map { preferences ->
            StoredPlaybackState(
                trackUri = preferences[Keys.TRACK_URI],
                position = preferences[Keys.TRACK_POSITION] ?: 0L,
                searchQuery = preferences[Keys.SEARCH_QUERY],
                isShuffleEnabled = preferences[Keys.SHUFFLE_ENABLED] ?: false
            )
        }

    suspend fun savePlaybackState(
        uri: String?,
        position: Long,
        searchQuery: String?,
        isShuffleEnabled: Boolean
    ) {
        context.dataStore.edit { settings ->
            if (uri == null) {
                settings.remove(Keys.TRACK_URI)
            } else {
                settings[Keys.TRACK_URI] = uri
            }
            settings[Keys.TRACK_POSITION] = position
            if (searchQuery == null) {
                settings.remove(Keys.SEARCH_QUERY)
            } else {
                settings[Keys.SEARCH_QUERY] = searchQuery
            }
            settings[Keys.SHUFFLE_ENABLED] = isShuffleEnabled
        }
    }
}
