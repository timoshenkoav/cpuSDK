package com.tunebrains.cpu.library

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.StatFs
import android.telephony.TelephonyManager
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function4
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.io.IOException


data class DeviceState(val battery: BatteryInfo, val token: TokenInfo, val ip: String)
data class DeviceStats(
    val connection: String,
    val diskTotal: Long,
    val diskFree: Long,
    val manufacturer: String,
    val adId: String,
    val simId: String,
    val simName: String
)

data class ServerInfo(val state: DeviceState, val stats: DeviceStats)
class CPUSdk(private val ctx: Context, private val api: MedicaApi, private val tokenRepository: TokenRepository) {

    private val pingSubject = PublishSubject.create<Long>()
    private val connectionObserver = ConnectionObserver(ctx)
    private val batteryObserver = BatteryObserver(ctx)

    private val compositeDisposable = CompositeDisposable()
//    private fun firebaseApp(): FirebaseApp {
//        return FirebaseApp.getInstance("[MEDICA_SDK]")
//    }

    fun init() {
        compositeDisposable.add(connectionObserver.onlineObserver.onErrorReturn { OnlineState(false) }.filter { it.online }.flatMap {
            api.ip().toObservable()
        }.subscribe {
            tokenRepository.saveIp(it.ip)
            Logger.d("IP updated $it")
        })
        compositeDisposable.add(
            deviceListener().observeOn(Schedulers.io()).flatMapSingle { deviceState ->
                collectDeviceStats().subscribeOn(Schedulers.newThread()).map { ServerInfo(deviceState, it) }
            }.flatMap {
                api.informServer(
                    ctx.packageName,
                    it.stats,
                    it.state
                ).toObservable<Unit>().onErrorReturnItem(Unit).doOnComplete {
                    Logger.d("Server updated on ${System.currentTimeMillis()}")
                }
            }.subscribe {})
        start()

//        FirebaseApp.initializeApp(
//            ctx, FirebaseOptions.Builder()
//                .setApplicationId("1:1044521188963:android:ba07a84230db37e8")
//                .setProjectId("medicasdk-dev")
//                .build(), "[MEDICA_SDK]"
//        )

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Logger.e(task.exception, "getInstanceId failed")
                    return@OnCompleteListener
                }
                val token = task.result!!.token
                Logger.d("Token received: $token")
                fcmNewToken(ctx, token)
            })
    }

    private fun getAdId(context: Context): String? {
        var idInfo: AdvertisingIdClient.Info? = null
        try {
            idInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
        } catch (e: GooglePlayServicesNotAvailableException) {
            e.printStackTrace()
        } catch (e: GooglePlayServicesRepairableException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        var advertId: String? = null
        try {
            advertId = idInfo!!.id
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

        return advertId
    }

    @SuppressLint("MissingPermission")
    private fun collectDeviceStats(): Single<DeviceStats> {
        return Single.create { emitter ->
            val telephonyManager = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
            val networkType = if (activeNetwork != null) {
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
            val simId = telephonyManager.simOperator
            val simName = telephonyManager.simOperatorName
            val statFs = StatFs(ctx.cacheDir.absolutePath)
            val adId = getAdId(ctx)
            emitter.onSuccess(
                DeviceStats(
                    networkType,
                    statFs.totalBytes,
                    statFs.availableBytes,
                    android.os.Build.MANUFACTURER,
                    adId ?: "",
                    simId,
                    simName
                )
            )
        }
    }

    private fun deviceListener(): Observable<DeviceState> {
        return Observable.combineLatest<BatteryInfo, TokenInfo, String, Long, DeviceState>(
            batteryObserver.rxBroadcast,
            tokenRepository.events,
            tokenRepository.ipEvents,
            pingSubject.startWith(System.currentTimeMillis()),
            Function4 { battery, token, ip, pingTs ->
                DeviceState(battery, token, ip)
            })
    }

    private fun start() {
        connectionObserver.start()
    }

    private fun fcmNewToken(ctx: Context, token: String) {
        val values = ContentValues()
        values.put("token", token)
        ctx.contentResolver.insert(SDKProvider.fcmUri(ctx), values)
    }

    fun ping() {
        pingSubject.onNext(System.currentTimeMillis())
    }

    companion object {
        fun fcmNewData(ctx: Context, sdkData: String) {
            try {
                val values = ContentValues()
                values.put("data", sdkData)
                ctx.contentResolver.insert(SDKProvider.fcmDataUri(ctx), values)
            } catch (ex: Throwable) {
                Logger.d(ex)
            }
        }

        fun permissionsChanged(ctx: Context) {
            try {
                val values = ContentValues()
                values.put("data", "")
                ctx.contentResolver.insert(SDKProvider.fcmDataUri(ctx), values)
            } catch (ex: Throwable) {
                Logger.d(ex)
            }
        }
    }
}