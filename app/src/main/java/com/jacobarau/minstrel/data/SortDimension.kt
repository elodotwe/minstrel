package com.jacobarau.minstrel.data

sealed class SortDimension {
    data object Folder : SortDimension()
    data object Artist : SortDimension()
    data object Album : SortDimension()
    data object Genre : SortDimension()

    fun getDisplayName(): String = when (this) {
        Folder -> "Folder"
        Artist -> "Artist"
        Album -> "Album"
        Genre -> "Genre"
    }

    fun getGroupValue(track: Track): String = when (this) {
        Folder -> track.directory.ifBlank { "Unknown Folder" }
        Artist -> track.artist?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
        Album -> track.album?.takeIf { it.isNotBlank() } ?: "Unknown Album"
        Genre -> track.genre?.takeIf { it.isNotBlank() } ?: "Unknown Genre"
    }
}
