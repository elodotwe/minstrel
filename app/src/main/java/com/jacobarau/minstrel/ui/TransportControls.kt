package com.jacobarau.minstrel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.jacobarau.minstrel.data.Track
import com.jacobarau.minstrel.player.PlaybackState
import com.jacobarau.minstrel.ui.theme.MinstrelTheme
import java.util.concurrent.TimeUnit

@Composable
fun TransportControls(
    playbackState: PlaybackState,
    isPreviousEnabled: Boolean,
    isNextEnabled: Boolean,
    shuffleModeEnabled: Boolean,
    onPlayPauseClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onShuffleClicked: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (playbackState is PlaybackState.Playing) {
        val track = playbackState.tracks.getOrNull(playbackState.currentTrackIndex)
        if (track != null) {
            Surface(tonalElevation = 3.dp, modifier = modifier) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val duration =
                        playbackState.trackDurationMillis.toFloat().coerceAtLeast(0f)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var isAlbumArtVisible by remember { mutableStateOf(true) }
                        LaunchedEffect(track.albumArtUri) {
                            isAlbumArtVisible = true
                        }

                        if (track.albumArtUri != null && isAlbumArtVisible) {
                            AsyncImage(
                                model = track.albumArtUri,
                                contentDescription = "Album art",
                                modifier = Modifier.size(64.dp),
                                onError = { isAlbumArtVisible = false }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = track.title ?: track.filename,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatTime(playbackState.trackProgressMillis),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                                Text(
                                    text = " / ",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = formatTime(playbackState.trackDurationMillis),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Slider(
                                value = playbackState.trackProgressMillis
                                    .toFloat()
                                    .coerceIn(0f, duration),
                                onValueChange = { onSeek(it.toLong()) },
                                valueRange = 0f..duration,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPreviousClicked,
                            enabled = isPreviousEnabled,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        IconButton(
                            onClick = onPlayPauseClicked,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Icon(
                                imageVector = if (!playbackState.isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        IconButton(
                            onClick = onNextClicked,
                            enabled = isNextEnabled,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        IconButton(
                            onClick = onShuffleClicked,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Spacer(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Spacer(modifier = modifier)
}

private fun formatTime(millis: Long): String {
    if (millis < 0L) {
        return "--:--"
    }
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Preview(showBackground = true)
@Composable
private fun TransportControlsPreview() {
    MinstrelTheme {
        TransportControls(
            playbackState = PlaybackState.Playing(
                tracks = listOf(
                    Track(
                        uri = android.net.Uri.EMPTY,
                        filename = "Song.mp3",
                        directory = "/Music",
                        title = "Sample Track",
                        artist = "Sample Artist",
                        album = "Sample Album",
                        albumArtUri = null
                    )
                ),
                currentTrackIndex = 0,
                isPaused = false,
                trackProgressMillis = 30000,
                trackDurationMillis = 180000
            ),
            isPreviousEnabled = false,
            isNextEnabled = true,
            shuffleModeEnabled = false,
            onPlayPauseClicked = {},
            onPreviousClicked = {},
            onNextClicked = {},
            onShuffleClicked = {},
            onSeek = {}
        )
    }
}
