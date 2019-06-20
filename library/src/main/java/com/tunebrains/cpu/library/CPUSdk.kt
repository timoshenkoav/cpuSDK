package com.tunebrains.cpu.library

import android.content.Context
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


class CPUSdk(ctx: Context, private val api: MedicaApi) {

    private val connectionObserver = ConnectionObserver(ctx)

    private val compositeDisposable = CompositeDisposable()
    fun init() {
        compositeDisposable.add(connectionObserver.onlineObserver.observeOn(Schedulers.io()).subscribe { onlineState ->
            if (onlineState.online) {
                compositeDisposable.add(api.ip().flatMapCompletable { ip -> api.informServer(ip.ip) }.subscribe({
                    Timber.d("Server updated on ip info")
                }, { ex ->
                    Timber.e(ex, "Server updated on ip info")
                }))
            }
        })
        start()
    }

    private fun start() {
        connectionObserver.start()
    }
}