package com.jacobarau.minstrel.data

sealed interface GroupedTrackItem {
    data class GroupHeader(val groupName: String) : GroupedTrackItem
    data class TrackItem(val track: Track) : GroupedTrackItem
}
