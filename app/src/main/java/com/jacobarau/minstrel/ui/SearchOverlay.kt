package com.jacobarau.minstrel.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jacobarau.minstrel.data.Track
import com.jacobarau.minstrel.data.TrackListState
import com.jacobarau.minstrel.player.PlaybackState
import com.jacobarau.minstrel.ui.theme.MinstrelTheme

@Composable
fun SearchOverlay(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onClose: () -> Unit,
    trackListState: TrackListState,
    onTrackSelected: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    val speechRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data: Intent? = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                onSearchQueryChanged(spokenText)
            }
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a search term")
            }
            speechRecognitionLauncher.launch(intent)
        }
    }

    fun launchVoiceSearch() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a search term")
                }
                speechRecognitionLauncher.launch(intent)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Close button, search input, and microphone button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close search",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier.focusRequester(focusRequester),
                    placeholder = { Text("Search tracks...") },
                    textStyle = MaterialTheme.typography.headlineSmall,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = { launchVoiceSearch() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Voice search",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Results list - reuse TrackList component
            Box(modifier = Modifier.weight(1f)) {
                TrackList(
                    trackListState = trackListState,
                    playbackState = PlaybackState.Stopped,
                    onTrackSelected = onTrackSelected,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchOverlayPreview() {
    MinstrelTheme {
        SearchOverlay(
            searchQuery = "",
            onSearchQueryChanged = {},
            onClose = {},
            trackListState = TrackListState.Success(
                tracks = listOf(
                    Track(
                        uri = android.net.Uri.EMPTY,
                        filename = "song1.mp3",
                        directory = "/storage/emulated/0/Music",
                        title = "Track One",
                        artist = "Artist A"
                    ),
                    Track(
                        uri = android.net.Uri.EMPTY,
                        filename = "song2.mp3",
                        directory = "/storage/emulated/0/Music",
                        title = "Track Two",
                        artist = "Artist B"
                    ),
                    Track(
                        uri = android.net.Uri.EMPTY,
                        filename = "song3.mp3",
                        directory = "/storage/emulated/0/Music",
                        title = "Track Three",
                        artist = "Artist C"
                    )
                )
            ),
            onTrackSelected = {}
        )
    }
}
