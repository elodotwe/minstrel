package com.jacobarau.minstrel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobarau.minstrel.data.Track
import com.jacobarau.minstrel.data.TrackListState
import com.jacobarau.minstrel.media.PlayerService
import com.jacobarau.minstrel.player.PlaybackState
import com.jacobarau.minstrel.ui.TrackViewModel
import com.jacobarau.minstrel.ui.theme.MinstrelTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: TrackViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startService(Intent(this, PlayerService::class.java))
        setContent {
            MinstrelTheme {
                val trackListState by viewModel.tracks.collectAsStateWithLifecycle()
                val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
                val trackProgress by viewModel.trackProgressMillis.collectAsStateWithLifecycle()
                val trackDuration by viewModel.trackDurationMillis.collectAsStateWithLifecycle()
                val isPreviousEnabled by viewModel.isPreviousEnabled.collectAsStateWithLifecycle()
                val isNextEnabled by viewModel.isNextEnabled.collectAsStateWithLifecycle()
                val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsStateWithLifecycle()

                var searchQuery by remember { mutableStateOf("") }
                var searchExpanded by remember { mutableStateOf(false) }
                val focusRequester = remember { FocusRequester() }

                Scaffold(modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                    topBar = {
                        TopAppBar(
                            title = {
                                if (searchExpanded) {
                                    TextField(
                                        value = searchQuery,
                                        onValueChange = {
                                            searchQuery = it
                                            viewModel.onSearchQueryChanged(it)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester),
                                        placeholder = { Text("Search") },
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                println("collapsing search")
                                                searchQuery = ""
                                                viewModel.onSearchQueryChanged("")
                                                searchExpanded = false
                                            }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Clear and close search"
                                                )
                                            }
                                        },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            disabledIndicatorColor = Color.Transparent,
                                        ),
                                        singleLine = true
                                    )
                                    LaunchedEffect(Unit) {
                                        focusRequester.requestFocus()
                                    }
                                } else {
                                    Text("Minstrel")
                                }
                            },
                            actions = {
                                if (!searchExpanded) {
                                    IconButton(onClick = { searchExpanded = true }) {
                                        Icon(Icons.Filled.Search, contentDescription = "Search")
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        if (playbackState !is PlaybackState.Stopped) {
                            val track = when (val state = playbackState) {
                                is PlaybackState.Playing -> state.track
                                is PlaybackState.Paused -> state.track
                                else -> null
                            }
                            if (track != null) {
                                Surface(tonalElevation = 3.dp) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        val duration =
                                            trackDuration.toFloat().coerceAtLeast(0f)

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
                                                text = formatTime(trackProgress),
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                            Text(
                                                text = " / ",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = formatTime(trackDuration),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }

                                        Slider(
                                            value = trackProgress
                                                .toFloat()
                                                .coerceIn(0f, duration),
                                            onValueChange = { viewModel.onSeek(it.toLong()) },
                                            valueRange = 0f..duration,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceAround,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.onPreviousClicked() },
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
                                                onClick = { viewModel.onPlayPauseClicked() },
                                                modifier = Modifier.size(80.dp) // Make play/pause bigger
                                            ) {
                                                Icon(
                                                    imageVector = if (playbackState is PlaybackState.Playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "Play/Pause",
                                                    modifier = Modifier.size(48.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.onNextClicked() },
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
                                                onClick = { viewModel.onShuffleClicked() },
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
                    }) { innerPadding ->
                    TrackList(
                        trackListState = trackListState,
                        playbackState = playbackState,
                        onTrackSelected = { track ->
                            viewModel.onTrackSelected(
                                track,
                                trackListState
                            )
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

fun formatTime(millis: Long): String {
    if (millis < 0L) {
        return "--:--"
    }
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

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
                val currentTrack = when (playbackState) {
                    is PlaybackState.Playing -> playbackState.track
                    is PlaybackState.Paused -> playbackState.track
                    else -> null
                }
                val lazyListState = rememberLazyListState()
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    MinstrelTheme {
        val track1 = Track(
            uri = android.net.Uri.EMPTY,
            filename = "song1.mp3",
            title = "The First Song",
            directory = "/storage/emulated/0/Music/Album1"
        )
        val track2 = Track(
            uri = android.net.Uri.EMPTY,
            filename = "song2.mp3",
            title = "The Second Song",
            directory = "/storage/emulated/0/Music/Album2"
        )
        val trackListState = TrackListState.Success(
            tracks = listOf(
                track1,
                track2
            )
        )
        val playbackState = PlaybackState.Playing(track1)
        val trackProgress = 15000L
        val trackDuration = 120000L
        val isPreviousEnabled = false
        val isNextEnabled = true
        val shuffleModeEnabled = true

        var searchQuery by remember { mutableStateOf("") }
        var searchExpanded by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }

        Scaffold(modifier = Modifier
            .fillMaxSize()
            .imePadding(),
            topBar = {
                TopAppBar(
                    title = {
                        if (searchExpanded) {
                            TextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                placeholder = { Text("Search") },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        searchExpanded = false
                                    }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear and close search"
                                        )
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                ),
                                singleLine = true
                            )
                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                        } else {
                            Text("Minstrel")
                        }
                    },
                    actions = {
                        if (!searchExpanded) {
                            IconButton(onClick = { searchExpanded = true }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (playbackState !is PlaybackState.Stopped) {
                    val track = when (val state = playbackState) {
                        is PlaybackState.Playing -> state.track
                        is PlaybackState.Paused -> state.track
                        else -> null
                    }
                    if (track != null) {
                        Surface(tonalElevation = 3.dp) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                val duration =
                                    trackDuration.toFloat().coerceAtLeast(0f)

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
                                        text = formatTime(trackProgress),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                    Text(
                                        text = " / ",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = formatTime(trackDuration),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Slider(
                                    value = trackProgress
                                        .toFloat()
                                        .coerceIn(0f, duration),
                                    onValueChange = { },
                                    valueRange = 0f..duration,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { },
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
                                        onClick = { },
                                        modifier = Modifier.size(80.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (playbackState is PlaybackState.Playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { },
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
                                        onClick = { },
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
            }) { innerPadding ->
            TrackList(
                trackListState = trackListState,
                playbackState = playbackState,
                onTrackSelected = { },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
