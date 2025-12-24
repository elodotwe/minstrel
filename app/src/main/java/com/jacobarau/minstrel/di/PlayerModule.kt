package com.jacobarau.minstrel.di

import com.jacobarau.minstrel.player.ExoPlayerPlayer
import com.jacobarau.minstrel.player.Player
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {
    @Binds
    abstract fun bindPlayer(impl: ExoPlayerPlayer): Player
}
