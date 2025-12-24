package com.jacobarau.minstrel.data

sealed interface TrackListState {
    data object Loading : TrackListState
    data class Success(val tracks: List<Track>) : TrackListState
    data object MissingPermissions : TrackListState
}
