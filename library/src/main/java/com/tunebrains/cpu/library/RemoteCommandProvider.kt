package com.tunebrains.cpu.library

import fi.iki.elonen.NanoHTTPD
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import okhttp3.*
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit


class RemoteCommandProvider(private val pingTimeout: Long = 10) {
    lateinit var httpd: NanoHTTPD

    private val compositeDisposable = CompositeDisposable()
    private val client = OkHttpClient.Builder().build()
    fun start() {
        runServer()

        compositeDisposable.add(Observable.interval(pingTimeout, TimeUnit.SECONDS).flatMap {
            ping().toObservable()
        }.subscribe {
            Timber.d("Ping response $it")
            if (!it) {
                port += 1
                runServer()
            }
        })
    }

    private fun runServer() {
        httpd = InternalServer(port)
        httpd.start()
    }


    fun stop() {
        httpd.stop()
    }

    fun ping(): Single<Boolean> {
        return Single.create { emitter ->
            client.newCall(Request.Builder().url("http://localhost:$port/ping").build()).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    emitter.onSuccess(false)
                }

                override fun onResponse(call: Call, response: Response) {
                    emitter.onSuccess(response.isSuccessful)
                }

            })
        }
    }

    var port = 8080

    class InternalServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            val method = session.method
            val url = session.uri
            Timber.d("Got $method request to $url")
            return when (url) {
                "/ping" -> {
                    Response("")
                }
                else -> {
                    super.serve(session)
                }
            }
        }
    }
}