package com.jacobarau.minstrel.data

import android.net.Uri
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito

class SortDimensionTest {

    private fun mockUri(): Uri = Mockito.mock(Uri::class.java)

    @Test
    fun sortDimension_folder_getGroupValue() {
        val track = Track(
            uri = mockUri(),
            filename = "song.mp3",
            directory = "/path/to/music",
            title = "Song"
        )
        val result = SortDimension.Folder.getGroupValue(track)
        assertEquals("/path/to/music", result)
    }

    @Test
    fun sortDimension_folder_withEmptyDirectory_returnsUnknownFolder() {
        val track = Track(
            uri = mockUri(),
            filename = "song.mp3",
            directory = "",
            title = "Song"
        )
        val result = SortDimension.Folder.getGroupValue(track)
        assertEquals("Unknown Folder", result)
    }

    @Test
    fun sortDimension_artist_getGroupValue() {
        val track = Track(
            uri = mockUri(),
            filename = "song.mp3",
            directory = "/music",
            artist = "Artist Name",
            title = "Song"
        )
        val result = SortDimension.Artist.getGroupValue(track)
        assertEquals("Artist Name", result)
    }

    @Test
    fun sortDimension_artist_withNullArtist_returnsUnknownArtist() {
        val track = Track(
            uri = mockUri(),
            filename = "song.mp3",
            directory = "/music",
            artist = null,
            title = "Song"
        )
        val result = SortDimension.Artist.getGroupValue(track)
        assertEquals("Unknown Artist", result)
    }

    @Test
    fun sortDimension_artist_withBlankArtist_returnsUnknownArtist() {
        val track = Track(
            uri = mockUri(),
            filename = "song.mp3",
            directory = "/music",
            artist = "   ",
            title = "Song"
        )
        val result = SortDimension.Artist.getGroupValue(track)
        assertEquals("Unknown Artist", result)
    }

    @Test
    fun sortDimension_album_getGroupValue() {
        val track = Track(
            uri = mockUri(),
            filename = "song.mp3",
            directory = "/music",
            album = "Album Name",
            title = "Song"
        )
        val result = SortDimension.Album.getGroupValue(track)
        assertEquals("Album Name", result)
    }

    @Test
    fun sortDimension_album_withNullAlbum_returnsUnknownAlbum() {
        val track = Track(
            uri = mockUri(),
            filename = "song.mp3",
            directory = "/music",
            album = null,
            title = "Song"
        )
        val result = SortDimension.Album.getGroupValue(track)
        assertEquals("Unknown Album", result)
    }

    @Test
    fun sortDimension_genre_getGroupValue() {
        val track = Track(
            uri = mockUri(),
            filename = "song.mp3",
            directory = "/music",
            genre = "Rock",
            title = "Song"
        )
        val result = SortDimension.Genre.getGroupValue(track)
        assertEquals("Rock", result)
    }

    @Test
    fun sortDimension_genre_withNullGenre_returnsUnknownGenre() {
        val track = Track(
            uri = mockUri(),
            filename = "song.mp3",
            directory = "/music",
            genre = null,
            title = "Song"
        )
        val result = SortDimension.Genre.getGroupValue(track)
        assertEquals("Unknown Genre", result)
    }

    @Test
    fun sortDimension_genre_withBlankGenre_returnsUnknownGenre() {
        val track = Track(
            uri = mockUri(),
            filename = "song.mp3",
            directory = "/music",
            genre = "  ",
            title = "Song"
        )
        val result = SortDimension.Genre.getGroupValue(track)
        assertEquals("Unknown Genre", result)
    }
}
