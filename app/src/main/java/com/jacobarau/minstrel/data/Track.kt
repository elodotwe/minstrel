package com.jacobarau.minstrel.data

import android.net.Uri

data class Track(
    val uri: Uri,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val filename: String,
    val directory: String
)
