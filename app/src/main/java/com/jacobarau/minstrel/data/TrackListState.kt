package com.jacobarau.minstrel.data

sealed interface TrackListState {
    data object Loading : TrackListState
    data class Success(
        val tracks: List<Track>,
        val groupedItems: List<GroupedTrackItem> = emptyList(),
        val sortDimension: SortDimension? = null
    ) : TrackListState
    data object MissingPermissions : TrackListState
}
