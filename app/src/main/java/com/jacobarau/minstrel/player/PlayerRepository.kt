package com.jacobarau.minstrel.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.jacobarau.minstrel.media.PlayerService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val player: Player
) {
    private val tag = this.javaClass.simpleName
    private var serviceConnection: ServiceConnection? = null

    /**
     * Binds to PlayerService to keep it alive during playback.
     * The system handles reference counting for multiple clients.
     */
    fun bind() {
        if (serviceConnection != null) {
            Log.d(tag, "Already bound to PlayerService")
            return
        }

        Log.d(tag, "Binding to PlayerService")
        val intent = Intent(context, PlayerService::class.java)
        
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.d(tag, "Connected to PlayerService")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.d(tag, "Disconnected from PlayerService")
            }
        }
        
        serviceConnection = connection
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Unbinds from PlayerService.
     * The service will stop when no other components are bound to it.
     */
    fun unbind() {
        if (serviceConnection == null) {
            Log.d(tag, "Not bound to PlayerService")
            return
        }

        Log.d(tag, "Unbinding from PlayerService")
        try {
            context.unbindService(serviceConnection!!)
        } catch (e: Exception) {
            Log.e(tag, "Error unbinding service", e)
        }
        serviceConnection = null
    }

    /**
     * Gets the Player instance managed by this repository.
     * The Player is injected and managed through DI.
     */
    fun getPlayer(): Player = player
}