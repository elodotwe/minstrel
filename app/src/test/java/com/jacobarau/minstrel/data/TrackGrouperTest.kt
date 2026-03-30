package com.jacobarau.minstrel.data

import android.net.Uri
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito

class TrackGrouperTest {

    private fun mockUri(): Uri = Mockito.mock(Uri::class.java)

    @Test
    fun groupAndSort_withNullDimension_returnsSortedByDirectory() {
        val tracks = listOf(
            Track(uri = mockUri(), filename = "song1.mp3", directory = "/Music/B"),
            Track(uri = mockUri(), filename = "song2.mp3", directory = "/Music/A"),
            Track(uri = mockUri(), filename = "song3.mp3", directory = "/Music/C")
        )

        val result = TrackGrouper.groupAndSort(tracks, null)

        assertEquals(3, result.size)
        assertTrue(result[0] is GroupedTrackItem.TrackItem)
        assertEquals("/Music/A", (result[0] as GroupedTrackItem.TrackItem).track.directory)
        assertEquals("/Music/B", (result[1] as GroupedTrackItem.TrackItem).track.directory)
        assertEquals("/Music/C", (result[2] as GroupedTrackItem.TrackItem).track.directory)
    }

    @Test
    fun groupAndSort_byArtist_createsHeadersAndGroupsTracks() {
        val tracks = listOf(
            Track(uri = mockUri(), filename = "song1.mp3", directory = "/Music", artist = "Artist B", title = "Track 2"),
            Track(uri = mockUri(), filename = "song2.mp3", directory = "/Music", artist = "Artist A", title = "Track 1"),
            Track(uri = mockUri(), filename = "song3.mp3", directory = "/Music", artist = "Artist A", title = "Track 3"),
            Track(uri = mockUri(), filename = "song4.mp3", directory = "/Music", artist = "Artist B", title = "Track 4")
        )

        val result = TrackGrouper.groupAndSort(tracks, SortDimension.Artist)

        // Expected order: Header "Artist A", Track 1, Track 3, Header "Artist B", Track 2, Track 4
        assertEquals(6, result.size)
        assertTrue(result[0] is GroupedTrackItem.GroupHeader)
        assertEquals("Artist A", (result[0] as GroupedTrackItem.GroupHeader).groupName)
        
        assertTrue(result[1] is GroupedTrackItem.TrackItem)
        assertEquals("Track 1", (result[1] as GroupedTrackItem.TrackItem).track.title)
        
        assertTrue(result[2] is GroupedTrackItem.TrackItem)
        assertEquals("Track 3", (result[2] as GroupedTrackItem.TrackItem).track.title)
        
        assertTrue(result[3] is GroupedTrackItem.GroupHeader)
        assertEquals("Artist B", (result[3] as GroupedTrackItem.GroupHeader).groupName)
        
        assertTrue(result[4] is GroupedTrackItem.TrackItem)
        assertEquals("Track 2", (result[4] as GroupedTrackItem.TrackItem).track.title)
        
        assertTrue(result[5] is GroupedTrackItem.TrackItem)
        assertEquals("Track 4", (result[5] as GroupedTrackItem.TrackItem).track.title)
    }

    @Test
    fun groupAndSort_byAlbum_groupsCorrectly() {
        val tracks = listOf(
            Track(uri = mockUri(), filename = "song1.mp3", directory = "/Music", album = "Album B", title = "Track 2"),
            Track(uri = mockUri(), filename = "song2.mp3", directory = "/Music", album = "Album A", title = "Track 1"),
            Track(uri = mockUri(), filename = "song3.mp3", directory = "/Music", album = "Album B", title = "Track 3")
        )

        val result = TrackGrouper.groupAndSort(tracks, SortDimension.Album)

        assertEquals(5, result.size)
        assertTrue(result[0] is GroupedTrackItem.GroupHeader)
        assertEquals("Album A", (result[0] as GroupedTrackItem.GroupHeader).groupName)
    }

    @Test
    fun groupAndSort_byFolder_groupsCorrectly() {
        val tracks = listOf(
            Track(uri = mockUri(), filename = "song1.mp3", directory = "/Music/B", title = "Track 2"),
            Track(uri = mockUri(), filename = "song2.mp3", directory = "/Music/A", title = "Track 1"),
            Track(uri = mockUri(), filename = "song3.mp3", directory = "/Music/A", title = "Track 3")
        )

        val result = TrackGrouper.groupAndSort(tracks, SortDimension.Folder)

        assertEquals(5, result.size)
        assertTrue(result[0] is GroupedTrackItem.GroupHeader)
        assertEquals("/Music/A", (result[0] as GroupedTrackItem.GroupHeader).groupName)
    }

    @Test
    fun groupAndSort_byGenre_groupsCorrectly() {
        val tracks = listOf(
            Track(uri = mockUri(), filename = "song1.mp3", directory = "/Music", genre = "Rock", title = "Track 2"),
            Track(uri = mockUri(), filename = "song2.mp3", directory = "/Music", genre = "Jazz", title = "Track 1"),
            Track(uri = mockUri(), filename = "song3.mp3", directory = "/Music", genre = "Rock", title = "Track 3")
        )

        val result = TrackGrouper.groupAndSort(tracks, SortDimension.Genre)

        assertEquals(5, result.size)
        assertTrue(result[0] is GroupedTrackItem.GroupHeader)
        assertEquals("Jazz", (result[0] as GroupedTrackItem.GroupHeader).groupName)
        assertTrue(result[1] is GroupedTrackItem.TrackItem)
        assertEquals("Track 1", (result[1] as GroupedTrackItem.TrackItem).track.title)
    }

    @Test
    fun groupAndSort_withNullValues_usesDefaultGroupNames() {
        val tracks = listOf(
            Track(uri = mockUri(), filename = "song1.mp3", directory = "/Music", artist = null, title = "Track 1"),
            Track(uri = mockUri(), filename = "song2.mp3", directory = "/Music", artist = "Artist A", title = "Track 2")
        )

        val result = TrackGrouper.groupAndSort(tracks, SortDimension.Artist)

        assertEquals(4, result.size)
        assertTrue(result[0] is GroupedTrackItem.GroupHeader)
        assertEquals("Artist A", (result[0] as GroupedTrackItem.GroupHeader).groupName)
        assertTrue(result[2] is GroupedTrackItem.GroupHeader)
        assertEquals("Unknown Artist", (result[2] as GroupedTrackItem.GroupHeader).groupName)
    }

    @Test
    fun groupAndSort_withEmptyDirectory_usesDefaultGroupName() {
        val tracks = listOf(
            Track(uri = mockUri(), filename = "song1.mp3", directory = "", title = "Track 1")
        )

        val result = TrackGrouper.groupAndSort(tracks, SortDimension.Folder)

        assertEquals(2, result.size)
        assertTrue(result[0] is GroupedTrackItem.GroupHeader)
        assertEquals("Unknown Folder", (result[0] as GroupedTrackItem.GroupHeader).groupName)
    }

    @Test
    fun groupAndSort_emptyTrackList_returnsEmpty() {
        val result = TrackGrouper.groupAndSort(emptyList(), SortDimension.Artist)

        assertEquals(0, result.size)
    }

    @Test
    fun groupAndSort_tracksWithinGroupAreSortedByTitle() {
        val tracks = listOf(
            Track(uri = mockUri(), filename = "song1.mp3", directory = "/Music", artist = "Artist A", title = "Zebra"),
            Track(uri = mockUri(), filename = "song2.mp3", directory = "/Music", artist = "Artist A", title = "Apple"),
            Track(uri = mockUri(), filename = "song3.mp3", directory = "/Music", artist = "Artist A", title = "Banana")
        )

        val result = TrackGrouper.groupAndSort(tracks, SortDimension.Artist)

        // Should be sorted: Header, Apple, Banana, Zebra
        assertEquals(4, result.size)
        assertTrue(result[1] is GroupedTrackItem.TrackItem)
        assertEquals("Apple", (result[1] as GroupedTrackItem.TrackItem).track.title)
        assertTrue(result[2] is GroupedTrackItem.TrackItem)
        assertEquals("Banana", (result[2] as GroupedTrackItem.TrackItem).track.title)
        assertTrue(result[3] is GroupedTrackItem.TrackItem)
        assertEquals("Zebra", (result[3] as GroupedTrackItem.TrackItem).track.title)
    }

    @Test
    fun groupAndSort_tracksWithoutTitleUsesFilename() {
        val tracks = listOf(
            Track(uri = mockUri(), filename = "zebra.mp3", directory = "/Music", artist = "Artist A", title = null),
            Track(uri = mockUri(), filename = "apple.mp3", directory = "/Music", artist = "Artist A", title = null)
        )

        val result = TrackGrouper.groupAndSort(tracks, SortDimension.Artist)

        // Should be sorted: Header, apple.mp3, zebra.mp3
        assertEquals(3, result.size)
        assertTrue(result[1] is GroupedTrackItem.TrackItem)
        assertEquals("apple.mp3", (result[1] as GroupedTrackItem.TrackItem).track.filename)
        assertTrue(result[2] is GroupedTrackItem.TrackItem)
        assertEquals("zebra.mp3", (result[2] as GroupedTrackItem.TrackItem).track.filename)
    }

    @Test
    fun sortDimension_getDisplayName_returnsCorrectNames() {
        assertEquals("Folder", SortDimension.Folder.getDisplayName())
        assertEquals("Artist", SortDimension.Artist.getDisplayName())
        assertEquals("Album", SortDimension.Album.getDisplayName())
        assertEquals("Genre", SortDimension.Genre.getDisplayName())
    }

    @Test
    fun sortDimension_getGroupValue_returnsCorrectValue() {
        val track = Track(
            uri = mockUri(),
            filename = "song.mp3",
            directory = "/Music/TestFolder",
            artist = "Test Artist",
            album = "Test Album",
            genre = "Test Genre",
            title = "Test Song"
        )

        assertEquals("/Music/TestFolder", SortDimension.Folder.getGroupValue(track))
        assertEquals("Test Artist", SortDimension.Artist.getGroupValue(track))
        assertEquals("Test Album", SortDimension.Album.getGroupValue(track))
        assertEquals("Test Genre", SortDimension.Genre.getGroupValue(track))
    }
}
