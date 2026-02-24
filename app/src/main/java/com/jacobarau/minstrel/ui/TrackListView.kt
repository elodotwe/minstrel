package com.jacobarau.minstrel.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacobarau.minstrel.data.Track
import com.jacobarau.minstrel.data.TrackListState
import com.jacobarau.minstrel.player.PlaybackState
import com.jacobarau.minstrel.ui.theme.MinstrelTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackList(
    trackListState: TrackListState,
    playbackState: PlaybackState,
    onTrackSelected: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (trackListState) {
            TrackListState.Loading -> {
                CircularProgressIndicator()
            }

            TrackListState.MissingPermissions -> {
                Text(text = "Missing permissions to read audio files.")
            }

            is TrackListState.Success -> {
                val currentTrack = if (playbackState is PlaybackState.Playing) {
                    playbackState.tracks.getOrNull(playbackState.currentTrackIndex)
                } else {
                    null
                }
                val lazyListState = rememberLazyListState()
                val flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState, snapPosition = SnapPosition.Start)
                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(currentTrack, trackListState) {
                    currentTrack?.let {
                        val index = trackListState.tracks.indexOf(it)
                        if (index != -1) {
                            coroutineScope.launch {
                                lazyListState.animateScrollToItem(index)
                            }
                        }
                    }
                }

                LazyColumn(
                    state = lazyListState,
                    flingBehavior = flingBehavior,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    itemsIndexed(trackListState.tracks) { _, track ->
                        val isPlaying = track == currentTrack
                        val backgroundColor = if (isPlaying) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        }

                        Column(
                            modifier = Modifier
                                .background(backgroundColor)
                                .clickable { onTrackSelected(track) }
                                .padding(vertical = 24.dp, horizontal = 16.dp)
                                .fillMaxWidth()
                        ) {
                            val textColor = if (isPlaying) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                            val secondaryTextColor = if (isPlaying) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(
                                text = track.title ?: track.filename,
                                color = textColor,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = track.directory,
                                maxLines = 1,
                                overflow = TextOverflow.StartEllipsis,
                                style = MaterialTheme.typography.bodyLarge,
                                color = secondaryTextColor
                            )
                            Text(
                                text = track.artist ?: "Unknown Artist",
                                style = MaterialTheme.typography.bodyMedium,
                                color = secondaryTextColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrackListPreview() {
    MinstrelTheme {
        TrackList(
            trackListState = TrackListState.Success(
                tracks = listOf(
                    Track(
                        uri = android.net.Uri.EMPTY,
                        filename = "song1.mp3",
                        directory = "/storage/emulated/0/Music/A long directory name that will surely be truncated"
                    ),
                    Track(
                        uri = android.net.Uri.EMPTY,
                        filename = "song2.mp3",
                        directory = "/storage/emulated/0/Music"
                    )
                )
            ),
            playbackState = PlaybackState.Stopped,
            onTrackSelected = {}
        )
    }
}
