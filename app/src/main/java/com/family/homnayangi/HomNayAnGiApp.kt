package com.family.homnayangi

import android.app.Application
import com.family.homnayangi.data.seed.SeedLoader
import com.family.homnayangi.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HomNayAnGiApp : Application() {
    @Inject lateinit var seedLoader: SeedLoader
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        appScope.launch { seedLoader.ensureSeeded() }
    }
}
