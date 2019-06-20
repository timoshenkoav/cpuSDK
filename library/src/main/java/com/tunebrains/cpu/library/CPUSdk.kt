package com.tunebrains.cpu.library

import android.content.Context
import android.content.Intent
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

data class DeviceState(val online: OnlineState, val battery: Intent)
class CPUSdk(ctx: Context, private val api: MedicaApi) {

    private val connectionObserver = ConnectionObserver(ctx)
    private val batteryObserver = BatteryObserver(ctx)

    private val compositeDisposable = CompositeDisposable()
    fun init() {
        compositeDisposable.add(
            deviceListener().observeOn(Schedulers.io()).subscribe { deviceState ->
                if (deviceState.online.online) {
                    compositeDisposable.add(api.ip().flatMapCompletable { ip -> api.informServer(ip.ip) }.subscribe({
                        Timber.d("Server updated on ip info")
                    }, { ex ->
                        Timber.e(ex, "Server updated on ip info")
                    }))
                }
            })
        start()
    }

    private fun deviceListener(): Observable<DeviceState> {
        return Observable.combineLatest<OnlineState, Intent, DeviceState>(
            connectionObserver.onlineObserver,
            batteryObserver.rxBroadcast,
            BiFunction { state, batter ->
                DeviceState(state, batter)
            })
    }

    private fun start() {
        connectionObserver.start()
    }
}