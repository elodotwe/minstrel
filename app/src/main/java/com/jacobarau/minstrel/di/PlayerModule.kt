package com.jacobarau.minstrel.di

import android.content.Context
import com.jacobarau.minstrel.player.ExoPlayerPlayer
import com.jacobarau.minstrel.player.Player
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    @Provides
    @Singleton
    fun providePlayer(@ApplicationContext context: Context): Player {
        return ExoPlayerPlayer(context)
    }
}
