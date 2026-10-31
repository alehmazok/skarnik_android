package by.mazokaleh.skarnik.core.common

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The current time, injected so tests can set it. */
fun interface WallClock {
    fun nowMillis(): Long
}

@Module
@InstallIn(SingletonComponent::class)
object WallClockModule {
    @Provides
    fun provideWallClock(): WallClock = WallClock(System::currentTimeMillis)
}
