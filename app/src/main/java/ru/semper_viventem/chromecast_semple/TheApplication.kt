package ru.semper_viventem.chromecast_semple

import android.app.Application
import timber.log.Timber

class TheApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        initLogging()
    }

    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}