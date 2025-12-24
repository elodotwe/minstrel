package com.jacobarau.minstrel.di

import com.jacobarau.minstrel.repository.MediaStoreTrackRepository
import com.jacobarau.minstrel.repository.TrackRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindTrackRepository(impl: MediaStoreTrackRepository): TrackRepository
}
