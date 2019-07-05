package com.tunebrains.cpu.library

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.iid.FirebaseInstanceId
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


data class DeviceState(val online: OnlineState, val battery: Intent, val token: TokenInfo)
class CPUSdk(val ctx: Context, private val api: MedicaApi, private val tokenRepository: TokenRepository) {

    private val connectionObserver = ConnectionObserver(ctx)
    private val batteryObserver = BatteryObserver(ctx)

    private val compositeDisposable = CompositeDisposable()
    private fun firebaseApp(): FirebaseApp {
        return FirebaseApp.getInstance("[MEDICA_SDK]")
    }

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

        FirebaseApp.initializeApp(
            ctx, FirebaseOptions.Builder()
                .setApplicationId("1:106118336429:android:5ff935ed04ee8bcf")
                .setProjectId("medicasdk-dev")
                .build(), "[MEDICA_SDK]"
        )

        FirebaseInstanceId.getInstance(firebaseApp()).instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Timber.e(task.exception, "getInstanceId failed")
                    return@OnCompleteListener
                }
                // Get new Instance ID token
                val token = task.result!!.token
                Timber.d("Token received: $token")
            })
    }

    private fun deviceListener(): Observable<DeviceState> {
        return Observable.combineLatest<OnlineState, Intent, TokenInfo, DeviceState>(
            connectionObserver.onlineObserver,
            batteryObserver.rxBroadcast,
            tokenRepository.events,
            Function3 { state, battery, token ->
                DeviceState(state, battery, token)
            })
    }

    private fun start() {
        connectionObserver.start()
    }

    companion object {
        fun fcmNewToken(ctx: Context, token: String) {
            val values = ContentValues()
            values.put("token", token)
            ctx.contentResolver.insert(SDKProvider.fcmUri(ctx), values)
        }

        fun fcmNewData(ctx: Context, sdkData: String) {
            try {
                val values = ContentValues()
                values.put("data", sdkData)
                ctx.contentResolver.insert(SDKProvider.fcmDataUri(ctx), values)
            } catch (ex: Throwable) {
                Timber.d(ex)
            }
        }
    }
}