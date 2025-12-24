package com.jacobarau.minstrel.media

import androidx.media.session.MediaButtonReceiver

/**
 * A broadcast receiver for handling media button events. This receiver extends the
 * [MediaButtonReceiver] from the AndroidX media library. It is declared in the
 * `AndroidManifest.xml` with an intent filter for `android.intent.action.MEDIA_BUTTON`.
 *
 * The parent class implementation automatically forwards the received media button event to the
 * `MediaBrowserServiceCompat` that is also registered to handle the `MEDIA_BUTTON` action.
 * In this app, that service is `MinstrelService`.
 */
class MediaButtonReceiver : MediaButtonReceiver()
