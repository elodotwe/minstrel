package com.jacobarau.minstrel.data

object TrackGrouper {
    fun groupAndSort(tracks: List<Track>, sortDimension: SortDimension?): List<GroupedTrackItem> {
        if (sortDimension == null) {
            // If no sorting is selected, just return tracks sorted by path
            return tracks.sortedBy { it.directory }
                .map { GroupedTrackItem.TrackItem(it) }
        }

        // Group tracks by the selected dimension
        val grouped = tracks.groupBy { sortDimension.getGroupValue(it) }

        // Sort groups alphabetically and flatten with headers
        val result = mutableListOf<GroupedTrackItem>()
        grouped.toSortedMap().forEach { (groupName, groupTracks) ->
            result.add(GroupedTrackItem.GroupHeader(groupName))
            // Sort tracks within each group by title or filename
            groupTracks.sortedBy { it.title ?: it.filename }
                .forEach { track ->
                    result.add(GroupedTrackItem.TrackItem(track))
                }
        }

        return result
    }
}
