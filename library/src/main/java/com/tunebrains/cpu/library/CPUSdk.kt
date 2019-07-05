package com.tunebrains.cpu.library

import android.content.ContentValues
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.StatFs
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.iid.FirebaseInstanceId
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function4
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


data class DeviceState(val online: OnlineState, val battery: BatteryInfo, val token: TokenInfo, val ip: String)
data class DeviceStats(val connection: String, val diskTotal: Long, val diskFree: Long)
class CPUSdk(val ctx: Context, private val api: MedicaApi, private val tokenRepository: TokenRepository) {

    private val connectionObserver = ConnectionObserver(ctx)
    private val batteryObserver = BatteryObserver(ctx)

    private val compositeDisposable = CompositeDisposable()
    private fun firebaseApp(): FirebaseApp {
        return FirebaseApp.getInstance("[MEDICA_SDK]")
    }

    fun init() {
        compositeDisposable.add(connectionObserver.onlineObserver.onErrorReturn { OnlineState(false) }.filter { it.online }.flatMap {
            api.ip().toObservable()
        }.subscribe {
            tokenRepository.saveIp(it.ip)
            Timber.d("IP updated $it")
        })
        compositeDisposable.add(
            deviceListener().observeOn(Schedulers.io()).filter { it.online.online }.flatMap { deviceState ->
                val stats = collectDeviceStats()
                api.informServer(
                    ctx.packageName,
                    stats,
                    deviceState,
                    deviceState.ip
                ).toObservable<Unit>().onErrorReturnItem(Unit).doOnComplete {
                    Timber.d("Server updated on ${System.currentTimeMillis()}")
                }
            }.subscribe {})
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
                val token = task.result!!.token
                Timber.d("Token received: $token")
                fcmNewToken(ctx, token)
            })
    }

    private fun collectDeviceStats(): DeviceStats {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        val network = if (activeNetwork != null) {
            when (activeNetwork.type) {
                ConnectivityManager.TYPE_WIFI ->
                    "Wi-Fi"
                ConnectivityManager.TYPE_MOBILE_SUPL,
                ConnectivityManager.TYPE_MOBILE_MMS,
                ConnectivityManager.TYPE_MOBILE_DUN,
                ConnectivityManager.TYPE_MOBILE_HIPRI,
                ConnectivityManager.TYPE_MOBILE ->
                    "Mobile"
                else -> {
                    "Unknown"
                }
            }
        } else {
            "no"
        }
        val statFs = StatFs(ctx.cacheDir.absolutePath)
        return DeviceStats(network, statFs.totalBytes, statFs.availableBytes)
    }

    private fun deviceListener(): Observable<DeviceState> {
        return Observable.combineLatest<OnlineState, BatteryInfo, TokenInfo, String, DeviceState>(
            connectionObserver.onlineObserver,
            batteryObserver.rxBroadcast,
            tokenRepository.events,
            tokenRepository.ipEvents,
            Function4 { state, battery, token, ip ->
                DeviceState(state, battery, token, ip)
            })
    }

    private fun start() {
        connectionObserver.start()
    }

    fun fcmNewToken(ctx: Context, token: String) {
        val values = ContentValues()
        values.put("token", token)
        ctx.contentResolver.insert(SDKProvider.fcmUri(ctx), values)
    }

    companion object {
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