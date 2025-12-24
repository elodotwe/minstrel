package com.jacobarau.minstrel.repository

import com.jacobarau.minstrel.data.Track
import kotlinx.coroutines.flow.Flow

interface TrackRepository {
    fun getTracks(): Flow<List<Track>>
}
