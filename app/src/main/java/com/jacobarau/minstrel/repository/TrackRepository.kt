package com.jacobarau.minstrel.repository

import com.jacobarau.minstrel.data.TrackListState
import kotlinx.coroutines.flow.Flow

interface TrackRepository {
    fun getTracks(): Flow<TrackListState>
}
