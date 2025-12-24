package com.jacobarau.minstrel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobarau.minstrel.data.Track
import com.jacobarau.minstrel.data.TrackListState
import com.jacobarau.minstrel.ui.TrackViewModel
import com.jacobarau.minstrel.ui.theme.MinstrelTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: TrackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MinstrelTheme {
                val trackListState by viewModel.tracks.collectAsStateWithLifecycle()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TrackList(
                        trackListState = trackListState,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun TrackList(trackListState: TrackListState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (trackListState) {
            TrackListState.Loading -> {
                CircularProgressIndicator()
            }
            TrackListState.MissingPermissions -> {
                Text(text = "Missing permissions to read audio files.")
            }
            is TrackListState.Success -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(trackListState.tracks) { track ->
                        Text(text = track.filename, modifier = Modifier.padding(16.dp))
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
                        directory = "/storage/emulated/0/Music"
                    ),
                    Track(
                        uri = android.net.Uri.EMPTY,
                        filename = "song2.mp3",
                        directory = "/storage/emulated/0/Music"
                    )
                )
            )
        )
    }
}
