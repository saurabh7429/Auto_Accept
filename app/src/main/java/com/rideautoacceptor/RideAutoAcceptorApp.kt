package com.rideautoacceptor

import android.app.Application
import android.util.Log
import com.rideautoacceptor.data.local.AppDatabase
import com.rideautoacceptor.data.local.PreferencesManager
import com.rideautoacceptor.data.repository.RideRepository

/**
 * Application class — initializes singletons at app startup.
 * Provides a global accessor for the repository.
 */
class RideAutoAcceptorApp : Application() {

    lateinit var repository: RideRepository
        private set

    override fun onCreate() {
        super.onCreate()
        Log.i("App", "RideAutoAcceptor starting up")

        val db      = AppDatabase.getInstance(this)
        val prefs   = PreferencesManager(this)
        repository  = RideRepository.getInstance(db, prefs)
    }

    companion object {
        fun get(context: android.content.Context): RideAutoAcceptorApp =
            context.applicationContext as RideAutoAcceptorApp
    }
}
