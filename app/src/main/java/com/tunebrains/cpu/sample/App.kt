package com.tunebrains.cpu.sample

import android.app.Application
import com.tunebrains.cpu.sample.debug.LumberYard
import timber.log.Timber


class App : Application() {
    val lumberYard = LumberYard(this)
    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
        Timber.plant(lumberYard.tree())
    }
}