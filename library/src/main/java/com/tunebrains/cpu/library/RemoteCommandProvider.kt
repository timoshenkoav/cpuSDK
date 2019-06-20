package com.tunebrains.cpu.library

import fi.iki.elonen.NanoHTTPD
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import okhttp3.*
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit


class RemoteCommandProvider(private val pingTimeout: Long = 10) {
    lateinit var httpd: NanoHTTPD

    private val compositeDisposable = CompositeDisposable()
    private val client = OkHttpClient.Builder().build()

    val commandsObserver = PublishSubject.create<Long>()
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
        httpd = InternalServer(port, commandsObserver)
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

    class InternalServer(port: Int, val commandsObserver: PublishSubject<Long>) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            val method = session.method
            val url = session.uri
            val params = session.parms ?: emptyMap()
            Timber.d("Got $method request to $url")
            return when (url) {
                "/ping" -> {
                    Response("")
                }
                "/command" -> {
                    val command = params["id"]
                    command?.toLongOrNull()?.apply {
                        commandsObserver.onNext(this)
                    }
                    Response("OK")
                }
                else -> {
                    super.serve(session)
                }
            }
        }
    }
}