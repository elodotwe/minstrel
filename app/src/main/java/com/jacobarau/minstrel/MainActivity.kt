package com.jacobarau.minstrel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobarau.minstrel.ui.MinstrelSearchBar
import com.jacobarau.minstrel.ui.PlayerViewModel
import com.jacobarau.minstrel.ui.SearchOverlay
import com.jacobarau.minstrel.ui.TrackList
import com.jacobarau.minstrel.ui.TransportControls
import com.jacobarau.minstrel.ui.theme.MinstrelTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MinstrelTheme {
                val trackListState by viewModel.tracks.collectAsStateWithLifecycle()
                val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
                val isPreviousEnabled by viewModel.isPreviousEnabled.collectAsStateWithLifecycle()
                val isNextEnabled by viewModel.isNextEnabled.collectAsStateWithLifecycle()
                val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsStateWithLifecycle()

                var searchQuery by remember { mutableStateOf("") }
                var showSearchOverlay by remember { mutableStateOf(false) }

                if (showSearchOverlay) {
                    SearchOverlay(
                        searchQuery = searchQuery,
                        onSearchQueryChanged = {
                            searchQuery = it
                            viewModel.onSearchQueryChanged(it)
                        },
                        onClose = { showSearchOverlay = false },
                        trackListState = trackListState,
                        onTrackSelected = { track ->
                            viewModel.onTrackSelected(track, trackListState)
                            showSearchOverlay = false
                            searchQuery = ""
                            viewModel.onSearchQueryChanged("")
                        }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
                        topBar = {
                            MinstrelSearchBar()
                        },
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { showSearchOverlay = true }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        },
                        bottomBar = {
                            TransportControls(
                                playbackState = playbackState,
                                isPreviousEnabled = isPreviousEnabled,
                                isNextEnabled = isNextEnabled,
                                shuffleModeEnabled = shuffleModeEnabled,
                                onPlayPauseClicked = { viewModel.onPlayPauseClicked() },
                                onPreviousClicked = { viewModel.onPreviousClicked() },
                                onNextClicked = { viewModel.onNextClicked() },
                                onShuffleClicked = { viewModel.onShuffleClicked() },
                                onSeek = { viewModel.onSeek(it) }
                            )
                        }
                    ) { innerPadding ->
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
}
