package dev.pschmitt.syncwich

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import dev.pschmitt.syncwich.sync.SyncScheduler
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class SyncwichApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncScheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

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
