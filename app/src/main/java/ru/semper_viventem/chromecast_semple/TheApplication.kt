package ru.semper_viventem.chromecast_semple

import android.app.Application
import com.google.android.gms.cast.framework.CastContext
import timber.log.Timber

class TheApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        initLogging()
        initChromeCast()
    }

    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun initChromeCast() {
        CastContext.getSharedInstance(this)
    }
}