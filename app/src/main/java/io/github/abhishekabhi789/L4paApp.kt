package io.github.abhishekabhi789

import android.app.Application
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.github.abhishekabhi789.lyricsforpoweramp.BuildConfig
import javax.inject.Inject


@HiltAndroidApp
class L4paApp : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder().setWorkerFactory(workerFactory).build()
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll() // Detects all potential thread violations
                    .penaltyLog() // Logs violations to logcat
                    // .penaltyDialog() // Shows an annoying dialog
                    // .penaltyDeath() // Crashes the app on violation (useful for finding hard errors)
                    .build()
            )

            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
    }
}
