package com.tunebrains.cpu.library

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.net.*
import android.os.Build
import com.cantrowitz.rxbroadcast.RxBroadcast
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

data class OnlineState(val online: Boolean)
class ConnectionObserver(private val ctx: Context) {
    val onlineObserver = PublishSubject.create<OnlineState>()
    private val rxBroadcast = RxBroadcast.fromBroadcast(ctx, IntentFilter().apply {
        addAction(ConnectivityManager.CONNECTIVITY_ACTION)
    })
    private val compositeDisposable = CompositeDisposable()
    @SuppressLint("MissingPermission")
    fun start() {
        val connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                object : ConnectivityManager.NetworkCallback() {
                    override fun onCapabilitiesChanged(network: Network?, networkCapabilities: NetworkCapabilities?) {
                        super.onCapabilitiesChanged(network, networkCapabilities)
                    }

                    override fun onLost(network: Network?) {
                        super.onLost(network)

                        Timber.d("onLost $network")
                        onlineObserver.onNext(OnlineState(isNetworkAvailable()))

                    }

                    override fun onLinkPropertiesChanged(network: Network?, linkProperties: LinkProperties?) {
                        super.onLinkPropertiesChanged(network, linkProperties)
                        Timber.d("onLinkPropertiesChanged $linkProperties")
                    }

                    override fun onUnavailable() {
                        super.onUnavailable()
                        Timber.d("onUnavailable")
                    }

                    override fun onLosing(network: Network?, maxMsToLive: Int) {
                        super.onLosing(network, maxMsToLive)
                        Timber.d("onLosing")
                    }

                    override fun onAvailable(network: Network?) {
                        super.onAvailable(network)
                        Timber.d("onAvailable $network")
                        onlineObserver.onNext(OnlineState(isNetworkAvailable()))
                    }
                })
        } else {
            compositeDisposable.add(rxBroadcast.subscribe {
                onlineObserver.onNext(OnlineState(isNetworkAvailable()))
            })
        }
    }

    fun stop() {
        compositeDisposable.clear()
    }

    @SuppressLint("MissingPermission")
    private fun isNetworkAvailable(): Boolean {
        val manager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = manager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}