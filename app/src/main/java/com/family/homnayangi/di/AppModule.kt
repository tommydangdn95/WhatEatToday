package com.family.homnayangi.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.family.homnayangi.data.db.AppDatabase
import com.family.homnayangi.domain.SuggestionEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            // Không dùng fallbackToDestructiveMigration: mất dữ liệu của Mẹ là lỗi nghiêm trọng.
            .build()

    @Provides fun dishDao(db: AppDatabase) = db.dishDao()
    @Provides fun ingredientDao(db: AppDatabase) = db.ingredientDao()
    @Provides fun pantryDao(db: AppDatabase) = db.pantryDao()
    @Provides fun mealLogDao(db: AppDatabase) = db.mealLogDao()
    @Provides fun shoppingDao(db: AppDatabase) = db.shoppingDao()
    @Provides fun weatherDao(db: AppDatabase) = db.weatherDao()

    @Provides @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    @Provides @Singleton @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    fun suggestionEngine(): SuggestionEngine = SuggestionEngine()
}
