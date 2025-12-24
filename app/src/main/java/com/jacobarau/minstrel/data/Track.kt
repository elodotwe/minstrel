package com.jacobarau.minstrel.data

import android.net.Uri

data class Track(
    val uri: Uri,
    val filename: String,
    val directory: String
)
