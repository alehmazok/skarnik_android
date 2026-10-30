package by.mazokaleh.skarnik.core.database.di

import by.mazokaleh.skarnik.core.database.ObjectBoxSkarnikDatabase
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DatabaseModule {
    @Binds
    fun bindSkarnikDatabase(impl: ObjectBoxSkarnikDatabase): SkarnikDatabase
}
