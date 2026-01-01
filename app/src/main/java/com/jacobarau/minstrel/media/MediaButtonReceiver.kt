package com.jacobarau.minstrel.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MediaButtonReceiver : BroadcastReceiver() {
    private val tag = this.javaClass.simpleName
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_MEDIA_BUTTON) {
            return
        }
        Log.d(tag, "Received media button event")
        val serviceIntent = Intent(context, PlayerService::class.java)
        serviceIntent.putExtras(intent.extras!!)
        context.startForegroundService(serviceIntent)
    }
}