package dev.pschmitt.syncwich

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.hilt.android.HiltAndroidApp
import dev.pschmitt.syncwich.sync.SyncScheduler
import javax.inject.Inject
import okhttp3.OkHttpClient
import timber.log.Timber

@HiltAndroidApp
class SyncwichApp : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncScheduler: SyncScheduler
    // Recipe images are cached via Coil (AGENTS.md), but a self-hosted Mealie instance can lock
    // media behind auth same as the API - reusing the app's own authenticated/dynamic-base-URL
    // OkHttpClient (see NetworkModule) instead of Coil's default client covers that case for free.
    @Inject lateinit var okHttpClient: OkHttpClient

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient })) }
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Both calls are no-ops until onboarding is complete (SyncWorker.doWork() returns early
        // when unconfigured) - scheduled unconditionally here so the very first sync fires as soon
        // as a connection is saved, without every screen having to know to kick one off itself.
        syncScheduler.schedulePeriodic()
        syncScheduler.scheduleStartup()
    }
}
